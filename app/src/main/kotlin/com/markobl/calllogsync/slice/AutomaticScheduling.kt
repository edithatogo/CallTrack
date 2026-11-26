package com.markobl.calllogsync.slice

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

private const val RETRY_BACKOFF_SECONDS = 10L
private const val RECOVERY_INTERVAL_MINUTES = 15L

/** Best-effort prompt work plus persistent periodic recovery; no fixed deadline promise. */
class AutomaticScheduling private constructor() {
    companion object {
        const val PERIODIC = "calltrack.automatic.periodic"
        const val TAG = "calltrack.automatic"

        @JvmStatic
        fun activate(
            context: Context?,
            calendar: Long,
            timezone: String?,
            incoming: Boolean,
            outgoing: Boolean,
            missed: Boolean,
        ): ActivationControl.Settings {
            val binding =
                checkNotNull(AndroidCalendarSink(context).writableBinding(calendar)) {
                    "Destination unavailable"
                }
            return activate(
                context,
                mapOf(calendar to setOf(CallRecord.PHONE)),
                timezone,
                incoming,
                outgoing,
                missed,
                mapOf(calendar to binding),
            )
        }

        @JvmStatic
        fun activate(
            context: Context?,
            destinations: Map<Long, Set<String>>,
            timezone: String?,
            incoming: Boolean,
            outgoing: Boolean,
            missed: Boolean,
            calendarBindings: Map<Long, String>,
        ): ActivationControl.Settings {
            require(calendarBindings.keys == destinations.keys) { "Calendar bindings required" }
            val sink = AndroidCalendarSink(context, calendarBindings)
            check(destinations.keys.all(sink::writable)) { "Destination changed or unavailable" }
            return control(context)
                .activate(
                    destinations,
                    timezone,
                    System.currentTimeMillis(),
                    incoming,
                    outgoing,
                    missed,
                    calendarBindings,
                )
        }

        @JvmStatic
        fun pause(context: Context?) {
            control(context).pause()
        }

        internal fun control(context: Context?): AutomaticScheduleControl =
            AutomaticScheduleControl(
                ActivationControl(AndroidActivationStore(context)),
                { settings -> schedule(context, settings) },
                { WorkManager.getInstance(context!!).cancelAllWorkByTag(TAG) },
            )

        @JvmStatic
        fun reconcile(context: Context?) {
            control(context).reconcile()
        }

        @JvmStatic
        @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
        fun reconcileCommitted(context: Context?, deadlineNanos: Long) {
            remainingBudget(deadlineNanos)
            val admitted =
                WakeAdmission.executor.submit(
                    Callable<Any?> {
                        awaitCommitted(
                            control(context).reconcile {
                                deadlineNanos - System.nanoTime() > 0 &&
                                    !Thread.currentThread().isInterrupted
                            },
                            deadlineNanos,
                        )
                        null
                    },
                )
            try {
                admitted.get(remainingBudget(deadlineNanos), TimeUnit.NANOSECONDS)
            } finally {
                if (!admitted.isDone) admitted.cancel(true)
                WakeAdmission.executor.purge()
            }
        }

        private object WakeAdmission {
            val executor =
                ThreadPoolExecutor(
                        1,
                        1,
                        30,
                        TimeUnit.SECONDS,
                        ArrayBlockingQueue<Runnable>(1),
                        ThreadFactory { task ->
                            Thread(task, "CallTrackWakeAdmission").apply { isDaemon = true }
                        },
                    )
                    .apply { allowCoreThreadTimeOut(true) }
        }

        internal fun remainingBudget(deadlineNanos: Long): Long {
            val remaining = deadlineNanos - System.nanoTime()
            if (remaining <= 0) throw TimeoutException("Wake enqueue unconfirmed")
            return remaining
        }

        @JvmStatic
        @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
        fun awaitCommitted(operations: Array<out Future<*>?>?, deadlineNanos: Long) {
            remainingBudget(deadlineNanos)
            for (operation in operations!!) {
                operation!!.get(
                    remainingBudget(deadlineNanos),
                    TimeUnit.NANOSECONDS,
                )
            }
        }

        internal fun input(settings: ActivationControl.Settings?): Data =
            Data.Builder().putLong(AutomaticLogWorker.GENERATION, settings!!.generation).build()

        @JvmStatic
        fun prompt(settings: ActivationControl.Settings?): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(AutomaticLogWorker::class.java)
                .setInputData(input(settings))
                .addTag(TAG)
                .setInitialDelay(2, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.LINEAR, RETRY_BACKOFF_SECONDS, TimeUnit.SECONDS)
                .build()

        @JvmStatic
        fun periodic(settings: ActivationControl.Settings?): PeriodicWorkRequest =
            PeriodicWorkRequest.Builder(
                    AutomaticLogWorker::class.java,
                    RECOVERY_INTERVAL_MINUTES,
                    TimeUnit.MINUTES,
                )
                .setInputData(input(settings))
                .addTag(TAG)
                .setBackoffCriteria(BackoffPolicy.LINEAR, RETRY_BACKOFF_SECONDS, TimeUnit.SECONDS)
                .build()

        private fun schedule(
            context: Context?,
            settings: ActivationControl.Settings?,
        ): Array<Future<*>> {
            val manager = WorkManager.getInstance(context!!)
            val periodic =
                manager
                    .enqueueUniquePeriodicWork(
                        PERIODIC,
                        ExistingPeriodicWorkPolicy.UPDATE,
                        periodic(settings),
                    )
                    .result
            val prompt =
                manager
                    .enqueueUniqueWork(
                        "calltrack.automatic.prompt." + settings!!.generation,
                        ExistingWorkPolicy.APPEND_OR_REPLACE,
                        prompt(settings),
                    )
                    .result
            return arrayOf<Future<*>>(periodic, prompt)
        }
    }
}
