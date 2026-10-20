package com.markobl.calllogsync.slice;

import java.util.List;

/**
 * Manual single-call orchestration. Provider insertion and ledger commit have explicit recovery.
 */
public final class SliceEngine {
    public enum Result {
        SAVED,
        ALREADY_LOGGED,
        RECOVERED,
        NO_CALL,
        PERMISSION_REQUIRED,
        DESTINATION_UNAVAILABLE,
        REVIEW_REQUIRED,
        INVALID_CALL,
        FAILED
    }

    public interface CallSource {
        CallRecord latest();
    }

    public interface CalendarSink {
        boolean writable(long calendar);

        List<StoredEvent> find(long calendar, String marker);

        long insert(long calendar, CallEvent event);

        StoredEvent read(long id);
    }

    public interface Work {
        Result run();
    }

    public interface Ledger {
        Result transaction(Work work);

        Entry get(String sourceKey, long calendar);

        void put(String sourceKey, long calendar, Entry entry);
    }

    public static final class Entry {
        public final long id;
        public final String fingerprint;

        public Entry(long id, String fingerprint) {
            this.id = id;
            this.fingerprint = fingerprint;
        }
    }

    public static final class StoredEvent {
        public final long id, calendar;
        public final CallEvent event;

        public StoredEvent(long id, long calendar, CallEvent event) {
            this.id = id;
            this.calendar = calendar;
            this.event = event;
        }
    }

    private final CallSource source;
    private final CalendarSink sink;
    private final Ledger ledger;
    private final String timezone;

    public SliceEngine(CallSource source, CalendarSink sink, Ledger ledger, String timezone) {
        this.source = source;
        this.sink = sink;
        this.ledger = ledger;
        this.timezone = timezone;
    }

    public Result run(long calendar) {
        try {
            if (calendar < 0 || !sink.writable(calendar)) return Result.DESTINATION_UNAVAILABLE;
            CallRecord call = source.latest();
            if (call == null) return Result.NO_CALL;
            CallEvent event = CallEvent.from(call, timezone);
            return ledger.transaction(() -> save(call.key(), calendar, event));
        } catch (SecurityException e) {
            return Result.PERMISSION_REQUIRED;
        } catch (ArithmeticException | IllegalArgumentException e) {
            return Result.INVALID_CALL;
        } catch (RuntimeException e) {
            return Result.FAILED;
        }
    }

    private boolean matches(StoredEvent stored, long calendar, CallEvent event) {
        return stored != null
                && stored.calendar == calendar
                && event.fingerprint().equals(stored.event.fingerprint());
    }

    private Result save(String key, long calendar, CallEvent event) {
        // Recheck immediately before any event mutation; no silent destination fallback.
        if (!sink.writable(calendar)) return Result.DESTINATION_UNAVAILABLE;
        Entry prior = ledger.get(key, calendar);
        if (prior != null) {
            if (!prior.fingerprint.equals(event.fingerprint())
                    || !matches(sink.read(prior.id), calendar, event))
                return Result.REVIEW_REQUIRED;
            return Result.ALREADY_LOGGED;
        }
        List<StoredEvent> found = sink.find(calendar, event.marker);
        if (found.size() > 1) return Result.REVIEW_REQUIRED;
        boolean recovered = !found.isEmpty();
        if (recovered && !matches(found.get(0), calendar, event)) return Result.REVIEW_REQUIRED;
        long id = recovered ? found.get(0).id : sink.insert(calendar, event);
        if (!matches(sink.read(id), calendar, event)) return Result.REVIEW_REQUIRED;
        ledger.put(key, calendar, new Entry(id, event.fingerprint()));
        return recovered ? Result.RECOVERED : Result.SAVED;
    }
}
