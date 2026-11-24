package com.markobl.calllogsync.slice

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.UUID

/** Local provider work only; work input/output contain no personal call fields. */
class AutomaticLogWorker(context: Context, parameters: WorkerParameters) :
    Worker(context, parameters) {
    private val statusToken = UUID.randomUUID().toString()

    override fun doWork(): Result {
        val generation = inputData.getLong(GENERATION, -1)
        try {
            val control = ActivationControl(AndroidActivationStore(applicationContext))
            val settings = control.current()
            if (
                settings == null ||
                    !settings.enabled ||
                    settings.generation != generation ||
                    isStopped
            )
                return report(generation, "PAUSED", 0, 0, false)
            var delivered = 0
            var review = 0
            var stopped: DurableDelivery.Result? = null
            var retry = false
            var backlog = false
            SqliteLedger(applicationContext).use { ledger ->
                ledger.expirePayloads(50)
                val delivery =
                    DurableDelivery(
                        ledger.outbox(),
                        AndroidCalendarSink(applicationContext, settings.calendarBindings),
                    )
                for ((calendar, sources) in settings.destinations) {
                    val source =
                        AndroidCallLogSource(
                            applicationContext,
                            sources.any { it != CallRecord.PHONE },
                        )
                    val runner = AutomaticDelivery(control, source, ledger, delivery)
                    var settled = false
                    repeat(8) {
                        if (!settled) {
                            val batch = runner.runForCalendar(settings, calendar, 50) { !isStopped }
                            delivered += batch.delivered
                            review += batch.review
                            val action = AutomaticWorkPolicy.decide(batch, runAttemptCount)
                            settled = action != AutomaticWorkPolicy.Action.CONTINUE
                            retry = retry || action == AutomaticWorkPolicy.Action.RETRY
                            if (stopped == null || batch.stopped == DurableDelivery.Result.FAILED) {
                                stopped = batch.stopped
                            }
                        }
                    }
                    backlog = backlog || !settled
                }
            }
            val status = stopped?.name ?: if (backlog) "BACKLOG" else "COMPLETE"
            return report(generation, status, delivered, review, retry || backlog)
        } catch (unavailable: RuntimeException) {
            return report(generation, "FAILED", 0, 0, runAttemptCount < 5)
        }
    }

    private fun report(
        generation: Long,
        code: String,
        delivered: Int,
        review: Int,
        retry: Boolean,
    ): Result {
        val committed =
            try {
                AndroidWorkStatusStore(applicationContext)
                    .publish(
                        generation,
                        System.currentTimeMillis(),
                        statusToken,
                        WorkStatus.Code.parse(code),
                        delivered,
                        review,
                    )
            } catch (unavailable: RuntimeException) {
                false
            }
        if (!committed) {
            if (runAttemptCount < 5) return Result.retry()
            return Result.failure(
                Data.Builder()
                    .putString("status", "UNKNOWN")
                    .putBoolean("statusStored", false)
                    .build(),
            )
        }
        if (retry) return Result.retry()
        return Result.success(
            Data.Builder()
                .putString("status", code)
                .putInt("delivered", delivered)
                .putInt("review", review)
                .build(),
        )
    }

    companion object {
        const val GENERATION = "activationGeneration"
    }
}
