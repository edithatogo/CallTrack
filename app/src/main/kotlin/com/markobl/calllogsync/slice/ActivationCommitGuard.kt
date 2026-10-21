package com.markobl.calllogsync.slice

import java.util.function.BooleanSupplier

/** Process-scoped fail-closed authority after an uncertain settings commit. */
internal class ActivationCommitGuard {
    private var uncertain = false

    @Synchronized
    fun checkReadable() {
        if (uncertain) throw IllegalStateException("Activation commit uncertain; restart required")
    }

    @Synchronized
    fun commit(write: BooleanSupplier?) {
        checkReadable()
        // Latch before Android: false/thrown outcomes may follow cache mutation.
        uncertain = true
        if (!write!!.asBoolean) throw IllegalStateException("Activation not committed")
        uncertain = false
    }
}
