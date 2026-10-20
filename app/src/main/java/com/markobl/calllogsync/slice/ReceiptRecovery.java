package com.markobl.calllogsync.slice;

import java.util.List;

/** Explicit inspection/reconciliation of existing effects; there is deliberately no insert port. */
public final class ReceiptRecovery {
    public interface ExistingEvents {
        SliceEngine.StoredEvent read(long id);

        List<SliceEngine.StoredEvent> find(long calendar, String marker);
    }

    public enum Result {
        VERIFIED,
        REVIEW_REQUIRED,
        NEVER_ATTEMPTED,
        SUPPRESSED,
        PERMISSION_REQUIRED,
        FAILED,
        NOT_FOUND
    }

    private final DurableDelivery.Store store;
    private final ExistingEvents events;

    public ReceiptRecovery(DurableDelivery.Store store, ExistingEvents events) {
        this.store = store;
        this.events = events;
    }

    public Result reconcile(String key, long calendar) {
        validate(key, calendar);
        synchronized (DurableDelivery.DELIVERY_LOCK) {
            try {
                DurableDelivery.Intent intent = store.get(key, calendar);
                if (intent == null) return Result.NOT_FOUND;
                if (intent.state == DurableDelivery.State.SUPPRESSED) return Result.SUPPRESSED;
                if (intent.eventId < 0) {
                    if (intent.state == DurableDelivery.State.READY) return Result.NEVER_ATTEMPTED;
                    if (intent.marker == null || intent.marker.isEmpty()) return review(intent);
                    List<SliceEngine.StoredEvent> found = events.find(calendar, intent.marker);
                    if (found == null || found.size() != 1 || !matches(found.get(0), intent))
                        return review(intent);
                    // Preserve a discovered ID before a second provider read which can fail.
                    intent = intent.state(DurableDelivery.State.ATTEMPTED, found.get(0).id);
                    store.put(intent);
                }
                SliceEngine.StoredEvent existing = events.read(intent.eventId);
                if (!matches(existing, intent) || existing.id != intent.eventId)
                    return review(intent);
                store.put(intent.state(DurableDelivery.State.CONFIRMED, intent.eventId));
                return Result.VERIFIED;
            } catch (SecurityException denied) {
                return Result.PERMISSION_REQUIRED;
            } catch (RuntimeException failed) {
                return Result.FAILED;
            }
        }
    }

    /**
     * Retains source/event identity and clears personal payload; calendar data is never deleted.
     */
    public Result suppress(String key, long calendar) {
        validate(key, calendar);
        synchronized (DurableDelivery.DELIVERY_LOCK) {
            try {
                DurableDelivery.Intent intent = store.get(key, calendar);
                if (intent == null) return Result.NOT_FOUND;
                store.put(intent.state(DurableDelivery.State.SUPPRESSED, intent.eventId));
                return Result.SUPPRESSED;
            } catch (RuntimeException failed) {
                return Result.FAILED;
            }
        }
    }

    private Result review(DurableDelivery.Intent intent) {
        store.put(intent.state(DurableDelivery.State.REVIEW, intent.eventId));
        return Result.REVIEW_REQUIRED;
    }

    private static boolean matches(SliceEngine.StoredEvent event, DurableDelivery.Intent intent) {
        return event != null
                && event.id >= 0
                && event.calendar == intent.calendar
                && event.event != null
                && intent.fingerprint.equals(event.event.fingerprint());
    }

    private static void validate(String key, long calendar) {
        if (key == null || key.isEmpty() || calendar < 0)
            throw new IllegalArgumentException("Invalid recovery key");
    }
}
