package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

public class AutomaticDeliveryTest {
    static class Progress extends ScanDeliveryTest.Progress implements AutomaticDelivery.Progress {
        public java.util.List<String> pending(
                DurableDelivery.Operation op, long calendar, int limit) {
            return java.util.Collections.emptyList();
        }

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

    @Test
    public void pendingInsertRecoversBeforeDeletedSourceCanBlockTraversal() {
        ActivationControl control = new ActivationControl(new ActivationControlTest.Store());
        ActivationControl.Settings settings = control.activate(7, "UTC", 1000, true, true, true);
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        SliceTest.Sink sink =
                new SliceTest.Sink() {
                    boolean crash = true;

                    @Override
                    public long insert(long calendar, CallEvent event) {
                        long id = super.insert(calendar, event);
                        if (crash) {
                            crash = false;
                            throw new DurableDeliveryTest.Crash();
                        }
                        return id;
                    }
                };
        DurableDelivery delivery = new DurableDelivery(store, sink);
        CallRecord call = PagedCallsTest.row(1, 1);
        assertThrows(
                DurableDeliveryTest.Crash.class,
                () -> delivery.deliver(call, 7, "UTC", settings.operation(), () -> true));
        Progress progress =
                new Progress() {
                    @Override
                    public java.util.List<String> pending(
                            DurableDelivery.Operation op, long calendar, int limit) {
                        DurableDelivery.Intent intent = store.get(call.key(), calendar);
                        return intent.state == DurableDelivery.State.IN_FLIGHT
                                ? java.util.Collections.singletonList(call.key())
                                : java.util.Collections.emptyList();
                    }
                };
        PagedCallsTest.Source source =
                new PagedCallsTest.Source() {
                    @Override
                    public String namespace() {
                        throw new AssertionError("Source must not be accessed during recovery");
                    }
                };
        ScanDelivery.Batch result =
                new AutomaticDelivery(control, source, progress, delivery).run(settings, 4);
        assertNull(result.stopped);
        assertEquals(1, result.delivered);
        assertFalse(result.complete);
        assertEquals(1, sink.inserts);
        assertEquals(DurableDelivery.State.CONFIRMED, store.get(call.key(), 7).state);
    }

    @Test
    public void workerStopAfterIntentCommitPreventsProviderInsertion() {
        ActivationControl control = new ActivationControl(new ActivationControlTest.Store());
        ActivationControl.Settings settings = control.activate(7, "UTC", 1000, true, true, true);
        java.util.concurrent.atomic.AtomicBoolean running =
                new java.util.concurrent.atomic.AtomicBoolean(true);
        DurableDeliveryTest.Store receipts =
                new DurableDeliveryTest.Store() {
                    @Override
                    public void put(DurableDelivery.Intent intent) {
                        super.put(intent);
                        if (intent.state == DurableDelivery.State.READY) running.set(false);
                    }
                };
        PagedCallsTest.Source source = new PagedCallsTest.Source();
        source.rows.put(1L, PagedCallsTest.row(1, 1));
        SliceTest.Sink sink = new SliceTest.Sink();
        Progress progress = new Progress();
        AutomaticDelivery runner =
                new AutomaticDelivery(
                        control, source, progress, new DurableDelivery(receipts, sink));
        assertEquals(DurableDelivery.Result.PAUSED, runner.run(settings, 4, running::get).stopped);
        assertEquals(0, sink.inserts);
        assertEquals(-1, progress.value.lastId);
        assertEquals(DurableDelivery.State.READY, receipts.get(source.rows.get(1L).key(), 7).state);
    }

    @Test
    public void recoveryAndFollowingScanCountOneNewlyConfirmedReceipt() {
        ActivationControl control = new ActivationControl(new ActivationControlTest.Store());
        ActivationControl.Settings settings = control.activate(7, "UTC", 1000, true, true, true);
        CallRecord call = PagedCallsTest.row(1, 1);
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        SliceTest.Sink sink = new SliceTest.Sink();
        CallEvent event = CallEvent.from(call, "UTC");
        sink.insert(7, event);
        store.put(
                new DurableDelivery.Intent(
                        DurableDelivery.State.IN_FLIGHT,
                        call.key(),
                        7,
                        CallIdentity.signature(call),
                        event.marker,
                        event.fingerprint(),
                        -1,
                        event,
                        settings.operation(),
                        DurableDelivery.SourceFacts.from(call)));
        Progress progress =
                new Progress() {
                    @Override
                    public java.util.List<String> pending(
                            DurableDelivery.Operation op, long calendar, int limit) {
                        return store.get(call.key(), calendar).state
                                        == DurableDelivery.State.IN_FLIGHT
                                ? java.util.Collections.singletonList(call.key())
                                : java.util.Collections.emptyList();
                    }
                };
        PagedCallsTest.Source source = new PagedCallsTest.Source();
        source.rows.put(1L, call);
        AutomaticDelivery runner =
                new AutomaticDelivery(control, source, progress, new DurableDelivery(store, sink));
        ScanDelivery.Batch recovery = runner.run(settings, 4);
        ScanDelivery.Batch scan = runner.run(settings, 4);
        assertEquals(1, recovery.delivered);
        assertTrue(scan.complete);
        assertEquals(0, scan.delivered);
        assertEquals(1, recovery.delivered + scan.delivered);
        assertEquals(1, sink.inserts);
    }
}
