package com.markobl.calllogsync.slice;

/**
 * Counts span all saved destinations; verified means a past readback, not current event existence.
 */
public final class DeliveryCounts {
    public final long pending, verified, review, suppressed;

    public DeliveryCounts(long pending, long verified, long review, long suppressed) {
        this.pending = pending;
        this.verified = verified;
        this.review = review;
        this.suppressed = suppressed;
    }
}
