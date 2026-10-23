package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.*;

public class LegacyEventMatcherTest {
    private CallRecord call(String id, long start, long seconds) {
        return new CallRecord(
                "fixture", id, start, seconds, 1, "Fixture", "000", Integer.valueOf(1), null);
    }

    private LegacyEventMatcher.Event event(long id, long start, long end, String description) {
        return new LegacyEventMatcher.Event(id, 7, start, end, "Legacy fixture", description, null);
    }

    private LegacyEventMatcher.Result match(
            CallRecord call,
            List<LegacyEventMatcher.Event> events,
            boolean complete,
            List<CallRecord> nearby) {
        return LegacyEventMatcher.match(
                call, 7, "Legacy fixture", "Description fixture", events, complete, nearby, true);
    }

    @Test
    public void exactLegacyTupleAndDescriptionProduceOnlyReadOnlyCandidateId() {
        CallRecord call = call("1", 1500, 2);
        LegacyEventMatcher.Result result =
                match(
                        call,
                        Collections.singletonList(event(9, 2000, 4000, "Description fixture")),
                        true,
                        Collections.singletonList(call));
        assertEquals(LegacyEventMatcher.Confidence.STRONG_CANDIDATE, result.confidence);
        assertEquals(Collections.singletonList(9L), result.eventIds);
        assertThrows(UnsupportedOperationException.class, () -> result.eventIds.add(10L));
        CallRecord beforeHalf = call("2", 1499, 2);
        assertEquals(
                LegacyEventMatcher.Confidence.STRONG_CANDIDATE,
                match(
                                beforeHalf,
                                Collections.singletonList(
                                        event(10, 1000, 3000, "Description fixture")),
                                true,
                                Collections.singletonList(beforeHalf))
                        .confidence);
        assertEquals(
                LegacyEventMatcher.Confidence.POSSIBLE,
                match(
                                call,
                                Collections.singletonList(
                                        new LegacyEventMatcher.Event(
                                                9,
                                                7,
                                                2000,
                                                4000,
                                                "Edited title",
                                                "Description fixture",
                                                null)),
                                true,
                                Collections.singletonList(call))
                        .confidence);
    }

    @Test
    public void duplicateEventsOrRoundedSourceCollisionAreAmbiguous() {
        CallRecord call = call("1", 1500, 2);
        assertEquals(
                LegacyEventMatcher.Confidence.AMBIGUOUS,
                match(
                                call,
                                Arrays.asList(
                                        event(9, 2000, 4000, "Description fixture"),
                                        event(10, 2000, 4000, "Description fixture")),
                                true,
                                Collections.singletonList(call))
                        .confidence);
        assertEquals(
                LegacyEventMatcher.Confidence.AMBIGUOUS,
                match(
                                call,
                                Collections.singletonList(
                                        event(9, 2000, 4000, "Description fixture")),
                                true,
                                Arrays.asList(call, call("2", 1999, 2)))
                        .confidence);
    }

    @Test
    public void unknownIdentityOrChangedDescriptionCannotBecomeStrong() {
        CallRecord call = call("1", 1500, 0);
        assertEquals(
                LegacyEventMatcher.Confidence.POSSIBLE,
                match(
                                call,
                                Collections.singletonList(event(9, 2000, 2000, "Edited notes")),
                                true,
                                Collections.singletonList(call))
                        .confidence);
        CallRecord unknown =
                new CallRecord("fixture", "1", 1500, 0, 1, null, null, Integer.valueOf(2), null);
        assertEquals(
                LegacyEventMatcher.Confidence.POSSIBLE,
                match(
                                unknown,
                                Collections.singletonList(
                                        event(9, 2000, 2000, "Description fixture")),
                                true,
                                Collections.singletonList(unknown))
                        .confidence);
        assertEquals(
                LegacyEventMatcher.Confidence.NONE,
                match(
                                call,
                                Collections.singletonList(
                                        event(9, 2000, 62000, "Description fixture")),
                                true,
                                Collections.singletonList(call))
                        .confidence);
    }

    @Test
    public void incompleteOrMalformedInputsCannotProduceStrongConfidence() {
        CallRecord call = call("1", 1500, 2);
        List<LegacyEventMatcher.Event> exact =
                Collections.singletonList(event(9, 2000, 4000, "Description fixture"));
        assertEquals(
                LegacyEventMatcher.Confidence.INCOMPLETE,
                match(call, exact, false, Collections.singletonList(call)).confidence);
        assertEquals(
                LegacyEventMatcher.Confidence.INCOMPLETE,
                match(
                                call,
                                Arrays.asList(exact.get(0), exact.get(0)),
                                true,
                                Collections.singletonList(call))
                        .confidence);
        assertEquals(
                LegacyEventMatcher.Confidence.INCOMPLETE,
                LegacyEventMatcher.match(
                                call,
                                7,
                                "Legacy fixture",
                                "Description fixture",
                                exact,
                                true,
                                Collections.singletonList(call),
                                false)
                        .confidence);
    }

    @Test
    public void unsupportedOldDurationArithmeticAndManagedMarkersAreExcluded() {
        CallRecord huge = call("1", 1500, 2147484);
        assertEquals(
                LegacyEventMatcher.Confidence.UNSUPPORTED_SOURCE,
                match(huge, Collections.emptyList(), true, Collections.singletonList(huge))
                        .confidence);
        CallRecord call = call("1", 1500, 2);
        assertEquals(
                LegacyEventMatcher.Confidence.NONE,
                match(
                                call,
                                Collections.singletonList(
                                        new LegacyEventMatcher.Event(
                                                9,
                                                7,
                                                2000,
                                                4000,
                                                "Legacy fixture",
                                                "Description fixture",
                                                "calltrack-dev://source/fixture")),
                                true,
                                Collections.singletonList(call))
                        .confidence);
    }
}
