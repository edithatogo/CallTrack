package com.markobl.calllogsync.slice

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext

private const val CALLS_PER_BATCH = 50
private const val BATCHES_PER_DESTINATION = 8

/** Local provider work only; work input/output contain no personal call fields. */
class AutomaticLogWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    private val statusToken = UUID.randomUUID().toString()

    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) { runDelivery(coroutineContext.job) }

    private fun runDelivery(execution: Job): Result {
        val generation = inputData.getLong(GENERATION, -1)
        return try {
            val control = ActivationControl(AndroidActivationStore(applicationContext))
            val settings = control.current()
            if (settings == null || !mayRun(settings, generation)) {
                report(generation, "PAUSED", 0, 0, false)
            } else {
                deliver(settings, control, generation, execution)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (ignored: RuntimeException) {
            execution.ensureActive()
            report(generation, "FAILED", 0, 0, runAttemptCount < AutomaticWorkPolicy.MAX_ATTEMPTS)
        }
    }

    private fun mayRun(settings: ActivationControl.Settings, generation: Long): Boolean =
        settings.enabled && settings.generation == generation && !isStopped

    private fun deliver(
        settings: ActivationControl.Settings,
        control: ActivationControl,
        generation: Long,
        execution: Job,
    ): Result {
        var delivered = 0
        var review = 0
        var stopped: DurableDelivery.Result? = null
        var retry = false
        var backlog = false
        SqliteLedger(applicationContext).use { ledger ->
            ledger.expirePayloads(RECEIPT_RETENTION_BATCH_SIZE)
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
                repeat(BATCHES_PER_DESTINATION) {
                    execution.ensureActive()
                    if (!settled) {
                        val batch =
                            runner.runForCalendar(settings, calendar, CALLS_PER_BATCH) {
                                execution.isActive && !isStopped
                            }
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
        execution.ensureActive()
        val status = stopped?.name ?: if (backlog) "BACKLOG" else "COMPLETE"
        return report(generation, status, delivered, review, retry || backlog)
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
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (ignored: RuntimeException) {
                false
            }
        if (!committed) {
            return if (runAttemptCount < AutomaticWorkPolicy.MAX_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure(
                    Data.Builder()
                        .putString("status", "UNKNOWN")
                        .putBoolean("statusStored", false)
                        .build(),
                )
            }
        }
        return if (retry) {
            Result.retry()
        } else {
            Result.success(
                Data.Builder()
                    .putString("status", code)
                    .putInt("delivered", delivered)
                    .putInt("review", review)
                    .build(),
            )
        }
    }

    companion object {
        const val GENERATION = "activationGeneration"
    }
}
