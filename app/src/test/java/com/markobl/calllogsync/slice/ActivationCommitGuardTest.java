package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

/** A cache mutation cannot authorise delivery when its durable outcome is unknown. */
public class ActivationCommitGuardTest {
    @Test
    public void successfulCommitRemainsReadable() {
        ActivationCommitGuard guard = new ActivationCommitGuard();
        guard.commit(() -> true);
        guard.checkReadable();
    }

    @Test
    public void falseOutcomeBlocksReadsAndFurtherWrites() {
        ActivationCommitGuard guard = new ActivationCommitGuard();
        assertThrows(IllegalStateException.class, () -> guard.commit(() -> false));
        assertThrows(IllegalStateException.class, guard::checkReadable);
        assertThrows(IllegalStateException.class, () -> guard.commit(() -> true));
    }

    @Test
    public void thrownOutcomeAfterCacheMutationCannotEnableLogging() {
        ActivationCommitGuard guard = new ActivationCommitGuard();
        boolean[] cacheEnabled = {false};
        assertThrows(
                IllegalStateException.class,
                () ->
                        guard.commit(
                                () -> {
                                    cacheEnabled[0] = true;
                                    throw new IllegalStateException("uncertain fixture outcome");
                                }));
        assertTrue(cacheEnabled[0]);
        assertThrows(IllegalStateException.class, guard::checkReadable);
        assertThrows(IllegalStateException.class, () -> guard.commit(() -> true));
    }

    @Test
    public void cacheCannotBeReadAsCommittedWhileWriteIsInProgress() {
        ActivationCommitGuard guard = new ActivationCommitGuard();
        guard.commit(
                () -> {
                    assertThrows(IllegalStateException.class, guard::checkReadable);
                    return true;
                });
        guard.checkReadable();
    }
}
