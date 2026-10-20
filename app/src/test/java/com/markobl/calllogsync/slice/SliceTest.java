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
            rows.forEach(
                    (id, e) -> {
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

    static class Ledger implements SliceEngine.Ledger {
        Map<String, SliceEngine.Entry> rows = new HashMap<>();
        boolean fail;

        public synchronized SliceEngine.Result transaction(SliceEngine.Work work) {
            return work.run();
        }

        public SliceEngine.Entry get(String key, long calendar) {
            return rows.get(key + ":" + calendar);
        }

        public void put(String key, long calendar, SliceEngine.Entry entry) {
            if (fail) throw new IllegalStateException("crash gap");
            rows.put(key + ":" + calendar, entry);
        }
    }

    @Test
    public void savedThenRetryCreatesOnlyOneEvent() {
        Sink sink = new Sink();
        Ledger ledger = new Ledger();
        SliceEngine engine = new SliceEngine(this::call, sink, ledger, "UTC");
        assertEquals(SliceEngine.Result.SAVED, engine.run(7));
        assertEquals(SliceEngine.Result.ALREADY_LOGGED, engine.run(7));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void crashAfterInsertRecoversFromMarker() {
        Sink sink = new Sink();
        Ledger ledger = new Ledger();
        ledger.fail = true;
        SliceEngine engine = new SliceEngine(this::call, sink, ledger, "UTC");
        assertEquals(SliceEngine.Result.FAILED, engine.run(7));
        ledger.fail = false;
        assertEquals(SliceEngine.Result.RECOVERED, engine.run(7));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void missingEventIsPreservedForReview() {
        Sink sink = new Sink();
        Ledger ledger = new Ledger();
        SliceEngine engine = new SliceEngine(this::call, sink, ledger, "UTC");
        engine.run(7);
        sink.rows.clear();
        assertEquals(SliceEngine.Result.REVIEW_REQUIRED, engine.run(7));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void editedEventIsNotOverwritten() {
        Sink sink = new Sink();
        Ledger ledger = new Ledger();
        SliceEngine engine = new SliceEngine(this::call, sink, ledger, "UTC");
        engine.run(7);
        sink.rows.put(
                1L,
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
                        "UTC"));
        assertEquals(SliceEngine.Result.REVIEW_REQUIRED, engine.run(7));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void invalidDestinationDoesNotReadCallSource() {
        Sink sink = new Sink();
        sink.writable = false;
        assertEquals(
                SliceEngine.Result.DESTINATION_UNAVAILABLE,
                new SliceEngine(
                                () -> {
                                    throw new AssertionError("personal source read");
                                },
                                sink,
                                new Ledger(),
                                "UTC")
                        .run(7));
    }

    @Test
    public void permissionDenialIsRecoverable() {
        assertEquals(
                SliceEngine.Result.PERMISSION_REQUIRED,
                new SliceEngine(
                                () -> {
                                    throw new SecurityException();
                                },
                                new Sink(),
                                new Ledger(),
                                "UTC")
                        .run(7));
    }

    @Test
    public void emptySourceDoesNotWrite() {
        Sink sink = new Sink();
        assertEquals(
                SliceEngine.Result.NO_CALL,
                new SliceEngine(() -> null, sink, new Ledger(), "UTC").run(7));
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
        Ledger ledger = new Ledger();
        assertEquals(
                SliceEngine.Result.REVIEW_REQUIRED,
                new SliceEngine(this::call, sink, ledger, "UTC").run(7));
        assertEquals(1, ledger.rows.size());
    }

    @Test
    public void ambiguousRecoveryDoesNotInsert() {
        Sink sink =
                new Sink() {
                    @Override
                    public List<SliceEngine.StoredEvent> find(long calendar, String marker) {
                        return Arrays.asList(
                                new SliceEngine.StoredEvent(
                                        1, calendar, CallEvent.from(call(), "UTC")),
                                new SliceEngine.StoredEvent(
                                        2, calendar, CallEvent.from(call(), "UTC")));
                    }
                };
        assertEquals(
                SliceEngine.Result.REVIEW_REQUIRED,
                new SliceEngine(this::call, sink, new Ledger(), "UTC").run(7));
        assertEquals(0, sink.inserts);
    }

    @Test
    public void changedSourceRequiresReview() {
        Sink sink = new Sink();
        Ledger ledger = new Ledger();
        assertEquals(
                SliceEngine.Result.SAVED, new SliceEngine(this::call, sink, ledger, "UTC").run(7));
        CallRecord changed =
                new CallRecord("fixture", "1", 1700000000123L, 99, "Incoming", "Ada", "123", true);
        assertEquals(
                SliceEngine.Result.REVIEW_REQUIRED,
                new SliceEngine(() -> changed, sink, ledger, "UTC").run(7));
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
        Ledger ledger = new Ledger();
        SliceEngine engine = new SliceEngine(this::call, sink, ledger, "UTC");
        assertEquals(SliceEngine.Result.REVIEW_REQUIRED, engine.run(7));
        assertEquals(SliceEngine.Result.REVIEW_REQUIRED, engine.run(7));
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
        Ledger ledger = new Ledger();
        SliceEngine engine = new SliceEngine(this::call, sink, ledger, "UTC");
        assertEquals(SliceEngine.Result.FAILED, engine.run(7));
        assertEquals(SliceEngine.Result.FAILED, engine.run(7));
        assertEquals(1, ledger.rows.size());
        assertEquals(1, sink.inserts);
    }
}
