package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;

public class DurableDeliveryTest {
    private CallRecord call() {
        return new CallRecord("fixture", "1", 1700000000123L, 65, "Incoming", "Ada", "123", true);
    }

    static class Crash extends Error {}

    static class Store implements DurableDelivery.Store {
        final Map<String, DurableDelivery.Intent> rows = new HashMap<>();
        DurableDelivery.State crashAfter;

        public DurableDelivery.Intent get(String key, long calendar) {
            return rows.get(key + "/" + calendar);
        }

        public void put(DurableDelivery.Intent intent) {
            rows.put(intent.key + "/" + intent.calendar, intent);
            if (intent.state == crashAfter) {
                crashAfter = null;
                throw new Crash();
            }
        }
    }

    private SliceTest.Sink sink() {
        return new SliceTest.Sink();
    }

    private DurableDelivery delivery(Store store, SliceTest.Sink sink) {
        return new DurableDelivery(store, sink);
    }

    @Test
    public void intentIsCommittedBeforeInsertAndPayloadPurgedAfterSuccess() {
        Store store = new Store();
        SliceTest.Sink sink =
                new SliceTest.Sink() {
                    @Override
                    public long insert(long calendar, CallEvent event) {
                        assertEquals(
                                DurableDelivery.State.IN_FLIGHT,
                                store.get(call().key(), calendar).state);
                        return super.insert(calendar, event);
                    }
                };
        assertEquals(
                DurableDelivery.Result.SAVED,
                delivery(store, sink).deliver(call(), 7, "UTC", () -> true));
        DurableDelivery.Intent receipt = store.get(call().key(), 7);
        assertEquals(DurableDelivery.State.CONFIRMED, receipt.state);
        assertNull(receipt.payload);
        assertEquals(
                DurableDelivery.Result.ALREADY_LOGGED,
                delivery(store, sink).deliver(call(), 7, "UTC", () -> true));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void interruptedBeforeInsertDoesNotGuessThatRetryIsSafe() {
        Store store = new Store();
        store.crashAfter = DurableDelivery.State.IN_FLIGHT;
        SliceTest.Sink sink = sink();
        assertThrows(
                Crash.class, () -> delivery(store, sink).deliver(call(), 7, "UTC", () -> true));
        assertEquals(
                DurableDelivery.Result.REVIEW_REQUIRED,
                delivery(store, sink).deliver(call(), 7, "UTC", () -> true));
        assertEquals(0, sink.inserts);
    }

    @Test
    public void crashAfterInsertRecoversExactlyOneMarker() {
        Store store = new Store();
        SliceTest.Sink sink =
                new SliceTest.Sink() {
                    boolean crash = true;

                    @Override
                    public long insert(long calendar, CallEvent event) {
                        long id = super.insert(calendar, event);
                        if (crash) {
                            crash = false;
                            throw new Crash();
                        }
                        return id;
                    }
                };
        assertThrows(
                Crash.class, () -> delivery(store, sink).deliver(call(), 7, "UTC", () -> true));
        assertEquals(
                DurableDelivery.Result.RECOVERED,
                delivery(store, sink).resume(call().key(), 7, () -> true));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void lostMarkerAfterCrashNeverCreatesAnotherEvent() {
        Store store = new Store();
        SliceTest.Sink sink =
                new SliceTest.Sink() {
                    @Override
                    public long insert(long calendar, CallEvent event) {
                        super.insert(calendar, event);
                        throw new Crash();
                    }

                    @Override
                    public List<SliceEngine.StoredEvent> find(long calendar, String marker) {
                        return Collections.emptyList();
                    }
                };
        assertThrows(
                Crash.class, () -> delivery(store, sink).deliver(call(), 7, "UTC", () -> true));
        assertEquals(
                DurableDelivery.Result.REVIEW_REQUIRED,
                delivery(store, sink).deliver(call(), 7, "UTC", () -> true));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void deletedConfirmedEventIsNeverRecreated() {
        Store store = new Store();
        SliceTest.Sink sink = sink();
        DurableDelivery delivery = delivery(store, sink);
        delivery.deliver(call(), 7, "UTC", () -> true);
        sink.rows.clear();
        assertEquals(
                DurableDelivery.Result.REVIEW_REQUIRED,
                delivery.deliver(call(), 7, "UTC", () -> true));
        assertEquals(
                DurableDelivery.Result.REVIEW_REQUIRED,
                delivery.deliver(call(), 7, "UTC", () -> true));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void cachedContactRenameOrTimezoneChangeDoesNotRewrite() {
        Store store = new Store();
        SliceTest.Sink sink = sink();
        DurableDelivery delivery = delivery(store, sink);
        delivery.deliver(call(), 7, "UTC", () -> true);
        CallRecord renamed =
                new CallRecord(
                        "fixture", "1", 1700000000123L, 65, "Incoming", "Renamed", "123", true);
        assertEquals(
                DurableDelivery.Result.ALREADY_LOGGED,
                delivery.deliver(renamed, 7, "Australia/Brisbane", () -> true));
        assertEquals("Incoming: Ada", sink.rows.get(1L).title);
        assertEquals(1, sink.inserts);
    }

    @Test
    public void reusedSourceIdWithDifferentContentNeedsReview() {
        Store store = new Store();
        SliceTest.Sink sink = sink();
        DurableDelivery delivery = delivery(store, sink);
        delivery.deliver(call(), 7, "UTC", () -> true);
        CallRecord changed =
                new CallRecord("fixture", "1", 1700000001123L, 65, "Incoming", "Ada", "123", true);
        assertEquals(
                DurableDelivery.Result.SOURCE_CHANGED,
                delivery.deliver(changed, 7, "UTC", () -> true));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void pausedDeliveryDoesNotPrepareOrWrite() {
        Store store = new Store();
        SliceTest.Sink sink = sink();
        assertEquals(
                DurableDelivery.Result.PAUSED,
                delivery(store, sink).deliver(call(), 7, "UTC", () -> false));
        assertTrue(store.rows.isEmpty());
        assertEquals(0, sink.inserts);
    }

    @Test
    public void permissionDeniedBeforeAttemptLeavesNoIntent() {
        Store store = new Store();
        SliceTest.Sink sink =
                new SliceTest.Sink() {
                    @Override
                    public boolean writable(long id) {
                        throw new SecurityException();
                    }
                };
        assertEquals(
                DurableDelivery.Result.PERMISSION_REQUIRED,
                delivery(store, sink).deliver(call(), 7, "UTC", () -> true));
        assertTrue(store.rows.isEmpty());
    }

    @Test
    public void readbackFailureRetainsKnownIdAcrossRetries() {
        Store store = new Store();
        SliceTest.Sink sink =
                new SliceTest.Sink() {
                    @Override
                    public SliceEngine.StoredEvent read(long id) {
                        throw new IllegalStateException();
                    }
                };
        DurableDelivery delivery = delivery(store, sink);
        assertEquals(DurableDelivery.Result.FAILED, delivery.deliver(call(), 7, "UTC", () -> true));
        assertEquals(1, store.get(call().key(), 7).eventId);
        assertEquals(DurableDelivery.Result.FAILED, delivery.deliver(call(), 7, "UTC", () -> true));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void overlappingInstancesProduceOneEffect() throws Exception {
        Store store = new Store();
        SliceTest.Sink sink = sink();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<DurableDelivery.Result>> requests = new ArrayList<>();
            for (int i = 0; i < 20; i++)
                requests.add(() -> delivery(store, sink).deliver(call(), 7, "UTC", () -> true));
            int saved = 0;
            for (Future<DurableDelivery.Result> result : pool.invokeAll(requests)) {
                DurableDelivery.Result value = result.get();
                if (value == DurableDelivery.Result.SAVED) saved++;
                else assertEquals(DurableDelivery.Result.ALREADY_LOGGED, value);
            }
            assertEquals(1, saved);
            assertEquals(1, sink.inserts);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void readyIntentResumesWithoutOriginalSourceRow() {
        Store store = new Store();
        store.crashAfter = DurableDelivery.State.READY;
        SliceTest.Sink sink = sink();
        assertThrows(
                Crash.class, () -> delivery(store, sink).deliver(call(), 7, "UTC", () -> true));
        assertEquals(
                DurableDelivery.Result.SAVED,
                delivery(store, sink).resume(call().key(), 7, () -> true));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void pendingIntentCannotBeResumedByAnotherOwnerOrGeneration() {
        Store store = new Store();
        store.crashAfter = DurableDelivery.State.READY;
        SliceTest.Sink sink = sink();
        DurableDelivery engine = delivery(store, sink);
        DurableDelivery.Operation original = new DurableDelivery.Operation("import-1", 1);
        assertThrows(Crash.class, () -> engine.deliver(call(), 7, "UTC", original, () -> true));
        assertEquals(
                DurableDelivery.Result.PAUSED,
                engine.resume(
                        call().key(),
                        7,
                        new DurableDelivery.Operation("automatic", 1),
                        () -> true));
        assertEquals(
                DurableDelivery.Result.PAUSED,
                engine.resume(
                        call().key(), 7, new DurableDelivery.Operation("import-1", 2), () -> true));
        assertEquals(DurableDelivery.Result.PAUSED, engine.resume(call().key(), 7, () -> true));
        assertEquals(
                DurableDelivery.Result.PAUSED,
                engine.deliver(
                        call(),
                        7,
                        "UTC",
                        new DurableDelivery.Operation("automatic", 1),
                        () -> true));
        assertEquals(0, sink.inserts);
        assertEquals(DurableDelivery.State.READY, store.get(call().key(), 7).state);
        assertEquals(
                DurableDelivery.Result.SAVED,
                engine.resume(
                        call().key(), 7, new DurableDelivery.Operation("import-1", 1), () -> true));
        assertEquals(1, sink.inserts);
        assertEquals(
                DurableDelivery.Result.ALREADY_LOGGED,
                engine.deliver(
                        call(),
                        7,
                        "UTC",
                        new DurableDelivery.Operation("automatic", 2),
                        () -> true));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void cancellationAtCommittedBoundaryDoesNotInsert() {
        java.util.concurrent.atomic.AtomicBoolean enabled =
                new java.util.concurrent.atomic.AtomicBoolean(true);
        Store store =
                new Store() {
                    @Override
                    public void put(DurableDelivery.Intent intent) {
                        super.put(intent);
                        if (intent.state == DurableDelivery.State.IN_FLIGHT) enabled.set(false);
                    }
                };
        SliceTest.Sink sink = sink();
        assertEquals(
                DurableDelivery.Result.PAUSED,
                delivery(store, sink)
                        .deliver(
                                call(),
                                7,
                                "UTC",
                                new DurableDelivery.Operation("import-1", 1),
                                enabled::get));
        assertEquals(0, sink.inserts);
        assertEquals(DurableDelivery.State.IN_FLIGHT, store.get(call().key(), 7).state);
    }

    @Test
    public void operationIdentityMustBeExplicitAndGenerationNonnegative() {
        assertThrows(IllegalArgumentException.class, () -> new DurableDelivery.Operation("", 0));
        assertThrows(IllegalArgumentException.class, () -> new DurableDelivery.Operation(null, 0));
        assertThrows(
                IllegalArgumentException.class, () -> new DurableDelivery.Operation("import", -1));
    }

    @Test
    public void sourceFactsSurviveZeroMarkerAndPayloadPurge() {
        Store store = new Store();
        SliceTest.Sink sink = sink();
        CallRecord zero =
                new CallRecord(
                        "facts", "1", 123456789, 0, 3, "Secret", "123", Integer.valueOf(3), null);
        assertEquals(
                DurableDelivery.Result.SAVED,
                delivery(store, sink).deliver(zero, 7, "UTC", () -> true));
        DurableDelivery.Intent receipt = store.get(zero.key(), 7);
        assertNull(receipt.payload);
        assertEquals(123456789, receipt.facts.start);
        assertEquals(0, receipt.facts.seconds);
        assertEquals(3, receipt.facts.type);
        assertEquals(Integer.valueOf(3), receipt.facts.presentation);
        assertEquals(60000, sink.rows.get(1L).end - sink.rows.get(1L).start);
    }

    @Test
    public void changedRawPresentationIsReviewedWithoutChangingLegacySignature() {
        Store store = new Store();
        SliceTest.Sink sink = sink();
        CallRecord restricted =
                new CallRecord("facts", "1", 123, 1, 1, "Secret", "123", Integer.valueOf(2), null);
        CallRecord unknown =
                new CallRecord("facts", "1", 123, 1, 1, "Secret", "123", Integer.valueOf(3), null);
        assertEquals(CallIdentity.signature(restricted), CallIdentity.signature(unknown));
        assertEquals(
                DurableDelivery.Result.SAVED,
                delivery(store, sink).deliver(restricted, 7, "UTC", () -> true));
        assertEquals(
                DurableDelivery.Result.SOURCE_CHANGED,
                delivery(store, sink).deliver(unknown, 7, "UTC", () -> true));
        assertEquals(1, sink.inserts);
        assertEquals(Integer.valueOf(2), store.get(restricted.key(), 7).facts.presentation);
    }

    @Test
    public void matchingPayloadReturnedForWrongEventIdCannotConfirmReceipt() {
        Store store = new Store();
        SliceTest.Sink sink =
                new SliceTest.Sink() {
                    @Override
                    public SliceEngine.StoredEvent read(long id) {
                        SliceEngine.StoredEvent actual = super.read(id);
                        return new SliceEngine.StoredEvent(id + 100, actual.calendar, actual.event);
                    }
                };
        DurableDelivery engine = delivery(store, sink);
        assertEquals(
                DurableDelivery.Result.REVIEW_REQUIRED,
                engine.deliver(call(), 7, "UTC", () -> true));
        DurableDelivery.Intent intent = store.get(call().key(), 7);
        assertEquals(DurableDelivery.State.REVIEW, intent.state);
        assertEquals(1, intent.eventId);
        assertEquals(1, sink.inserts);
        assertEquals(
                DurableDelivery.Result.REVIEW_REQUIRED,
                engine.deliver(call(), 7, "UTC", () -> true));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void markerMatchWithInvalidEventIdCannotBecomeAConfirmedReceipt() {
        Store store = new Store();
        store.crashAfter = DurableDelivery.State.IN_FLIGHT;
        SliceTest.Sink sink =
                new SliceTest.Sink() {
                    @Override
                    public List<SliceEngine.StoredEvent> find(long calendar, String marker) {
                        return Collections.singletonList(
                                new SliceEngine.StoredEvent(
                                        -1, calendar, CallEvent.from(call(), "UTC")));
                    }

                    @Override
                    public SliceEngine.StoredEvent read(long id) {
                        return new SliceEngine.StoredEvent(id, 7, CallEvent.from(call(), "UTC"));
                    }
                };
        DurableDelivery engine = delivery(store, sink);
        assertThrows(Crash.class, () -> engine.deliver(call(), 7, "UTC", () -> true));
        assertEquals(
                DurableDelivery.Result.REVIEW_REQUIRED,
                engine.deliver(call(), 7, "UTC", () -> true));
        assertEquals(DurableDelivery.State.REVIEW, store.get(call().key(), 7).state);
        assertEquals(0, sink.inserts);
    }
}
