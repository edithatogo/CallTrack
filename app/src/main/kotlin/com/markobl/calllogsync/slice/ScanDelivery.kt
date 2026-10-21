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

    class Policy(
        @JvmField val activationStart: Long,
        @JvmField val incoming: Boolean,
        @JvmField val outgoing: Boolean,
        @JvmField val missed: Boolean,
    ) {
        init {
            require(activationStart >= 0) { "Invalid activation boundary" }
        }

        fun includes(call: CallRecord?): Boolean =
            call!!.start >= activationStart &&
                (call.type == 1 && incoming ||
                    call.type == 2 && outgoing ||
                    call.type == 3 && missed)
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
        require(operation != null && policy != null && allowed != null && limit in 1..500) {
            "Invalid scan request"
        }
        return synchronized(DurableDelivery.DELIVERY_LOCK) {
            var handled = 0
            var delivered = 0
            var excluded = 0
            var review = 0
            try {
                if (!allowed.asBoolean)
                    return Batch(0, 0, 0, 0, false, DurableDelivery.Result.PAUSED)
                var checkpoint = progress!!.get(operation)
                if (checkpoint == null) {
                    checkpoint = PagedCalls.capture(source)
                    progress.put(operation, checkpoint)
                }
                val rows = PagedCalls.read(source, checkpoint, limit)
                for (call in rows) {
                    if (!allowed.asBoolean)
                        return Batch(
                            handled,
                            delivered,
                            excluded,
                            review,
                            false,
                            DurableDelivery.Result.PAUSED,
                        )
                    val current = source!!.at(PagedCalls.id(call))
                    if (
                        current == null ||
                            call.id != current.id ||
                            call.namespace != current.namespace ||
                            PagedCalls.binding(call) != PagedCalls.binding(current)
                    )
                        throw PagedCalls.SourceChanged()
                    if (!policy.includes(call)) excluded++
                    else {
                        val wasConfirmed = delivery!!.previouslyConfirmed(call.key(), calendar)
                        val result =
                            delivery.deliver(call, calendar, timezone, operation, allowed)!!
                        when (result) {
                            DurableDelivery.Result.SAVED,
                            DurableDelivery.Result.RECOVERED,
                            DurableDelivery.Result.ALREADY_LOGGED -> if (!wasConfirmed) delivered++
                            DurableDelivery.Result.REVIEW_REQUIRED,
                            DurableDelivery.Result.SOURCE_CHANGED -> review++
                            else ->
                                return Batch(handled, delivered, excluded, review, false, result)
                        }
                    }
                    checkpoint = checkpoint!!.advance(call)
                    progress.put(operation, checkpoint)
                    handled++
                }
                Batch(
                    handled,
                    delivered,
                    excluded,
                    review,
                    checkpoint!!.lastId == checkpoint.upperId,
                    null,
                )
            } catch (changed: PagedCalls.SourceChanged) {
                Batch(
                    handled,
                    delivered,
                    excluded,
                    review,
                    false,
                    DurableDelivery.Result.SOURCE_CHANGED,
                )
            } catch (denied: SecurityException) {
                Batch(
                    handled,
                    delivered,
                    excluded,
                    review,
                    false,
                    DurableDelivery.Result.PERMISSION_REQUIRED,
                )
            } catch (failed: RuntimeException) {
                Batch(handled, delivered, excluded, review, false, DurableDelivery.Result.FAILED)
            }
        }
    }
}
