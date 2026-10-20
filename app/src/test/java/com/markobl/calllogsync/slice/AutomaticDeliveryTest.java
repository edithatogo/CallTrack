package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

public class AutomaticDeliveryTest {
    static class Progress extends ScanDeliveryTest.Progress implements AutomaticDelivery.Progress {
        public void rollover(
                DurableDelivery.Operation op,
                PagedCalls.Checkpoint expected,
                PagedCalls.Checkpoint next) {
            if (!PagedCalls.equal(value, expected)) throw new IllegalStateException();
            value = next;
        }
    }

    @Test
    public void newCallsAfterCompletedOrEmptyScanAreDeliveredWithoutReplayingHistory() {
        ActivationControl control = new ActivationControl(new ActivationControlTest.Store());
        ActivationControl.Settings settings = control.activate(7, "UTC", 1000, true, true, true);
        PagedCallsTest.Source source = new PagedCallsTest.Source();
        Progress progress = new Progress();
        SliceTest.Sink sink = new SliceTest.Sink();
        AutomaticDelivery runner =
                new AutomaticDelivery(
                        control,
                        source,
                        progress,
                        new DurableDelivery(new DurableDeliveryTest.Store(), sink));
        assertTrue(runner.run(settings, 4).complete);
        source.rows.put(1L, PagedCallsTest.row(1, 1));
        assertTrue(runner.run(settings, 4).complete);
        assertEquals(1, sink.inserts);
        source.rows.put(2L, PagedCallsTest.row(2, 1));
        assertTrue(runner.run(settings, 4).complete);
        assertEquals(2, sink.inserts);
        assertTrue(runner.run(settings, 4).complete);
        assertEquals(2, sink.inserts);
        control.pause();
        assertEquals(DurableDelivery.Result.PAUSED, runner.run(settings, 4).stopped);
    }

    @Test
    public void deletedProcessedAnchorRequiresReviewAndCannotAdoptReset() {
        ActivationControl control = new ActivationControl(new ActivationControlTest.Store());
        ActivationControl.Settings settings = control.activate(7, "UTC", 1000, true, true, true);
        PagedCallsTest.Source source = new PagedCallsTest.Source();
        source.rows.put(1L, PagedCallsTest.row(1, 1));
        Progress progress = new Progress();
        SliceTest.Sink sink = new SliceTest.Sink();
        AutomaticDelivery runner =
                new AutomaticDelivery(
                        control,
                        source,
                        progress,
                        new DurableDelivery(new DurableDeliveryTest.Store(), sink));
        assertTrue(runner.run(settings, 4).complete);
        source.rows.clear();
        source.rows.put(2L, PagedCallsTest.row(2, 1));
        assertEquals(DurableDelivery.Result.SOURCE_CHANGED, runner.run(settings, 4).stopped);
        assertEquals(1, sink.inserts);
    }
}
