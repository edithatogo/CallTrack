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
        ActivationControl.Settings settings =
                new ActivationControl(new AndroidActivationStore(context))
                        .activate(
                                calendar,
                                timezone,
                                System.currentTimeMillis(),
                                incoming,
                                outgoing,
                                missed);
        schedule(context, settings);
        return settings;
    }

    public static void pause(Context context) {
        new ActivationControl(new AndroidActivationStore(context)).pause();
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG);
    }

    public static void reconcile(Context context) {
        requests(context);
    }

    private static java.util.concurrent.Future<?>[] requests(Context context) {
        ActivationControl.Settings settings =
                new ActivationControl(new AndroidActivationStore(context)).current();
        return settings != null && settings.enabled
                ? schedule(context, settings)
                : new java.util.concurrent.Future<?>[0];
    }

    static void reconcileCommitted(Context context, long deadlineNanos)
            throws java.util.concurrent.ExecutionException,
                    InterruptedException,
                    java.util.concurrent.TimeoutException {
        awaitCommitted(requests(context), deadlineNanos);
    }

    static void awaitCommitted(java.util.concurrent.Future<?>[] operations, long deadlineNanos)
            throws java.util.concurrent.ExecutionException,
                    InterruptedException,
                    java.util.concurrent.TimeoutException {
        for (java.util.concurrent.Future<?> operation : operations) {
            long remaining = deadlineNanos - System.nanoTime();
            if (remaining <= 0)
                throw new java.util.concurrent.TimeoutException("Wake enqueue unconfirmed");
            operation.get(remaining, TimeUnit.NANOSECONDS);
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
