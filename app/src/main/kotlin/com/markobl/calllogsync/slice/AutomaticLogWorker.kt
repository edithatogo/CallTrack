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
            SqliteLedger(applicationContext).use { ledger ->
                ledger.expirePayloads(50)
                val runner =
                    AutomaticDelivery(
                        control,
                        AndroidCallLogSource(applicationContext),
                        ledger,
                        DurableDelivery(ledger.outbox(), AndroidCalendarSink(applicationContext)),
                    )
                for (page in 0 until 8) {
                    val batch = runner.run(settings, 50) { !isStopped }
                    delivered += batch.delivered
                    review += batch.review
                    when (AutomaticWorkPolicy.decide(batch, runAttemptCount)) {
                        AutomaticWorkPolicy.Action.RETRY ->
                            return report(generation, "FAILED", delivered, review, true)
                        AutomaticWorkPolicy.Action.STOP ->
                            return report(
                                generation,
                                batch.stopped?.name ?: "COMPLETE",
                                delivered,
                                review,
                                false,
                            )
                        else -> Unit
                    }
                }
            }
            return report(generation, "BACKLOG", delivered, review, true)
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
                    .build()
            )
        }
        if (retry) return Result.retry()
        return Result.success(
            Data.Builder()
                .putString("status", code)
                .putInt("delivered", delivered)
                .putInt("review", review)
                .build()
        )
    }

    companion object {
        const val GENERATION = "activationGeneration"
    }
}
