package com.markobl.calllogsync.slice

import java.util.function.BooleanSupplier

/** One-page activation-bound runner; stale workers fail closed. */
class AutomaticDelivery(
    private val activation: ActivationControl?,
    private val source: PagedCalls.Source?,
    private val progress: Progress?,
    private val delivery: DurableDelivery?,
) {
    interface Progress : ScanDelivery.Progress {
        fun pending(
            operation: DurableDelivery.Operation?,
            calendar: Long,
            limit: Int,
        ): List<String?>?

        fun rollover(
            operation: DurableDelivery.Operation?,
            expected: PagedCalls.Checkpoint?,
            next: PagedCalls.Checkpoint?,
        )
    }

    private val scan = ScanDelivery(source, progress, delivery)

    fun run(worker: ActivationControl.Settings?, limit: Int): ScanDelivery.Batch =
        run(worker, limit, BooleanSupplier { true })

    fun run(
        worker: ActivationControl.Settings?,
        limit: Int,
        running: BooleanSupplier?,
    ): ScanDelivery.Batch {
        require(limit in 1..500) { "Invalid page limit" }
        return synchronized(DurableDelivery.DELIVERY_LOCK) {
            try {
                if (!permitted(worker, running)) return stopped(DurableDelivery.Result.PAUSED)
                val settings = worker!!
                val pending = progress!!.pending(settings.operation(), settings.calendar, limit)
                check(
                    pending != null &&
                        pending.size <= limit &&
                        HashSet(pending).size == pending.size
                ) {
                    "Invalid pending batch"
                }
                if (pending.isNotEmpty()) {
                    var handled = 0
                    var delivered = 0
                    var review = 0
                    for (key in pending) {
                        check(!key.isNullOrEmpty()) { "Invalid pending key" }
                        val result =
                            delivery!!.resume(key, settings.calendar, settings.operation()) {
                                permitted(worker, running)
                            }!!
                        when (result) {
                            DurableDelivery.Result.SAVED,
                            DurableDelivery.Result.RECOVERED,
                            DurableDelivery.Result.ALREADY_LOGGED -> delivered++
                            DurableDelivery.Result.REVIEW_REQUIRED,
                            DurableDelivery.Result.SOURCE_CHANGED -> review++
                            else ->
                                return ScanDelivery.Batch(
                                    handled,
                                    delivered,
                                    0,
                                    review,
                                    false,
                                    result,
                                )
                        }
                        handled++
                    }
                    // Recovery and source scanning remain separate invocations.
                    return ScanDelivery.Batch(handled, delivered, 0, review, false, null)
                }
                val checkpoint = progress.get(settings.operation())
                if (checkpoint != null && checkpoint.lastId == checkpoint.upperId) {
                    val next = PagedCalls.extend(source, checkpoint)
                    if (!permitted(worker, running)) return stopped(DurableDelivery.Result.PAUSED)
                    progress.rollover(settings.operation(), checkpoint, next)
                }
                scan.run(
                    settings.operation(),
                    settings.policy(),
                    settings.calendar,
                    settings.timezone,
                    limit,
                ) {
                    permitted(worker, running)
                }
            } catch (changed: PagedCalls.SourceChanged) {
                stopped(DurableDelivery.Result.SOURCE_CHANGED)
            } catch (denied: SecurityException) {
                stopped(DurableDelivery.Result.PERMISSION_REQUIRED)
            } catch (failed: RuntimeException) {
                stopped(DurableDelivery.Result.FAILED)
            }
        }
    }

    private fun permitted(worker: ActivationControl.Settings?, running: BooleanSupplier?): Boolean =
        running!!.asBoolean && activation!!.allowed(worker)

    private fun stopped(result: DurableDelivery.Result): ScanDelivery.Batch =
        ScanDelivery.Batch(0, 0, 0, 0, false, result)
}
