package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** An older caller must not replace newer recovery work or cancel a newer activation. */
public class AutomaticScheduleControlTest {
    @Test
    public void activationCannotPublishWhileAnEarlierRequestIsBeingScheduled() throws Exception {
        verifyOrdering(false);
    }

    @Test
    public void activationCannotPublishBeforeEarlierPauseCancellationIsRequested()
            throws Exception {
        verifyOrdering(true);
    }

    private void verifyOrdering(boolean pause) throws Exception {
        ActivationControl activation = new ActivationControl(new ActivationControlTest.Store());
        activation.activate(7, "UTC", 1000, true, true, true);
        CountDownLatch entered = new CountDownLatch(1),
                release = new CountDownLatch(1),
                started = new CountDownLatch(1);
        AtomicBoolean hold = new AtomicBoolean(true);
        Runnable wait =
                () -> {
                    if (!hold.getAndSet(false)) return;
                    entered.countDown();
                    try {
                        if (!release.await(3, TimeUnit.SECONDS))
                            throw new AssertionError("fixture timeout");
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(interrupted);
                    }
                };
        AutomaticScheduleControl control =
                new AutomaticScheduleControl(
                        activation,
                        settings -> {
                            wait.run();
                            return new Future<?>[0];
                        },
                        wait);
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            Future<?> first =
                    threads.submit(
                            () -> {
                                if (pause) control.pause();
                                else control.reconcile();
                            });
            assertTrue(entered.await(1, TimeUnit.SECONDS));
            Future<?> second =
                    threads.submit(
                            () -> {
                                started.countDown();
                                control.activate(8, "UTC", 2000, true, true, true);
                            });
            assertTrue(started.await(1, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> second.get(100, TimeUnit.MILLISECONDS));
            release.countDown();
            first.get(1, TimeUnit.SECONDS);
            second.get(1, TimeUnit.SECONDS);
            assertEquals(2, activation.current().generation);
            assertEquals(8, activation.current().calendar);
            assertTrue(activation.current().enabled);
        } finally {
            release.countDown();
            threads.shutdownNow();
        }
    }

    @Test
    public void expiredWakeCannotScheduleAfterWaitingForDeliveryLock() throws Exception {
        ActivationControl activation = new ActivationControl(new ActivationControlTest.Store());
        activation.activate(7, "UTC", 1000, true, true, true);
        java.util.concurrent.atomic.AtomicInteger scheduled =
                new java.util.concurrent.atomic.AtomicInteger();
        AtomicBoolean permitted = new AtomicBoolean(true);
        AutomaticScheduleControl control =
                new AutomaticScheduleControl(
                        activation,
                        settings -> {
                            scheduled.incrementAndGet();
                            return new Future<?>[0];
                        },
                        () -> {});
        ExecutorService thread = Executors.newSingleThreadExecutor();
        CountDownLatch started = new CountDownLatch(1);
        Future<Future<?>[]> result;
        try {
            synchronized (DurableDelivery.DELIVERY_LOCK) {
                result =
                        thread.submit(
                                () ->
                                        control.reconcile(
                                                () -> {
                                                    boolean allowed = permitted.get();
                                                    started.countDown();
                                                    return allowed;
                                                }));
                assertTrue(started.await(1, TimeUnit.SECONDS));
                permitted.set(false);
            }
            assertEquals(0, result.get(1, TimeUnit.SECONDS).length);
            assertEquals(0, scheduled.get());
            assertTrue(activation.current().enabled);
        } finally {
            thread.shutdownNow();
        }
    }

    @Test
    public void pausedPolicyDoesNotScheduleAndSchedulingFailureRetainsCommittedPolicy() {
        ActivationControl activation = new ActivationControl(new ActivationControlTest.Store());
        AutomaticScheduleControl control =
                new AutomaticScheduleControl(
                        activation,
                        settings -> {
                            throw new IllegalStateException("fixture enqueue failure");
                        },
                        () -> {});
        assertEquals(0, control.reconcile().length);
        assertThrows(
                IllegalStateException.class,
                () -> control.activate(7, "UTC", 1000, true, true, true));
        assertTrue(activation.current().enabled);
        control.pause();
        assertEquals(0, control.reconcile().length);
    }
}
