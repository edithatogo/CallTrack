package com.markobl.calllogsync.slice;

/**
 * Explicit latest-call action shares the same durable receipts and process lock as background work.
 */
public final class ManualDelivery {
    private final SliceEngine.CallSource source;
    private final DurableDelivery delivery;
    private final String timezone;

    public ManualDelivery(
            SliceEngine.CallSource source, DurableDelivery delivery, String timezone) {
        this.source = source;
        this.delivery = delivery;
        this.timezone = timezone;
    }

    public SliceEngine.Result run(long calendar) {
        try {
            CallRecord call = source.latest();
            if (call == null) return SliceEngine.Result.NO_CALL;
            DurableDelivery.Result result = delivery.deliver(call, calendar, timezone, () -> true);
            switch (result) {
                case SOURCE_CHANGED:
                case PAUSED:
                case REVIEW_REQUIRED:
                    return SliceEngine.Result.REVIEW_REQUIRED;
                case SAVED:
                    return SliceEngine.Result.SAVED;
                case RECOVERED:
                    return SliceEngine.Result.RECOVERED;
                case ALREADY_LOGGED:
                    return SliceEngine.Result.ALREADY_LOGGED;
                case PERMISSION_REQUIRED:
                    return SliceEngine.Result.PERMISSION_REQUIRED;
                case DESTINATION_UNAVAILABLE:
                    return SliceEngine.Result.DESTINATION_UNAVAILABLE;
                case INVALID_CALL:
                    return SliceEngine.Result.INVALID_CALL;
                default:
                    return SliceEngine.Result.FAILED;
            }
        } catch (SecurityException denied) {
            return SliceEngine.Result.PERMISSION_REQUIRED;
        } catch (RuntimeException failed) {
            return SliceEngine.Result.FAILED;
        }
    }
}
