package com.markobl.calllogsync.slice

import android.content.Context
import androidx.work.*
import java.util.concurrent.*

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
        ): ActivationControl.Settings =
            control(context)
                .activate(
                    calendar,
                    timezone,
                    System.currentTimeMillis(),
                    incoming,
                    outgoing,
                    missed,
                )

        @JvmStatic
        fun pause(context: Context?) {
            control(context).pause()
        }

        private fun control(context: Context?): AutomaticScheduleControl =
            AutomaticScheduleControl(
                ActivationControl(AndroidActivationStore(context)),
                { settings -> schedule(context, settings) },
                { WorkManager.getInstance(context!!).cancelAllWorkByTag(TAG) },
            )

        @JvmStatic
        fun reconcile(context: Context?) {
            requests(context)
        }

        private fun requests(context: Context?): Array<Future<*>>? = control(context).reconcile()

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
                    }
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

        private fun remainingBudget(deadlineNanos: Long): Long {
            val remaining = deadlineNanos - System.nanoTime()
            if (remaining <= 0) throw TimeoutException("Wake enqueue unconfirmed")
            return remaining
        }

        @JvmStatic
        @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
        fun awaitCommitted(operations: Array<out Future<*>?>?, deadlineNanos: Long) {
            remainingBudget(deadlineNanos)
            for (operation in operations!!) operation!!.get(
                remainingBudget(deadlineNanos),
                TimeUnit.NANOSECONDS,
            )
        }

        private fun input(settings: ActivationControl.Settings?): Data =
            Data.Builder().putLong(AutomaticLogWorker.GENERATION, settings!!.generation).build()

        @JvmStatic
        fun prompt(settings: ActivationControl.Settings?): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(AutomaticLogWorker::class.java)
                .setInputData(input(settings))
                .addTag(TAG)
                .setInitialDelay(2, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                .build()

        @JvmStatic
        fun periodic(settings: ActivationControl.Settings?): PeriodicWorkRequest =
            PeriodicWorkRequest.Builder(AutomaticLogWorker::class.java, 15, TimeUnit.MINUTES)
                .setInputData(input(settings))
                .addTag(TAG)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
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
