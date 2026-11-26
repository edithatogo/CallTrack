// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import com.markobl.calllogsync.R

/** Package visibility finds candidates. An attributed Android row is evidence of shared history. */
internal object AndroidCallSources {
    data class App(val packageName: String, val label: String, val observed: Boolean) {
        fun option(context: Context): CalendarDestinationOption {
            val status =
                if (observed) R.string.auto_source_observed else R.string.auto_source_unverified
            return CalendarDestinationOption(packageName, context.getString(status, label))
        }
    }

    private val common =
        setOf(
            "com.facebook.orca",
            "com.microsoft.teams",
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.thoughtcrime.securesms",
            "org.telegram.messenger",
            "org.telegram.messenger.web",
        )

    fun apps(context: Context, configured: Set<String> = emptySet()): List<App> {
        val manager = context.packageManager
        val advertised =
            manager
                .queryIntentActivities(
                    Intent(TelecomManager.ACTION_CALL_BACK),
                    PackageManager.ResolveInfoFlags.of(0),
                )
                .map { it.activityInfo.packageName }
        val observed =
            try {
                AndroidCallLogSource(context, true).observedAppPackages()
            } catch (_: RuntimeException) {
                // Discovery can still show installed candidates while history access is
                // unavailable.
                emptySet()
            }
        return (common + advertised + observed + configured - CallRecord.PHONE)
            .mapNotNull { id ->
                val label =
                    try {
                        manager
                            .getApplicationInfo(id, PackageManager.ApplicationInfoFlags.of(0))
                            .loadLabel(manager)
                            .toString()
                    } catch (_: PackageManager.NameNotFoundException) {
                        if (id in observed || id in configured) id else return@mapNotNull null
                    }
                App(id, label, id in observed)
            }
            .sortedWith(compareBy({ it.label }, { it.packageName }))
    }
}
