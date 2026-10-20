package com.markobl.calllogsync.slice;

/** Independent CallLog type switches. Extended/unknown types are excluded, never reclassified. */
public final class CallSelection {
    public final boolean incoming, outgoing, missed;

    public CallSelection(boolean incoming, boolean outgoing, boolean missed) {
        this.incoming = incoming;
        this.outgoing = outgoing;
        this.missed = missed;
    }

    public boolean accepts(CallRecord call) {
        return call.type == 1 ? incoming : call.type == 2 ? outgoing : call.type == 3 && missed;
    }

    public static boolean inRange(CallRecord call, long startInclusive, long endExclusive) {
        if (startInclusive < 0 || endExclusive <= startInclusive)
            throw new IllegalArgumentException("Invalid call range");
        return call.start >= startInclusive && call.start < endExclusive;
    }
}
