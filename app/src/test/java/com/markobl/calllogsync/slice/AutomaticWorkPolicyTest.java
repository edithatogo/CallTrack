package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

public class AutomaticWorkPolicyTest {
    @Test
    public void transientFailureRetriesButPermissionAndReviewWaitForNextWake() {
        assertEquals(
                AutomaticWorkPolicy.Action.RETRY,
                AutomaticWorkPolicy.decide(
                        new ScanDelivery.Batch(0, 0, 0, 0, false, DurableDelivery.Result.FAILED),
                        0));
        assertEquals(
                AutomaticWorkPolicy.Action.STOP,
                AutomaticWorkPolicy.decide(
                        new ScanDelivery.Batch(
                                0, 0, 0, 0, false, DurableDelivery.Result.PERMISSION_REQUIRED),
                        0));
        assertEquals(
                AutomaticWorkPolicy.Action.STOP,
                AutomaticWorkPolicy.decide(
                        new ScanDelivery.Batch(
                                0, 0, 0, 0, false, DurableDelivery.Result.SOURCE_CHANGED),
                        0));
        assertEquals(
                AutomaticWorkPolicy.Action.STOP,
                AutomaticWorkPolicy.decide(
                        new ScanDelivery.Batch(0, 0, 0, 0, false, DurableDelivery.Result.FAILED),
                        5));
    }

    @Test
    public void incompleteBatchesContinueAndCompleteOrPausedBatchesStop() {
        assertEquals(
                AutomaticWorkPolicy.Action.CONTINUE,
                AutomaticWorkPolicy.decide(new ScanDelivery.Batch(50, 50, 0, 0, false, null), 0));
        assertEquals(
                AutomaticWorkPolicy.Action.STOP,
                AutomaticWorkPolicy.decide(new ScanDelivery.Batch(1, 1, 0, 0, true, null), 0));
        assertEquals(
                AutomaticWorkPolicy.Action.STOP,
                AutomaticWorkPolicy.decide(
                        new ScanDelivery.Batch(0, 0, 0, 0, false, DurableDelivery.Result.PAUSED),
                        0));
    }
}
