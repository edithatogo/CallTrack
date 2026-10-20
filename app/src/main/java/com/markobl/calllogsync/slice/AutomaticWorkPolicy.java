package com.markobl.calllogsync.slice;

/** Retry transient failures with a bound; review/permissions await the next user/system wake. */
public final class AutomaticWorkPolicy {
    private AutomaticWorkPolicy() {}

    public enum Action {
        CONTINUE,
        RETRY,
        STOP
    }

    public static Action decide(ScanDelivery.Batch batch, int attempt) {
        if (batch.stopped == DurableDelivery.Result.FAILED)
            return attempt < 5 ? Action.RETRY : Action.STOP;
        if (batch.stopped != null || batch.complete) return Action.STOP;
        return Action.CONTINUE;
    }
}
