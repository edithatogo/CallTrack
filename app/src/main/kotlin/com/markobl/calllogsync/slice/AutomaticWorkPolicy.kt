package com.markobl.calllogsync.slice

/** Retry transient failures with a bound; review/permissions await another wake. */
class AutomaticWorkPolicy private constructor() {
    enum class Action {
        CONTINUE,
        RETRY,
        STOP,
    }

    companion object {
        @JvmStatic
        fun decide(batch: ScanDelivery.Batch?, attempt: Int): Action {
            val current = batch!!
            if (current.stopped == DurableDelivery.Result.FAILED)
                return if (attempt < 5) Action.RETRY else Action.STOP
            if (current.stopped != null || current.complete) return Action.STOP
            return Action.CONTINUE
        }
    }
}
