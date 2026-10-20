package com.markobl.calllogsync.slice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Read-only legacy heuristic. Even STRONG_CANDIDATE is not proof of ownership or adoption consent.
 */
public final class LegacyEventMatcher {
    private LegacyEventMatcher() {}

    public enum Confidence {
        NONE,
        POSSIBLE,
        STRONG_CANDIDATE,
        AMBIGUOUS,
        INCOMPLETE,
        UNSUPPORTED_SOURCE
    }

    /** Caller-supplied observations only; this class has no provider or receipt mutation port. */
    public static final class Event {
        public final long id, calendar, start, end;
        public final String title, description, marker;

        public Event(
                long id,
                long calendar,
                long start,
                long end,
                String title,
                String description,
                String marker) {
            this.id = id;
            this.calendar = calendar;
            this.start = start;
            this.end = end;
            this.title = title;
            this.description = description;
            this.marker = marker;
        }
    }

    public static final class Result {
        public final Confidence confidence;

        /** IDs only; diagnostic results do not retain caller names/numbers/event text. */
        public final List<Long> eventIds;

        Result(Confidence confidence, List<Long> ids) {
            this.confidence = confidence;
            eventIds = Collections.unmodifiableList(new ArrayList<>(ids));
        }
    }

    /**
     * Expected text must come from an explicit historical rendering profile, not today's mapper.
     */
    public static Result match(
            CallRecord source,
            long calendar,
            String expectedTitle,
            String expectedDescription,
            List<Event> events,
            boolean eventsComplete,
            List<CallRecord> nearby,
            boolean nearbyComplete) {
        if (source == null || calendar < 0 || expectedTitle == null || expectedTitle.isEmpty())
            return result(Confidence.UNSUPPORTED_SOURCE);
        long[] tuple;
        try {
            tuple = legacyTimes(source);
        } catch (ArithmeticException | IllegalArgumentException unsupported) {
            return result(Confidence.UNSUPPORTED_SOURCE);
        }
        if (!eventsComplete
                || !nearbyComplete
                || events == null
                || nearby == null
                || events.size() > 500
                || nearby.size() > 500) return result(Confidence.INCOMPLETE);
        Set<Long> eventIds = new HashSet<>();
        for (Event event : events)
            if (event == null
                    || event.id < 0
                    || event.calendar < 0
                    || event.start < 0
                    || event.end < event.start
                    || !eventIds.add(event.id)) return result(Confidence.INCOMPLETE);
        boolean sourcePresent = false, collision = false;
        Set<String> sourceKeys = new HashSet<>();
        for (CallRecord neighbor : nearby) {
            if (neighbor == null || !sourceKeys.add(neighbor.key()))
                return result(Confidence.INCOMPLETE);
            if (neighbor.key().equals(source.key())) {
                if (!PagedCalls.binding(neighbor).equals(PagedCalls.binding(source)))
                    return result(Confidence.INCOMPLETE);
                sourcePresent = true;
                continue;
            }
            try {
                long[] other = legacyTimes(neighbor);
                // Conservative: two source rows with the same rounded time/duration can collide.
                if (tuple[0] == other[0] && tuple[1] == other[1]) collision = true;
            } catch (ArithmeticException | IllegalArgumentException unsupported) {
                return result(Confidence.INCOMPLETE);
            }
        }
        if (!sourcePresent) return result(Confidence.INCOMPLETE);
        List<Long> matches = new ArrayList<>();
        boolean exact = false;
        for (Event event : events) {
            if (event.calendar != calendar
                    || event.start != tuple[0]
                    || event.end != tuple[1]
                    || event.marker != null && !event.marker.isEmpty()) continue;
            boolean title = expectedTitle.equals(event.title);
            boolean description =
                    expectedDescription != null
                            && !expectedDescription.isEmpty()
                            && expectedDescription.equals(event.description);
            if (!title && !description) continue;
            matches.add(event.id);
            exact = title && description;
        }
        Collections.sort(matches);
        if (matches.isEmpty()) return result(Confidence.NONE);
        Confidence confidence =
                matches.size() > 1 || collision
                        ? Confidence.AMBIGUOUS
                        : exact
                                        && source.identityAllowed
                                        && source.number != null
                                        && source.number.length() >= 3
                                ? Confidence.STRONG_CANDIDATE
                                : Confidence.POSSIBLE;
        return new Result(confidence, matches);
    }

    private static long[] legacyTimes(CallRecord call) {
        if (call.type < 1 || call.type > 3 || call.seconds > Integer.MAX_VALUE / 1000)
            throw new IllegalArgumentException("Unsupported legacy source");
        // Audited APK rounded using double division and Math.round. Do not emulate int overflow.
        long start = Math.multiplyExact(Math.round((double) call.start / 1000.0), 1000L);
        return new long[] {start, Math.addExact(start, Math.multiplyExact(call.seconds, 1000L))};
    }

    private static Result result(Confidence confidence) {
        return new Result(confidence, Collections.emptyList());
    }
}
