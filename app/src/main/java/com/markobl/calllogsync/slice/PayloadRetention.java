package com.markobl.calllogsync.slice;

/**
 * Best-effort seven-day frozen-payload retention, measured from storage rather than source time.
 */
public final class PayloadRetention {
    public static final long WINDOW_MILLIS = 7L * 24 * 60 * 60 * 1000;

    private PayloadRetention() {}

    public static boolean expired(Long savedAt, long now) {
        if (now < 0 || savedAt != null && savedAt < 0)
            throw new IllegalArgumentException("Invalid retention clock");
        return savedAt != null && now >= savedAt && now - savedAt >= WINDOW_MILLIS;
    }

    public static DurableDelivery.Intent clear(DurableDelivery.Intent intent) {
        DurableDelivery.State state =
                intent.state == DurableDelivery.State.READY && intent.eventId < 0
                        ? DurableDelivery.State.EXPIRED_READY
                        : intent.state;
        return new DurableDelivery.Intent(
                state,
                intent.key,
                intent.calendar,
                intent.sourceSignature,
                intent.marker,
                intent.fingerprint,
                intent.eventId,
                null,
                intent.operation,
                intent.facts);
    }
}
