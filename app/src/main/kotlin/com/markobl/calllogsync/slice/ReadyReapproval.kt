package com.markobl.calllogsync.slice

import java.util.UUID
import java.util.function.BooleanSupplier

/** One explicit human reapproval of an unchanged, never-attempted frozen intent. */
class ReadyReapproval(
    private val store: DurableDelivery.Store?,
    private val sink: SliceEngine.CalendarSink?,
) {
    fun approve(
        expected: DurableDelivery.Intent?,
        confirmed: BooleanSupplier?,
    ): DurableDelivery.Result =
        synchronized(DurableDelivery.DELIVERY_LOCK) {
            try {
                if (!confirmed!!.asBoolean) return DurableDelivery.Result.PAUSED
                if (!eligible(expected)) return DurableDelivery.Result.REVIEW_REQUIRED
                val initial = expected!!
                val current = store!!.get(initial.key, initial.calendar)
                if (!eligible(current)) return DurableDelivery.Result.REVIEW_REQUIRED
                val saved = current!!
                if (!unchanged(saved, initial)) return DurableDelivery.Result.SOURCE_CHANGED
                if (!sink!!.writable(saved.calendar))
                    return DurableDelivery.Result.DESTINATION_UNAVAILABLE
                if (!confirmed.asBoolean) return DurableDelivery.Result.PAUSED
                val approval =
                    DurableDelivery.Operation("manual-reapproval/" + UUID.randomUUID(), 1)
                store.put(
                    DurableDelivery.Intent(
                        DurableDelivery.State.READY,
                        saved.key,
                        saved.calendar,
                        saved.sourceSignature,
                        saved.marker,
                        saved.fingerprint,
                        -1,
                        saved.payload,
                        approval,
                        saved.facts,
                    )
                )
                DurableDelivery(store, sink).resume(saved.key, saved.calendar, approval, confirmed)
            } catch (denied: SecurityException) {
                DurableDelivery.Result.PERMISSION_REQUIRED
            } catch (failed: RuntimeException) {
                DurableDelivery.Result.FAILED
            }
        }

    companion object {
        @JvmStatic
        fun eligible(intent: DurableDelivery.Intent?): Boolean =
            intent != null &&
                intent.state == DurableDelivery.State.READY &&
                intent.eventId < 0 &&
                intent.payload != null &&
                intent.sourceSignature != null &&
                intent.marker != null &&
                intent.marker == intent.payload.marker &&
                intent.fingerprint == intent.payload.fingerprint()

        private fun unchanged(a: DurableDelivery.Intent, b: DurableDelivery.Intent): Boolean =
            a.key == b.key &&
                a.calendar == b.calendar &&
                a.fingerprint == b.fingerprint &&
                a.sourceSignature == b.sourceSignature &&
                a.marker == b.marker &&
                sameOperation(a.operation, b.operation) &&
                sameFacts(a.facts, b.facts)

        private fun sameOperation(
            a: DurableDelivery.Operation?,
            b: DurableDelivery.Operation?,
        ): Boolean =
            if (a == null) b == null
            else b != null && a.owner == b.owner && a.generation == b.generation

        private fun sameFacts(
            a: DurableDelivery.SourceFacts?,
            b: DurableDelivery.SourceFacts?,
        ): Boolean =
            if (a == null) b == null
            else
                b != null &&
                    a.start == b.start &&
                    a.seconds == b.seconds &&
                    a.type == b.type &&
                    a.presentationSupplied == b.presentationSupplied &&
                    a.presentation == b.presentation
    }
}
