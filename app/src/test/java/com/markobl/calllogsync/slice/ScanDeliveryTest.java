package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

public class ScanDeliveryTest {
    static class Progress implements ScanDelivery.Progress {
        PagedCalls.Checkpoint value;
        boolean crash;

        public PagedCalls.Checkpoint get(DurableDelivery.Operation op) {
            return value;
        }

        public void put(DurableDelivery.Operation op, PagedCalls.Checkpoint next) {
            if (crash && next.lastId >= 0) {
                crash = false;
                throw new DurableDeliveryTest.Crash();
            }
            value = next;
        }
    }

    private ScanDelivery.Policy policy() {
        return new ScanDelivery.Policy(1001, true, false, true);
    }

    @Test
    public void activationAndFiltersAdvanceExcludedRowsAndDeliverAcrossPages() {
        PagedCallsTest.Source source = new PagedCallsTest.Source();
        for (int i = 0; i < 25; i++) source.rows.put((long) i, PagedCallsTest.row(i, 1));
        Progress progress = new Progress();
        SliceTest.Sink sink = new SliceTest.Sink();
        ScanDelivery scan =
                new ScanDelivery(
                        source,
                        progress,
                        new DurableDelivery(new DurableDeliveryTest.Store(), sink));
        DurableDelivery.Operation op = new DurableDelivery.Operation("automatic", 1);
        ScanDelivery.Batch batch;
        do {
            batch = scan.run(op, policy(), 7, "UTC", 4, () -> true);
        } while (!batch.complete);
        assertEquals(24, sink.inserts);
        assertEquals(24, progress.value.lastId);
    }

    @Test
    public void lostCheckpointAfterVerifiedDeliveryDoesNotDuplicate() {
        PagedCallsTest.Source source = new PagedCallsTest.Source();
        source.rows.put(1L, PagedCallsTest.row(1, 1));
        Progress progress = new Progress();
        progress.crash = true;
        SliceTest.Sink sink = new SliceTest.Sink();
        ScanDelivery scan =
                new ScanDelivery(
                        source,
                        progress,
                        new DurableDelivery(new DurableDeliveryTest.Store(), sink));
        DurableDelivery.Operation op = new DurableDelivery.Operation("automatic", 1);
        assertThrows(
                DurableDeliveryTest.Crash.class,
                () -> scan.run(op, policy(), 7, "UTC", 4, () -> true));
        assertEquals(1, sink.inserts);
        assertEquals(-1, progress.value.lastId);
        assertTrue(scan.run(op, policy(), 7, "UTC", 4, () -> true).complete);
        assertEquals(1, sink.inserts);
    }

    @Test
    public void unavailableDestinationAndPauseDoNotConsumeRows() {
        PagedCallsTest.Source source = new PagedCallsTest.Source();
        source.rows.put(1L, PagedCallsTest.row(1, 1));
        Progress progress = new Progress();
        SliceTest.Sink sink = new SliceTest.Sink();
        sink.writable = false;
        ScanDelivery scan =
                new ScanDelivery(
                        source,
                        progress,
                        new DurableDelivery(new DurableDeliveryTest.Store(), sink));
        DurableDelivery.Operation op = new DurableDelivery.Operation("automatic", 1);
        assertEquals(
                DurableDelivery.Result.DESTINATION_UNAVAILABLE,
                scan.run(op, policy(), 7, "UTC", 4, () -> true).stopped);
        assertEquals(-1, progress.value.lastId);
        assertEquals(
                DurableDelivery.Result.PAUSED,
                scan.run(op, policy(), 7, "UTC", 4, () -> false).stopped);
        assertEquals(0, sink.inserts);
    }

    @Test
    public void extendedTypesAndOutgoingAreExcludedWithoutReinterpretation() {
        ScanDelivery.Policy p = policy();
        assertTrue(p.includes(PagedCallsTest.row(1, 1)));
        for (int type : new int[] {2, 4, 5, 6, 7, 99})
            assertFalse(
                    p.includes(
                            new CallRecord(
                                    "fixture",
                                    "1",
                                    1001,
                                    0,
                                    type,
                                    null,
                                    null,
                                    Integer.valueOf(1),
                                    null)));
    }

    @Test
    public void candidateChangedAfterPageReadStopsBeforeInsert() {
        PagedCallsTest.Source source =
                new PagedCallsTest.Source() {
                    int reads;

                    @Override
                    public CallRecord at(long id) {
                        reads++;
                        // Upper anchor checks precede the immediate candidate check.
                        return reads >= 3 ? PagedCallsTest.row(id, 9) : super.at(id);
                    }
                };
        source.rows.put(1L, PagedCallsTest.row(1, 1));
        Progress progress = new Progress();
        SliceTest.Sink sink = new SliceTest.Sink();
        ScanDelivery scan =
                new ScanDelivery(
                        source,
                        progress,
                        new DurableDelivery(new DurableDeliveryTest.Store(), sink));
        ScanDelivery.Batch result =
                scan.run(
                        new DurableDelivery.Operation("automatic", 1),
                        policy(),
                        7,
                        "UTC",
                        4,
                        () -> true);
        assertEquals(DurableDelivery.Result.SOURCE_CHANGED, result.stopped);
        assertEquals(0, sink.inserts);
        assertEquals(-1, progress.value.lastId);
    }
}
