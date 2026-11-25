// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import android.content.Context
import com.markobl.calllogsync.R
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal data class ManualCalendars(val labels: Map<Long, String>, val saved: Long?)

internal interface ManualLoggingDataSource {
    suspend fun calendars(): ManualCalendars

    suspend fun select(calendar: Long)

    suspend fun logLatest(calendar: Long): SliceEngine.Result

    suspend fun maintain()
}

internal class AndroidManualLoggingDataSource(private val context: Context) :
    ManualLoggingDataSource {
    private val settings = AndroidSettingsStore(context)

    override suspend fun calendars(): ManualCalendars =
        withContext(Dispatchers.IO) {
            val saved =
                settings.data.first().let {
                    if (it.hasSelectedCalendar()) it.selectedCalendar else null
                }
            val choices = AndroidCalendarSink(context).choices()
            val selected = saved?.takeIf { id -> choices.any { it.id == id } }
            if (saved != null && saved >= 0 && selected == null) settings.forgetCalendar(saved)
            ManualCalendars(
                labels =
                    calendarChoiceLabels(
                        choices,
                        context.getString(R.string.slice_unnamed_calendar),
                    ),
                saved = selected,
            )
        }

    override suspend fun select(calendar: Long) = settings.selectCalendar(calendar)

    override suspend fun logLatest(calendar: Long): SliceEngine.Result =
        withContext(Dispatchers.IO) {
            SqliteLedger(context).use { ledger ->
                ManualDelivery(
                        AndroidCallLogSource(context),
                        DurableDelivery(ledger.outbox(), AndroidCalendarSink(context)),
                        TimeZone.getDefault().id,
                    )
                    .run(calendar)
            }
        }

    override suspend fun maintain() {
        withContext(Dispatchers.IO) {
            try {
                AutomaticScheduling.reconcile(context)
            } finally {
                SqliteLedger(context).use { it.expirePayloads(FOREGROUND_RETENTION_BATCH) }
            }
        }
    }
}

private const val FOREGROUND_RETENTION_BATCH = 50
