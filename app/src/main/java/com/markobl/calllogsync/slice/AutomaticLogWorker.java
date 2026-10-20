package com.markobl.calllogsync.slice;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** Local provider work only; no network constraint or personal caller data in work input/output. */
public final class AutomaticLogWorker extends Worker {
    public static final String GENERATION = "activationGeneration";
    private final String statusToken = java.util.UUID.randomUUID().toString();

    public AutomaticLogWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    @NonNull
    @Override
    public Result doWork() {
        long generation = getInputData().getLong(GENERATION, -1);
        try {
            ActivationControl control =
                    new ActivationControl(new AndroidActivationStore(getApplicationContext()));
            ActivationControl.Settings settings = control.current();
            if (settings == null
                    || !settings.enabled
                    || settings.generation != generation
                    || isStopped()) return report(generation, "PAUSED", 0, 0, false);
            int delivered = 0, review = 0;
            try (SqliteLedger ledger = new SqliteLedger(getApplicationContext())) {
                AutomaticDelivery runner =
                        new AutomaticDelivery(
                                control,
                                new AndroidCallLogSource(getApplicationContext()),
                                ledger,
                                new DurableDelivery(
                                        ledger.outbox(),
                                        new AndroidCalendarSink(getApplicationContext())));
                for (int page = 0; page < 8; page++) {
                    ScanDelivery.Batch batch = runner.run(settings, 50, () -> !isStopped());
                    delivered += batch.delivered;
                    review += batch.review;
                    AutomaticWorkPolicy.Action action =
                            AutomaticWorkPolicy.decide(batch, getRunAttemptCount());
                    if (action == AutomaticWorkPolicy.Action.RETRY)
                        return report(generation, "FAILED", delivered, review, true);
                    if (action == AutomaticWorkPolicy.Action.STOP)
                        return report(
                                generation,
                                batch.stopped == null ? "COMPLETE" : batch.stopped.name(),
                                delivered,
                                review,
                                false);
                }
            }
            return report(generation, "BACKLOG", delivered, review, true);
        } catch (RuntimeException unavailable) {
            return report(generation, "FAILED", 0, 0, getRunAttemptCount() < 5);
        }
    }

    private Result report(long generation, String code, int delivered, int review, boolean retry) {
        // Status is nonpersonal and generation-scoped; receipt data is the authority for delivered
        // events.
        boolean committed;
        try {
            committed =
                    new AndroidWorkStatusStore(getApplicationContext())
                            .publish(
                                    generation,
                                    System.currentTimeMillis(),
                                    statusToken,
                                    WorkStatus.Code.parse(code),
                                    delivered,
                                    review);
        } catch (RuntimeException unavailable) {
            committed = false;
        }
        if (!committed) {
            if (getRunAttemptCount() < 5) return Result.retry();
            return Result.failure(
                    new Data.Builder()
                            .putString("status", "UNKNOWN")
                            .putBoolean("statusStored", false)
                            .build());
        }
        if (retry) return Result.retry();
        return Result.success(
                new Data.Builder()
                        .putString("status", code)
                        .putInt("delivered", delivered)
                        .putInt("review", review)
                        .build());
    }
}
