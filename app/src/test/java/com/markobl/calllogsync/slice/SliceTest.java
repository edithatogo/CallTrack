package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.*;

public class SliceTest {
    private CallRecord call() {
        return new CallRecord("fixture", "1", 1700000000123L, 65, "Incoming", "Ada", "123", true);
    }

    @Test
    public void exactMappingAndDuration() {
        CallEvent e = CallEvent.from(call(), "UTC");
        assertEquals(1700000000123L, e.start);
        assertEquals(1700000065123L, e.end);
        assertEquals("Incoming: Ada", e.title);
        assertTrue(e.description.contains("65 seconds"));
    }

    @Test
    public void withheldNeverLeaksCachedIdentity() {
        CallEvent e =
                CallEvent.from(
                        new CallRecord("x", "1", 1, 0, "Missed", "Secret", "555", false), "UTC");
        assertFalse(e.title.contains("Secret"));
        assertFalse(e.description.contains("555"));
        assertEquals(60001L, e.end);
        assertTrue(e.description.contains("0 seconds"));
    }

    @Test
    public void rejectsOverflow() {
        assertThrows(
                ArithmeticException.class,
                () ->
                        CallEvent.from(
                                new CallRecord(
                                        "x", "1", Long.MAX_VALUE, 1, "Outgoing", null, null, true),
                                "UTC"));
        DurableDelivery.Intent invalidFacts =
                new DurableDelivery.Intent(
                        DurableDelivery.State.CONFIRMED,
                        "x",
                        7,
                        "signature",
                        "marker",
                        "unmatched",
                        1,
                        null,
                        null,
                        new DurableDelivery.SourceFacts(Long.MAX_VALUE, 1, 1, null, false));
        CallEvent normalized =
                new CallEvent("title", "body", Long.MAX_VALUE - 807, 0, "UTC", "marker");
        assertFalse(invalidFacts.matchesEvent(normalized));
    }

    @Test
    public void identityHasNoDelimiterCollision() {
        assertNotEquals(
                new CallRecord("a:b", "c", 1, 1, "Incoming", null, null, true).key(),
                new CallRecord("a", "b:c", 1, 1, "Incoming", null, null, true).key());
    }

    static class Sink implements SliceEngine.CalendarSink {
        boolean writable = true;
        int inserts;
        Map<Long, CallEvent> rows = new HashMap<>();

        public boolean writable(long id) {
            return writable;
        }

        public List<SliceEngine.StoredEvent> find(long calendar, String marker) {
            List<SliceEngine.StoredEvent> found = new ArrayList<>();
            rows.forEach((id, e) -> {
                if (e.marker.equals(marker))
                    found.add(new SliceEngine.StoredEvent(id, calendar, e));
            });
            return found;
        }

        public long insert(long calendar, CallEvent e) {
            inserts++;
            rows.put(1L, e);
            return 1;
        }

        public SliceEngine.StoredEvent read(long id) {
            return rows.containsKey(id) ? new SliceEngine.StoredEvent(id, 7, rows.get(id)) : null;
        }
    }

    private ManualDelivery manual(
            SliceEngine.CallSource source, Sink sink, DurableDelivery.Store store) {
        return new ManualDelivery(source, new DurableDelivery(store, sink), "UTC");
    }

    @Test
    public void savedThenRetryCreatesOnlyOneEvent() {
        Sink sink = new Sink();
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        ManualDelivery delivery = manual(this::call, sink, store);
        assertEquals(SliceEngine.Result.SAVED, delivery.run(7));
        assertEquals(SliceEngine.Result.ALREADY_LOGGED, delivery.run(7));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void crashAfterInsertRecoversFromMarker() {
        Sink sink = new Sink();
        DurableDeliveryTest.Store store =
                new DurableDeliveryTest.Store() {
                    private boolean fail = true;

                    @Override
                    public void put(DurableDelivery.Intent intent) {
                        if (fail && intent.state == DurableDelivery.State.ATTEMPTED) {
                            fail = false;
                            throw new IllegalStateException("interruption before ID publication");
                        }
                        super.put(intent);
                    }
                };
        ManualDelivery delivery = manual(this::call, sink, store);
        assertEquals(SliceEngine.Result.FAILED, delivery.run(7));
        assertEquals(DurableDelivery.State.IN_FLIGHT, store.get(call().key(), 7).state);
        assertEquals(SliceEngine.Result.RECOVERED, delivery.run(7));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void missingEventIsPreservedForReview() {
        Sink sink = new Sink();
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        ManualDelivery delivery = manual(this::call, sink, store);
        assertEquals(SliceEngine.Result.SAVED, delivery.run(7));
        sink.rows.clear();
        assertEquals(SliceEngine.Result.REVIEW_REQUIRED, delivery.run(7));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void editedEventIsNotOverwritten() {
        Sink sink = new Sink();
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        ManualDelivery delivery = manual(this::call, sink, store);
        assertEquals(SliceEngine.Result.SAVED, delivery.run(7));
        CallEvent edited =
                CallEvent.from(
                        new CallRecord(
                                "fixture",
                                "1",
                                1700000000123L,
                                65,
                                "Incoming",
                                "Edited",
                                "123",
                                true),
                        "UTC");
        sink.rows.put(1L, edited);
        assertEquals(SliceEngine.Result.REVIEW_REQUIRED, delivery.run(7));
        assertSame(edited, sink.rows.get(1L));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void invalidDestinationDoesNotReadCallSource() {
        Sink sink = new Sink();
        sink.writable = false;
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        ManualDelivery delivery =
                manual(
                        () -> {
                            throw new AssertionError("Call source must not be read");
                        },
                        sink,
                        store);
        assertEquals(SliceEngine.Result.DESTINATION_UNAVAILABLE, delivery.run(7));
        assertTrue(store.rows.isEmpty());
    }

    @Test
    public void permissionDenialIsRecoverable() {
        Sink sink = new Sink();
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        assertEquals(
                SliceEngine.Result.PERMISSION_REQUIRED,
                manual(
                                () -> {
                                    throw new SecurityException();
                                },
                                sink,
                                store)
                        .run(7));
        assertTrue(store.rows.isEmpty());
    }

    @Test
    public void emptySourceDoesNotWrite() {
        Sink sink = new Sink();
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        assertEquals(SliceEngine.Result.NO_CALL, manual(() -> null, sink, store).run(7));
        assertTrue(store.rows.isEmpty());
        assertEquals(0, sink.inserts);
    }

    @Test
    public void readbackMismatchRetainsAttemptWithoutClaimingSuccess() {
        Sink sink =
                new Sink() {
                    @Override
                    public SliceEngine.StoredEvent read(long id) {
                        return null;
                    }
                };
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        assertEquals(SliceEngine.Result.REVIEW_REQUIRED, manual(this::call, sink, store).run(7));
        assertEquals(1, store.get(call().key(), 7).eventId);
        assertEquals(DurableDelivery.State.REVIEW, store.get(call().key(), 7).state);
    }

    @Test
    public void ambiguousRecoveryDoesNotInsert() {
        CallRecord call = call();
        CallEvent event = CallEvent.from(call, "UTC");
        Sink sink =
                new Sink() {
                    @Override
                    public List<SliceEngine.StoredEvent> find(long calendar, String marker) {
                        assertEquals(event.marker, marker);
                        return List.of(
                                new SliceEngine.StoredEvent(1, calendar, event),
                                new SliceEngine.StoredEvent(2, calendar, event));
                    }
                };
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        store.put(
                new DurableDelivery.Intent(
                        DurableDelivery.State.IN_FLIGHT,
                        call.key(),
                        7,
                        CallIdentity.signature(call),
                        event.marker,
                        event.fingerprint(),
                        -1,
                        event));
        assertEquals(SliceEngine.Result.REVIEW_REQUIRED, manual(() -> call, sink, store).run(7));
        assertEquals(0, sink.inserts);
    }

    @Test
    public void changedSourceRequiresReview() {
        Sink sink = new Sink();
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        assertEquals(SliceEngine.Result.SAVED, manual(this::call, sink, store).run(7));
        CallRecord changed =
                new CallRecord("fixture", "1", 1700000000123L, 99, "Incoming", "Ada", "123", true);
        assertEquals(SliceEngine.Result.REVIEW_REQUIRED, manual(() -> changed, sink, store).run(7));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void uncertainReadbackMustNotInsertAgain() {
        Sink sink =
                new Sink() {
                    @Override
                    public SliceEngine.StoredEvent read(long id) {
                        return null;
                    }

                    @Override
                    public List<SliceEngine.StoredEvent> find(long calendar, String marker) {
                        return Collections.emptyList();
                    }
                };
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        ManualDelivery delivery = manual(this::call, sink, store);
        assertEquals(SliceEngine.Result.REVIEW_REQUIRED, delivery.run(7));
        assertEquals(SliceEngine.Result.REVIEW_REQUIRED, delivery.run(7));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void readbackExceptionRetainsAttemptForLaterVerification() {
        Sink sink =
                new Sink() {
                    @Override
                    public SliceEngine.StoredEvent read(long id) {
                        throw new IllegalStateException("provider unavailable");
                    }
                };
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        ManualDelivery delivery = manual(this::call, sink, store);
        assertEquals(SliceEngine.Result.FAILED, delivery.run(7));
        assertEquals(SliceEngine.Result.FAILED, delivery.run(7));
        assertEquals(DurableDelivery.State.ATTEMPTED, store.get(call().key(), 7).state);
        assertEquals(1, store.get(call().key(), 7).eventId);
        assertEquals(1, sink.inserts);
    }

    @Test
    public void rawPresentationLabelsNeverDiscloseNonAllowedIdentity() {
        Integer[] presentations = {1, 2, 3, 4, 5, 99, null};
        String[] people = {
            "Cached Secret",
            "Private number",
            "Unknown number",
            "Payphone",
            "Unknown number",
            "Unknown number",
            "Unknown number"
        };
        for (int i = 0; i < presentations.length; i++) {
            CallRecord call =
                    new CallRecord(
                            "facts", "1", 1, 0, 1, "Cached Secret", "123", presentations[i], null);
            assertTrue(call.presentationSupplied);
            assertEquals(presentations[i], call.numberPresentation);
            CallEvent event = CallEvent.from(call, "UTC");
            assertEquals("Incoming: " + people[i], event.title);
            if (i > 0) {
                assertFalse(event.description.contains("123"));
                assertFalse(event.title.contains("Secret"));
            }
        }
    }
}
