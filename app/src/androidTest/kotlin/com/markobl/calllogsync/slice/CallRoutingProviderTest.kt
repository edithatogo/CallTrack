// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.CallLog.Calls
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class CallRoutingProviderTest {
    @Test
    fun nativeSharedHistoryRoutesIndependentlyAndRetriesWithoutDuplicates() {
        check(Build.HARDWARE == "ranchu") { "Call provider fixture requires an emulator" }
        val fixture = ParityProviderTest()
        val account = fixture.account()
        val context = fixture.context()
        val resolver = context.contentResolver
        val where = "${Calls.PHONE_ACCOUNT_ID}=?"
        val args = arrayOf(account)
        val store = AndroidActivationStore(context)
        val control = ActivationControl(store)
        val manager = WorkManager.getInstance(context)
        val unavailable = fixture.insertCalendar("Unavailable route")
        val combined = fixture.insertCalendar("Phone and app")
        val appOnly = fixture.insertCalendar("App only")
        val oldCalendar = fixture.insertCalendar("Original destination")
        val app = "org.calltrack.example.calls"
        val other = "org.calltrack.example.other"
        val now = System.currentTimeMillis()
        try {
            val old = context.getSharedPreferences(account, Context.MODE_PRIVATE)
            assertTrue(
                old.edit()
                    .putString(
                        "automaticActivation",
                        """{"version":1,"generation":8,"enabled":true,"calendar":$combined,"timezone":"UTC","start":$now,"incoming":true,"outgoing":false,"missed":true}""",
                    )
                    .commit(),
            )
            val upgraded = checkNotNull(AndroidActivationStore(old).get())
            assertEquals(mapOf(combined to setOf(CallRecord.PHONE)), upgraded.destinations)
            assertEquals("automatic", upgraded.operation().owner)
            assertEquals(8L, upgraded.generation)
            assertEquals(now, upgraded.start)
            assertTrue(upgraded.enabled)
            assertFalse(upgraded.outgoing)

            val sink = AndroidCalendarSink(context)
            val bindings =
                listOf(unavailable, combined, appOnly, oldCalendar).associateWith {
                    checkNotNull(sink.writableBinding(it))
                }
            val settings =
                control.activate(
                    mapOf(
                        unavailable to setOf(app),
                        combined to setOf(CallRecord.PHONE, app),
                        appOnly to setOf(app),
                        oldCalendar to setOf(app),
                    ),
                    "UTC",
                    now,
                    true,
                    true,
                    true,
                    bindings,
                )
            assertTrue(settings.same(store.get()))
            fixture.removeCalendar(unavailable)
            fixture.removeCalendar(oldCalendar)
            val replacement = fixture.insertCalendar("Replacement destination")
            assertEquals("Calendar provider can reuse a deleted row ID", oldCalendar, replacement)
            assertTrue(sink.writable(replacement))
            assertFalse(AndroidCalendarSink(context, bindings).writable(replacement))
            var appRow = -1L
            FixtureCallLog.asShell {
                for (source in listOf(null, app, other)) {
                    val values =
                        ContentValues().apply {
                            put(Calls.PHONE_ACCOUNT_ID, account)
                            put(Calls.TYPE, Calls.OUTGOING_TYPE)
                            put(Calls.DATE, now)
                            put(Calls.NUMBER, "5550100")
                            put(Calls.NUMBER_PRESENTATION, Calls.PRESENTATION_ALLOWED)
                            put(Calls.DURATION, 1L)
                            if (source != null) {
                                put(Calls.UUID, UUID.randomUUID().toString())
                                put(Calls.PHONE_ACCOUNT_COMPONENT_NAME, "$source/.Calls")
                            }
                        }
                    val id =
                        ContentUris.parseId(checkNotNull(FixtureCallLog.insert(context, values)))
                    if (source == app) appRow = id
                }
            }
            assertNull(AndroidCallLogSource(context).at(appRow))
            val shared = checkNotNull(AndroidCallLogSource(context, true).at(appRow))
            assertEquals(app, shared.source)
            assertTrue(
                AndroidCallSources.apps(context).any { it.packageName == app && it.observed },
            )

            repeat(2) {
                assertEquals("DESTINATION_UNAVAILABLE", runWorker(manager, settings))
                assertEquals(2, fixture.events(combined))
                assertEquals(1, fixture.events(appOnly))
                assertEquals(0, fixture.events(replacement))
                val marker = CallEvent.from(shared, "UTC").marker
                assertEquals(1, sink.find(combined, marker).size)
                assertEquals(1, sink.find(appOnly, marker).size)
            }
        } finally {
            control.pause()
            manager.cancelAllWorkByTag(AutomaticScheduling.TAG).result.get(15, TimeUnit.SECONDS)
            FixtureCallLog.asShell {
                resolver.delete(Calls.CONTENT_URI_WITH_VOIP_CALLS, where, args)
                resolver
                    .query(Calls.CONTENT_URI_WITH_VOIP_CALLS, arrayOf(Calls._ID), where, args, null)
                    .use { assertEquals(0, checkNotNull(it).count) }
            }
            fixture.cleanupFixture()
        }
    }

    private fun runWorker(manager: WorkManager, settings: ActivationControl.Settings): String? {
        val request = AutomaticScheduling.prompt(settings)
        manager.enqueue(request).result.get(15, TimeUnit.SECONDS)
        val deadline = SystemClock.elapsedRealtime() + 30_000
        do {
            val info = manager.getWorkInfoById(request.id).get(5, TimeUnit.SECONDS)
            if (info?.state?.isFinished == true) {
                assertEquals(WorkInfo.State.SUCCEEDED, info.state)
                return info.outputData.getString("status")
            }
            SystemClock.sleep(100)
        } while (SystemClock.elapsedRealtime() < deadline)
        throw AssertionError("Route worker did not finish")
    }
}
