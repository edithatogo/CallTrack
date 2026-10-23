package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.util.concurrent.*;

/** A held delivery monitor must not hold the protected broadcast's completion budget. */
public class WakeBudgetTest {
    @Test
    public void heldDeliveryLockCannotHoldWakePastDeadline() throws Exception {
        android.content.Context context =
                InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertNull(new ActivationControl(new AndroidActivationStore(context)).current());
        ExecutorService caller = Executors.newSingleThreadExecutor();
        CountDownLatch entered = new CountDownLatch(1);
        Future<Throwable> result;
        try {
            synchronized (DurableDelivery.DELIVERY_LOCK) {
                result =
                        caller.submit(
                                () -> {
                                    entered.countDown();
                                    try {
                                        AutomaticScheduling.reconcileCommitted(
                                                context,
                                                System.nanoTime()
                                                        + TimeUnit.MILLISECONDS.toNanos(100));
                                        return null;
                                    } catch (Throwable outcome) {
                                        return outcome;
                                    }
                                });
                assertTrue(entered.await(1, TimeUnit.SECONDS));
                // The monitor stays held while the caller must return a deadline failure.
                Throwable outcome = result.get(1, TimeUnit.SECONDS);
                assertTrue(
                        "Expected an explicit unconfirmed enqueue deadline",
                        outcome instanceof TimeoutException);
            }
        } finally {
            caller.shutdownNow();
            assertTrue(caller.awaitTermination(5, TimeUnit.SECONDS));
        }
        assertNull(new ActivationControl(new AndroidActivationStore(context)).current());
    }
}
