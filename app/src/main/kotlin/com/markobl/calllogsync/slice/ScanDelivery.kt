package com.markobl.calllogsync.slice

import java.util.function.BooleanSupplier

/** One bounded step; durable receipts precede progress so replay cannot duplicate inserts. */
class ScanDelivery(
    private val source: PagedCalls.Source?,
    private val progress: Progress?,
    private val delivery: DurableDelivery?,
) {
    interface Progress {
        fun get(operation: DurableDelivery.Operation?): PagedCalls.Checkpoint?

        fun put(operation: DurableDelivery.Operation?, checkpoint: PagedCalls.Checkpoint?)
    }

    class Policy
    @JvmOverloads
    constructor(
        @JvmField val activationStart: Long,
        @JvmField val incoming: Boolean,
        @JvmField val outgoing: Boolean,
        @JvmField val missed: Boolean,
        private val sources: Set<String> = setOf(CallRecord.PHONE),
    ) {
        init {
            require(activationStart >= 0) { "Invalid activation boundary" }
        }

        private val selection = CallSelection(incoming, outgoing, missed)

        fun includes(call: CallRecord?): Boolean =
            call!!.start >= activationStart && call.source in sources && selection.accepts(call)
    }

    class Batch(
        @JvmField val handled: Int,
        @JvmField val delivered: Int,
        @JvmField val excluded: Int,
        @JvmField val review: Int,
        @JvmField val complete: Boolean,
        @JvmField val stopped: DurableDelivery.Result?,
    )

    fun run(
        operation: DurableDelivery.Operation?,
        policy: Policy?,
        calendar: Long,
        timezone: String?,
        limit: Int,
        allowed: BooleanSupplier?,
    ): Batch {
        require(
            operation != null &&
                policy != null &&
                allowed != null &&
                limit in 1..PagedCalls.MAX_PAGE_SIZE,
        ) {
            "Invalid scan request"
        }
        return synchronized(DurableDelivery.DELIVERY_LOCK) {
            val counts = Counts()
            try {
                if (!allowed.asBoolean) {
                    return@synchronized counts.batch(stopped = DurableDelivery.Result.PAUSED)
                }
                val storage = progress!!
                var checkpoint =
                    storage.get(operation)
                        ?: PagedCalls.capture(source).also { storage.put(operation, it) }
                val rows = PagedCalls.read(source, checkpoint, limit)
                for (call in rows) {
                    if (!allowed.asBoolean) {
                        return@synchronized counts.batch(stopped = DurableDelivery.Result.PAUSED)
                    }
                    verifySource(call)
                    if (policy.includes(call)) {
                        val stopped = deliver(call, calendar, timezone, operation, allowed, counts)
                        if (stopped != null) return@synchronized counts.batch(stopped = stopped)
                    } else {
                        counts.excluded++
                    }
                    checkpoint = checkpoint.advance(call)
                    storage.put(operation, checkpoint)
                    counts.handled++
                }
                counts.batch(complete = checkpoint.lastId == checkpoint.upperId)
            } catch (ignored: PagedCalls.SourceChanged) {
                counts.batch(stopped = DurableDelivery.Result.SOURCE_CHANGED)
            } catch (ignored: SecurityException) {
                counts.batch(stopped = DurableDelivery.Result.PERMISSION_REQUIRED)
            } catch (ignored: RuntimeException) {
                counts.batch(stopped = DurableDelivery.Result.FAILED)
            }
        }
    }

    private fun verifySource(call: CallRecord) {
        val current = source!!.at(PagedCalls.id(call)) ?: throw PagedCalls.SourceChanged()
        if (
            call.id != current.id ||
                call.namespace != current.namespace ||
                PagedCalls.binding(call) != PagedCalls.binding(current)
        ) {
            throw PagedCalls.SourceChanged()
        }
    }

    private fun deliver(
        call: CallRecord,
        calendar: Long,
        timezone: String?,
        operation: DurableDelivery.Operation,
        allowed: BooleanSupplier,
        counts: Counts,
    ): DurableDelivery.Result? {
        val wasConfirmed = delivery!!.previouslyConfirmed(call.key(), calendar)
        val result = delivery.deliver(call, calendar, timezone, operation, allowed)
        when (result) {
            DurableDelivery.Result.SAVED,
            DurableDelivery.Result.RECOVERED,
            DurableDelivery.Result.ALREADY_LOGGED -> if (!wasConfirmed) counts.delivered++

            DurableDelivery.Result.REVIEW_REQUIRED,
            DurableDelivery.Result.SOURCE_CHANGED -> counts.review++

            else -> return result
        }
        return null
    }

    private class Counts {
        var handled = 0
        var delivered = 0
        var excluded = 0
        var review = 0

        fun batch(complete: Boolean = false, stopped: DurableDelivery.Result? = null): Batch =
            Batch(handled, delivered, excluded, review, complete, stopped)
    }
}
