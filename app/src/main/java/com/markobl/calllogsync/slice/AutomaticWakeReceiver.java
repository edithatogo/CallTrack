package com.markobl.calllogsync.slice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

/** Protected system signals only; phone number extras are never read or logged. */
public final class AutomaticWakeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        boolean callEnded =
                TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)
                        && TelephonyManager.EXTRA_STATE_IDLE.equals(
                                intent.getStringExtra(TelephonyManager.EXTRA_STATE));
        if (!callEnded
                && !Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
        PendingResult pending = goAsync();
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(8);
        java.util.concurrent.ExecutorService executor =
                java.util.concurrent.Executors.newSingleThreadExecutor();
        executor.execute(
                () -> {
                    try {
                        AutomaticScheduling.reconcileCommitted(context, deadline);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        android.util.Log.w("CallTrackScheduling", "WAKE_ENQUEUE_UNCONFIRMED");
                    } catch (Exception unavailable) {
                        // No caller fields, account names, exception text or stack traces.
                        android.util.Log.w("CallTrackScheduling", "WAKE_ENQUEUE_UNCONFIRMED");
                    } finally {
                        pending.finish();
                        executor.shutdown();
                    }
                });
    }
}
