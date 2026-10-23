package com.markobl.calllogsync.slice;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Single-process durable delivery. Every Store.put must commit before returning. The process lock
 * serializes UI/workers while individual SQL writes survive process death.
 */
public final class DurableDelivery {
    static final Object DELIVERY_LOCK = new Object();

    public enum State {
        READY,
        IN_FLIGHT,
        ATTEMPTED,
        CONFIRMED,
        REVIEW,
        SUPPRESSED,
        LEGACY
    }

    public enum Result {
        SAVED,
        RECOVERED,
        ALREADY_LOGGED,
        REVIEW_REQUIRED,
        SOURCE_CHANGED,
        PAUSED,
        PERMISSION_REQUIRED,
        DESTINATION_UNAVAILABLE,
        INVALID_CALL,
        FAILED,
        NOTHING_PENDING
    }

    public interface Store {
        Intent get(String key, long calendar);

        void put(Intent intent);
    }

    public static final class Intent {
        public final State state;
        public final String key, sourceSignature, marker, fingerprint;
        public final long calendar, eventId;
        public final CallEvent payload;

        public Intent(
                State state,
                String key,
                long calendar,
                String sourceSignature,
                String marker,
                String fingerprint,
                long eventId,
                CallEvent payload) {
            this.state = Objects.requireNonNull(state);
            this.key = Objects.requireNonNull(key);
            this.calendar = calendar;
            this.sourceSignature = sourceSignature;
            this.marker = marker;
            this.fingerprint = Objects.requireNonNull(fingerprint);
            this.eventId = eventId;
            this.payload = payload;
        }

        Intent state(State next, long id) {
            // Confirmed/review receipts need hashes and identity, not names/numbers in payload.
            CallEvent retained =
                    next == State.CONFIRMED || next == State.REVIEW || next == State.SUPPRESSED
                            ? null
                            : payload;
            return new Intent(
                    next, key, calendar, sourceSignature, marker, fingerprint, id, retained);
        }

        Intent bind(String signature) {
            return new Intent(
                    state, key, calendar, signature, marker, fingerprint, eventId, payload);
        }
    }

    private final Store store;
    private final SliceEngine.CalendarSink sink;

    public DurableDelivery(Store store, SliceEngine.CalendarSink sink) {
        this.store = store;
        this.sink = sink;
    }

    public Result deliver(
            CallRecord call, long calendar, String timezone, BooleanSupplier allowed) {
        synchronized (DELIVERY_LOCK) {
            try {
                if (!allowed.getAsBoolean()) return Result.PAUSED;
                if (calendar < 0 || !sink.writable(calendar)) return Result.DESTINATION_UNAVAILABLE;
                String signature = CallIdentity.signature(call);
                Intent intent = store.get(call.key(), calendar);
                if (intent == null) {
                    CallEvent event = CallEvent.from(call, timezone);
                    intent =
                            new Intent(
                                    State.READY,
                                    call.key(),
                                    calendar,
                                    signature,
                                    event.marker,
                                    event.fingerprint(),
                                    -1,
                                    event);
                    store.put(intent);
                } else if (intent.sourceSignature == null) {
                    // A v1 receipt has no source signature: bind only if its original payload
                    // agrees.
                    if (!intent.fingerprint.equals(CallEvent.from(call, timezone).fingerprint()))
                        return review(intent, Result.REVIEW_REQUIRED);
                    intent = intent.bind(signature);
                    store.put(intent);
                } else if (!intent.sourceSignature.equals(signature)) {
                    return review(intent, Result.SOURCE_CHANGED);
                }
                return attempt(intent, allowed);
            } catch (SecurityException e) {
                return Result.PERMISSION_REQUIRED;
            } catch (ArithmeticException | IllegalArgumentException e) {
                return Result.INVALID_CALL;
            } catch (RuntimeException e) {
                return Result.FAILED;
            }
        }
    }

    /** Resume a committed payload even when the original call-log row no longer exists. */
    public Result resume(String key, long calendar, BooleanSupplier allowed) {
        synchronized (DELIVERY_LOCK) {
            try {
                if (!allowed.getAsBoolean()) return Result.PAUSED;
                if (calendar < 0 || !sink.writable(calendar)) return Result.DESTINATION_UNAVAILABLE;
                Intent intent = store.get(key, calendar);
                return intent == null ? Result.NOTHING_PENDING : attempt(intent, allowed);
            } catch (SecurityException e) {
                return Result.PERMISSION_REQUIRED;
            } catch (RuntimeException e) {
                return Result.FAILED;
            }
        }
    }

    private Result attempt(Intent intent, BooleanSupplier allowed) {
        if (intent.state == State.REVIEW || intent.state == State.SUPPRESSED)
            return Result.REVIEW_REQUIRED;
        if (intent.eventId >= 0) return verify(intent, false);
        if (intent.state == State.IN_FLIGHT) return recover(intent);
        if (intent.state != State.READY || intent.payload == null)
            return review(intent, Result.REVIEW_REQUIRED);
        if (!allowed.getAsBoolean()) return Result.PAUSED;
        if (!sink.writable(intent.calendar)) return Result.DESTINATION_UNAVAILABLE;
        // This committed transition is the cross-store boundary; never undo it on
        // exceptions.
        intent = intent.state(State.IN_FLIGHT, -1);
        store.put(intent);
        long id = sink.insert(intent.calendar, intent.payload);
        if (id < 0) return review(intent, Result.REVIEW_REQUIRED);
        intent = intent.state(State.ATTEMPTED, id);
        store.put(intent);
        return verify(intent, true);
    }

    private Result recover(Intent intent) {
        List<SliceEngine.StoredEvent> found = sink.find(intent.calendar, intent.marker);
        if (found.size() != 1 || !matches(found.get(0), intent))
            return review(intent, Result.REVIEW_REQUIRED);
        Intent known = intent.state(State.ATTEMPTED, found.get(0).id);
        store.put(known);
        Result result = verify(known, false);
        return result == Result.ALREADY_LOGGED ? Result.RECOVERED : result;
    }

    private Result verify(Intent intent, boolean inserted) {
        if (!matches(sink.read(intent.eventId), intent))
            return review(intent, Result.REVIEW_REQUIRED);
        store.put(intent.state(State.CONFIRMED, intent.eventId));
        return inserted ? Result.SAVED : Result.ALREADY_LOGGED;
    }

    private boolean matches(SliceEngine.StoredEvent event, Intent intent) {
        return event != null
                && event.calendar == intent.calendar
                && event.event != null
                && intent.fingerprint.equals(event.event.fingerprint());
    }

    private Result review(Intent intent, Result result) {
        store.put(intent.state(State.REVIEW, intent.eventId));
        return result;
    }
}
