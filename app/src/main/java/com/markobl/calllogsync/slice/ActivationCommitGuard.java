package com.markobl.calllogsync.slice;

import java.util.function.BooleanSupplier;

/** Process-scoped fail-closed authority after an uncertain settings commit. */
final class ActivationCommitGuard {
    private boolean uncertain;

    synchronized void checkReadable() {
        if (uncertain)
            throw new IllegalStateException("Activation commit uncertain; restart required");
    }

    synchronized void commit(BooleanSupplier write) {
        checkReadable();
        // Latch before invoking Android: a thrown outcome may follow cache mutation.
        uncertain = true;
        if (!write.getAsBoolean()) {
            throw new IllegalStateException("Activation not committed");
        }
        uncertain = false;
    }
}
