package com.markobl.calllogsync.slice

/** Existing-effect reconciliation; deliberately no insertion port. */
class ReceiptRecovery(
    private val store: DurableDelivery.Store?,
    private val events: ExistingEvents?,
) {
    interface ExistingEvents {
        fun read(id: Long): SliceEngine.StoredEvent?

        fun find(calendar: Long, marker: String?): List<SliceEngine.StoredEvent?>?
    }

    enum class Result {
        VERIFIED,
        REVIEW_REQUIRED,
        NEVER_ATTEMPTED,
        SUPPRESSED,
        PERMISSION_REQUIRED,
        FAILED,
        NOT_FOUND,
    }

    fun reconcile(key: String?, calendar: Long): Result {
        validate(key, calendar)
        return synchronized(DurableDelivery.DELIVERY_LOCK) {
            try {
                var intent = store!!.get(key, calendar) ?: return Result.NOT_FOUND
                if (intent.state == DurableDelivery.State.SUPPRESSED) return Result.SUPPRESSED
                if (intent.eventId < 0) {
                    if (
                        intent.state == DurableDelivery.State.READY ||
                            intent.state == DurableDelivery.State.EXPIRED_READY
                    )
                        return Result.NEVER_ATTEMPTED
                    if (intent.marker.isNullOrEmpty()) return review(intent)
                    val found = events!!.find(calendar, intent.marker)
                    if (found == null || found.size != 1 || !matches(found[0], intent))
                        return review(intent)
                    // Persist discovered identity before the second provider read.
                    intent = intent.state(DurableDelivery.State.ATTEMPTED, found[0]!!.id)
                    store.put(intent)
                }
                val existing = events!!.read(intent.eventId)
                if (!matches(existing, intent) || existing!!.id != intent.eventId)
                    return review(intent)
                store.put(intent.state(DurableDelivery.State.CONFIRMED, intent.eventId))
                Result.VERIFIED
            } catch (denied: SecurityException) {
                Result.PERMISSION_REQUIRED
            } catch (failed: RuntimeException) {
                Result.FAILED
            }
        }
    }

    fun suppress(key: String?, calendar: Long): Result {
        validate(key, calendar)
        return synchronized(DurableDelivery.DELIVERY_LOCK) {
            try {
                val intent = store!!.get(key, calendar) ?: return Result.NOT_FOUND
                store.put(intent.state(DurableDelivery.State.SUPPRESSED, intent.eventId))
                Result.SUPPRESSED
            } catch (failed: RuntimeException) {
                Result.FAILED
            }
        }
    }

    private fun review(intent: DurableDelivery.Intent): Result {
        store!!.put(intent.state(DurableDelivery.State.REVIEW, intent.eventId))
        return Result.REVIEW_REQUIRED
    }

    private fun matches(event: SliceEngine.StoredEvent?, intent: DurableDelivery.Intent): Boolean =
        event != null &&
            event.id >= 0 &&
            event.calendar == intent.calendar &&
            event.event != null &&
            intent.fingerprint == event.event.fingerprint()

    private fun validate(key: String?, calendar: Long) {
        require(!key.isNullOrEmpty() && calendar >= 0) { "Invalid recovery key" }
    }
}
