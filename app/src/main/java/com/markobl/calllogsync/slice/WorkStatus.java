package com.markobl.calllogsync.slice;

/** Nonpersonal execution status; counts failures of worker attempts, not failed call rows. */
public final class WorkStatus {
    public enum Code {
        COMPLETE,
        PAUSED,
        BACKLOG,
        PERMISSION_REQUIRED,
        DESTINATION_UNAVAILABLE,
        SOURCE_CHANGED,
        REVIEW_REQUIRED,
        INVALID_CALL,
        FAILED,
        NOTHING_PENDING,
        UNKNOWN;

        public static Code parse(String value) {
            try {
                return valueOf(value);
            } catch (IllegalArgumentException | NullPointerException invalid) {
                return UNKNOWN;
            }
        }
    }

    public final long generation, at, failures;
    public final String token;
    public final Code code;
    public final int delivered, review;

    public WorkStatus(
            long generation,
            long at,
            String token,
            Code code,
            int delivered,
            int review,
            long failures) {
        if (generation < 1
                || at < 0
                || token == null
                || token.isEmpty()
                || code == null
                || delivered < 0
                || review < 0
                || failures < 0) throw new IllegalArgumentException("Invalid work status");
        this.generation = generation;
        this.at = at;
        this.token = token;
        this.code = code;
        this.delivered = delivered;
        this.review = review;
        this.failures = failures;
    }

    public static WorkStatus next(
            WorkStatus prior,
            long generation,
            long at,
            String token,
            Code code,
            int delivered,
            int review) {
        if (prior != null && prior.generation > generation) return prior;
        long failures = prior != null && prior.generation == generation ? prior.failures : 0;
        if (code == Code.FAILED
                && (prior == null || prior.generation != generation || !prior.token.equals(token))
                && failures < Long.MAX_VALUE) failures++;
        return new WorkStatus(generation, at, token, code, delivered, review, failures);
    }
}
