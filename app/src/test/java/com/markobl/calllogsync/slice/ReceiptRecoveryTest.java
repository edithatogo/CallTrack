package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.*;

public class ReceiptRecoveryTest {
    private CallRecord call() {
        return PagedCallsTest.row(1, 1);
    }

    private ReceiptRecovery recovery(DurableDeliveryTest.Store store, SliceTest.Sink sink) {
        return new ReceiptRecovery(
                store,
                new ReceiptRecovery.ExistingEvents() {
                    public SliceEngine.StoredEvent read(long id) {
                        return sink.read(id);
                    }

                    public List<SliceEngine.StoredEvent> find(long calendar, String marker) {
                        return sink.find(calendar, marker);
                    }
                });
    }

    @Test
    public void neverAttemptedIntentCannotBeInsertedByReconciliation() {
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        CallEvent event = CallEvent.from(call(), "UTC");
        store.put(
                new DurableDelivery.Intent(
                        DurableDelivery.State.READY,
                        call().key(),
                        7,
                        CallIdentity.signature(call()),
                        event.marker,
                        event.fingerprint(),
                        -1,
                        event));
        SliceTest.Sink sink = new SliceTest.Sink();
        assertEquals(
                ReceiptRecovery.Result.NEVER_ATTEMPTED,
                recovery(store, sink).reconcile(call().key(), 7));
        assertEquals(0, sink.inserts);
        assertNotNull(store.get(call().key(), 7).payload);
    }

    @Test
    public void interruptedEffectCanBeRecoveredAcrossInactiveOwnershipWithoutNewInsert() {
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        SliceTest.Sink sink = new SliceTest.Sink();
        CallEvent event = CallEvent.from(call(), "UTC");
        sink.insert(7, event);
        store.put(
                new DurableDelivery.Intent(
                        DurableDelivery.State.IN_FLIGHT,
                        call().key(),
                        7,
                        CallIdentity.signature(call()),
                        event.marker,
                        event.fingerprint(),
                        -1,
                        event,
                        new DurableDelivery.Operation("cancelled-import", 1)));
        assertEquals(
                ReceiptRecovery.Result.VERIFIED, recovery(store, sink).reconcile(call().key(), 7));
        assertEquals(1, sink.inserts);
        assertEquals(DurableDelivery.State.CONFIRMED, store.get(call().key(), 7).state);
        assertNull(store.get(call().key(), 7).payload);

        DurableDeliveryTest.Store syncedStore = new DurableDeliveryTest.Store();
        SliceTest.Sink syncedSink = new SliceTest.Sink();
        assertEquals(
                DurableDelivery.Result.SAVED,
                new DurableDelivery(syncedStore, syncedSink).deliver(call(), 7, "UTC", () -> true));
        syncedSink.rows.put(1L, DurableDeliveryTest.wholeSecondCopy(syncedSink.rows.get(1L)));
        assertEquals(
                ReceiptRecovery.Result.VERIFIED,
                recovery(syncedStore, syncedSink).reconcile(call().key(), 7));
        assertEquals(1, syncedSink.inserts);
    }

    @Test
    public void changedOrMissingEventRemainsReviewAndSuppressionKeepsTombstone() {
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        SliceTest.Sink sink = new SliceTest.Sink();
        new DurableDelivery(store, sink).deliver(call(), 7, "UTC", () -> true);
        sink.rows.clear();
        ReceiptRecovery recovery = recovery(store, sink);
        assertEquals(ReceiptRecovery.Result.REVIEW_REQUIRED, recovery.reconcile(call().key(), 7));
        assertEquals(ReceiptRecovery.Result.SUPPRESSED, recovery.suppress(call().key(), 7));
        assertNotNull(store.get(call().key(), 7));
        assertEquals(1, store.get(call().key(), 7).eventId);
        assertNull(store.get(call().key(), 7).payload);
        assertEquals(ReceiptRecovery.Result.SUPPRESSED, recovery.reconcile(call().key(), 7));
        assertEquals(1, sink.inserts);

        // Normalization must not hide changes to the title or either boundary.
        for (int change = 0; change < 3; change++) {
            DurableDeliveryTest.Store changedStore = new DurableDeliveryTest.Store();
            SliceTest.Sink changedSink = new SliceTest.Sink();
            assertEquals(
                    DurableDelivery.Result.SAVED,
                    new DurableDelivery(changedStore, changedSink)
                            .deliver(call(), 7, "UTC", () -> true));
            CallEvent original = changedSink.rows.get(1L);
            CallEvent normalized = DurableDeliveryTest.wholeSecondCopy(original);
            changedSink.rows.put(
                    1L,
                    new CallEvent(
                            change == 0 ? normalized.title + " edited" : normalized.title,
                            normalized.description,
                            change == 1 ? normalized.start + 1000 : normalized.start,
                            change == 2 ? original.end : normalized.end,
                            normalized.timezone,
                            normalized.marker));
            assertEquals(
                    ReceiptRecovery.Result.REVIEW_REQUIRED,
                    recovery(changedStore, changedSink).reconcile(call().key(), 7));
            assertEquals(1, changedSink.inserts);
        }

        // Old receipts without source facts cannot safely infer fractional seconds.
        DurableDeliveryTest.Store legacyStore = new DurableDeliveryTest.Store();
        SliceTest.Sink legacySink = new SliceTest.Sink();
        CallEvent original = CallEvent.from(call(), "UTC");
        legacySink.insert(7, DurableDeliveryTest.wholeSecondCopy(original));
        legacyStore.put(
                new DurableDelivery.Intent(
                        DurableDelivery.State.CONFIRMED,
                        call().key(),
                        7,
                        CallIdentity.signature(call()),
                        original.marker,
                        original.fingerprint(),
                        1,
                        null));
        assertEquals(
                ReceiptRecovery.Result.REVIEW_REQUIRED,
                recovery(legacyStore, legacySink).reconcile(call().key(), 7));
        assertEquals(1, legacySink.inserts);
    }

    @Test
    public void ambiguousOrPermissionDeniedInspectionCannotInventBinding() {
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        CallEvent event = CallEvent.from(call(), "UTC");
        store.put(
                new DurableDelivery.Intent(
                        DurableDelivery.State.IN_FLIGHT,
                        call().key(),
                        7,
                        "s",
                        event.marker,
                        event.fingerprint(),
                        -1,
                        event));
        ReceiptRecovery denied =
                new ReceiptRecovery(
                        store,
                        new ReceiptRecovery.ExistingEvents() {
                            public SliceEngine.StoredEvent read(long id) {
                                throw new SecurityException();
                            }

                            public List<SliceEngine.StoredEvent> find(long c, String m) {
                                throw new SecurityException();
                            }
                        });
        assertEquals(ReceiptRecovery.Result.PERMISSION_REQUIRED, denied.reconcile(call().key(), 7));
        assertEquals(DurableDelivery.State.IN_FLIGHT, store.get(call().key(), 7).state);
        ReceiptRecovery ambiguous =
                new ReceiptRecovery(
                        store,
                        new ReceiptRecovery.ExistingEvents() {
                            public SliceEngine.StoredEvent read(long id) {
                                return null;
                            }

                            public List<SliceEngine.StoredEvent> find(long c, String m) {
                                return Arrays.asList(
                                        new SliceEngine.StoredEvent(1, 7, event),
                                        new SliceEngine.StoredEvent(2, 7, event));
                            }
                        });
        assertEquals(ReceiptRecovery.Result.REVIEW_REQUIRED, ambiguous.reconcile(call().key(), 7));
        assertEquals(-1, store.get(call().key(), 7).eventId);
    }

    @Test
    public void discoveredIdSurvivesDeniedReadbackWithoutSourceOrReinsertion() {
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        CallEvent event = CallEvent.from(call(), "UTC");
        store.put(
                new DurableDelivery.Intent(
                        DurableDelivery.State.IN_FLIGHT,
                        call().key(),
                        7,
                        "s",
                        event.marker,
                        event.fingerprint(),
                        -1,
                        event));
        ReceiptRecovery recovery =
                new ReceiptRecovery(
                        store,
                        new ReceiptRecovery.ExistingEvents() {
                            public SliceEngine.StoredEvent read(long id) {
                                throw new SecurityException();
                            }

                            public List<SliceEngine.StoredEvent> find(long c, String m) {
                                return Collections.singletonList(
                                        new SliceEngine.StoredEvent(42, 7, event));
                            }
                        });
        assertEquals(
                ReceiptRecovery.Result.PERMISSION_REQUIRED, recovery.reconcile(call().key(), 7));
        assertEquals(42, store.get(call().key(), 7).eventId);
        assertEquals(DurableDelivery.State.ATTEMPTED, store.get(call().key(), 7).state);
    }
}
