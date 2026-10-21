package com.markobl.calllogsync.slice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Protected system signals only; phone-number extras are never read or logged. */
class AutomaticWakeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val callEnded =
            TelephonyManager.ACTION_PHONE_STATE_CHANGED == action &&
                TelephonyManager.EXTRA_STATE_IDLE ==
                    intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        if (
            !callEnded &&
                Intent.ACTION_BOOT_COMPLETED != action &&
                Intent.ACTION_MY_PACKAGE_REPLACED != action
        )
            return
        val pending = goAsync()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8)
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                AutomaticScheduling.reconcileCommitted(context, deadline)
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
                Log.w("CallTrackScheduling", "WAKE_ENQUEUE_UNCONFIRMED")
            } catch (unavailable: Exception) {
                Log.w("CallTrackScheduling", "WAKE_ENQUEUE_UNCONFIRMED")
            } finally {
                pending!!.finish()
                executor.shutdown()
            }
        }
    }
}
