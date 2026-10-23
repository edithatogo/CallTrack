package com.markobl.calllogsync.slice

/** Best-effort payload expiry measured from storage rather than source time. */
class PayloadRetention private constructor() {
    companion object {
        const val WINDOW_MILLIS: Long = 7L * 24 * 60 * 60 * 1000

        @JvmStatic
        fun expired(savedAt: Long?, now: Long): Boolean {
            require(now >= 0 && (savedAt == null || savedAt >= 0)) { "Invalid retention clock" }
            return savedAt != null && now >= savedAt && now - savedAt >= WINDOW_MILLIS
        }

        @JvmStatic
        fun clear(intent: DurableDelivery.Intent?): DurableDelivery.Intent {
            val current = intent!!
            val state =
                if (current.state == DurableDelivery.State.READY && current.eventId < 0)
                    DurableDelivery.State.EXPIRED_READY
                else current.state
            return DurableDelivery.Intent(
                state,
                current.key,
                current.calendar,
                current.sourceSignature,
                current.marker,
                current.fingerprint,
                current.eventId,
                null,
                current.operation,
                current.facts,
            )
        }
    }
}
