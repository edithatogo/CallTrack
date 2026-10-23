package com.markobl.calllogsync.slice;

import android.content.Context;

import androidx.work.*;

import java.util.concurrent.TimeUnit;

/** Best-effort prompt work plus persistent periodic recovery; neither promises a fixed deadline. */
public final class AutomaticScheduling {
    static final String PERIODIC = "calltrack.automatic.periodic";
    static final String TAG = "calltrack.automatic";

    private AutomaticScheduling() {}

    public static ActivationControl.Settings activate(
            Context context,
            long calendar,
            String timezone,
            boolean incoming,
            boolean outgoing,
            boolean missed) {
        return control(context)
                .activate(
                        calendar, timezone, System.currentTimeMillis(), incoming, outgoing, missed);
    }

    public static void pause(Context context) {
        control(context).pause();
    }

    private static AutomaticScheduleControl control(Context context) {
        return new AutomaticScheduleControl(
                new ActivationControl(new AndroidActivationStore(context)),
                settings -> schedule(context, settings),
                () -> WorkManager.getInstance(context).cancelAllWorkByTag(TAG));
    }

    public static void reconcile(Context context) {
        requests(context);
    }

    private static java.util.concurrent.Future<?>[] requests(Context context) {
        return control(context).reconcile();
    }

    static void reconcileCommitted(Context context, long deadlineNanos)
            throws java.util.concurrent.ExecutionException,
                    InterruptedException,
                    java.util.concurrent.TimeoutException {
        remainingBudget(deadlineNanos);
        java.util.concurrent.Future<?> admitted =
                WakeAdmission.EXECUTOR.submit(
                        () -> {
                            awaitCommitted(
                                    control(context)
                                            .reconcile(
                                                    () ->
                                                            deadlineNanos - System.nanoTime() > 0
                                                                    && !Thread.currentThread()
                                                                            .isInterrupted()),
                                    deadlineNanos);
                            return null;
                        });
        try {
            admitted.get(remainingBudget(deadlineNanos), TimeUnit.NANOSECONDS);
        } finally {
            if (!admitted.isDone()) admitted.cancel(true);
            WakeAdmission.EXECUTOR.purge();
        }
    }

    // One blocked admission plus one waiting request; later wakes fail unconfirmed rather
    // than accumulating unbounded threads/requests. Periodic and reopen recovery remain active.
    private static final class WakeAdmission {
        static final java.util.concurrent.ThreadPoolExecutor EXECUTOR = create();

        private static java.util.concurrent.ThreadPoolExecutor create() {
            java.util.concurrent.ThreadPoolExecutor executor =
                    new java.util.concurrent.ThreadPoolExecutor(
                            1,
                            1,
                            30,
                            TimeUnit.SECONDS,
                            new java.util.concurrent.ArrayBlockingQueue<>(1),
                            task -> {
                                Thread thread = new Thread(task, "CallTrackWakeAdmission");
                                thread.setDaemon(true);
                                return thread;
                            });
            executor.allowCoreThreadTimeOut(true);
            return executor;
        }
    }

    private static long remainingBudget(long deadlineNanos)
            throws java.util.concurrent.TimeoutException {
        long remaining = deadlineNanos - System.nanoTime();
        if (remaining <= 0)
            throw new java.util.concurrent.TimeoutException("Wake enqueue unconfirmed");
        return remaining;
    }

    static void awaitCommitted(java.util.concurrent.Future<?>[] operations, long deadlineNanos)
            throws java.util.concurrent.ExecutionException,
                    InterruptedException,
                    java.util.concurrent.TimeoutException {
        remainingBudget(deadlineNanos);
        for (java.util.concurrent.Future<?> operation : operations) {
            operation.get(remainingBudget(deadlineNanos), TimeUnit.NANOSECONDS);
        }
    }

    private static Data input(ActivationControl.Settings settings) {
        return new Data.Builder()
                .putLong(AutomaticLogWorker.GENERATION, settings.generation)
                .build();
    }

    static OneTimeWorkRequest prompt(ActivationControl.Settings settings) {
        OneTimeWorkRequest.Builder request =
                new OneTimeWorkRequest.Builder(AutomaticLogWorker.class)
                        .setInputData(input(settings))
                        .addTag(TAG)
                        .setInitialDelay(2, TimeUnit.SECONDS)
                        .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS);
        // Delayed requests cannot be expedited; use ordinary work to allow call-log provider
        // settling.
        return request.build();
    }

    static PeriodicWorkRequest periodic(ActivationControl.Settings settings) {
        return new PeriodicWorkRequest.Builder(AutomaticLogWorker.class, 15, TimeUnit.MINUTES)
                .setInputData(input(settings))
                .addTag(TAG)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                .build();
    }

    private static java.util.concurrent.Future<?>[] schedule(
            Context context, ActivationControl.Settings settings) {
        WorkManager manager = WorkManager.getInstance(context);
        java.util.concurrent.Future<?> periodic =
                manager.enqueueUniquePeriodicWork(
                                PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, periodic(settings))
                        .getResult();
        java.util.concurrent.Future<?> prompt =
                manager.enqueueUniqueWork(
                                "calltrack.automatic.prompt." + settings.generation,
                                ExistingWorkPolicy.APPEND_OR_REPLACE,
                                prompt(settings))
                        .getResult();
        return new java.util.concurrent.Future<?>[] {periodic, prompt};
    }
}
