// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.provider.CallLog.Calls
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.markobl.calllogsync.R
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallRoutingProviderTest {
    @Test
    fun nativeSharedHistoryRoutesIndependentlyAndRetriesWithoutDuplicates() {
        check(Build.HARDWARE == "ranchu") { "Call provider fixture requires an emulator" }
        val fixture = ParityProviderTest()
        val account = fixture.account()
        val context = fixture.context()
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
            assertLegacyActivation(context, account, combined, now)

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
            val replacement = replaceBoundCalendar(fixture, sink, context, oldCalendar, bindings)
            val appRow = insertSharedHistory(context, account, now, app, other)
            val shared = readSharedCall(context, appRow, app)

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
            clearSharedHistory(context, account)
            fixture.cleanupFixture()
        }
    }

    @Test
    fun renderedHistoricalAppImportPersistsItsSourceAndWritesOnlyTheSelectedCall() {
        check(Build.HARDWARE == "ranchu") { "Call provider fixture requires an emulator" }
        val fixture = ParityProviderTest()
        val context = fixture.context()
        val account = fixture.account()
        val calendarLabel = "Shared history fixture"
        val calendar = fixture.insertCalendar(calendarLabel)
        val app = "org.calltrack.example_app.calls"
        val other = "org.calltrack.exampleXapp.calls"
        val ui = RenderedUi(InstrumentationRegistry.getInstrumentation())
        try {
            val now = System.currentTimeMillis() - 60_000
            val appRow = insertSharedHistory(context, account, now, app, other)
            assertSharedHistoryScope(context, appRow, app)

            ActivityScenario.launch(HistoricalImportActivity::class.java).use { scenario ->
                selectHistoricalRoute(ui, context, app, calendarLabel)
                ui.tapId("import_create")
                ui.await(
                    { lastImport(context)?.phase == HistoricalImport.Phase.PREVIEW },
                    "App history preview did not complete",
                )
                val preview = checkNotNull(lastImport(context))
                assertEquals(app, preview.callSource)
                assertEquals(calendar, preview.calendar)
                assertEquals(
                    AndroidCalendarSink(context).writableBinding(calendar),
                    preview.calendarBinding,
                )
                assertEquals(1L, preview.count)
                assertEquals(0, fixture.events(calendar))

                scenario.recreate()
                ui.revealId("import_summary_text")
                ui.await(
                    { ui.find("import_summary_text", true)?.text?.contains(app) == true },
                    "Reopened preview did not display its saved source",
                )
                ui.capture("import-shared-call-preview")
                ui.tapId("import_confirm")
                ui.tapDialogPositive()
                ui.await(
                    { lastImport(context)?.phase == HistoricalImport.Phase.COMPLETE },
                    "Confirmed app history import did not complete",
                )
                val complete = checkNotNull(lastImport(context))
                assertEquals(preview.id, complete.id)
                assertEquals(app, complete.callSource)
                assertEquals(1L, complete.saved)
                assertEquals(0L, complete.review)
                assertEquals(1, fixture.events(calendar))
                ui.capture("import-shared-call-complete")
            }
        } finally {
            clearSharedHistory(context, account)
            fixture.removeCalendar(calendar)
        }
    }

    private fun assertSharedHistoryScope(context: Context, appRow: Long, app: String) {
        val source = AndroidCallLogSource.forApp(context, app)
        assertEquals(appRow.toString(), checkNotNull(source.upper()).id)
        assertEquals(listOf(app), source.page(-1, Long.MAX_VALUE, 10)?.map { it?.source })
    }

    private fun selectHistoricalRoute(
        ui: RenderedUi,
        context: Context,
        app: String,
        calendarLabel: String,
    ) {
        ui.revealId("import_source_picker")
        val picker = checkNotNull(ui.shown("import_source_picker", true))
        val bounds = Rect().also(picker::getBoundsInScreen)
        val minimum = (48 * context.resources.displayMetrics.density).toInt()
        assertTrue(picker.isFocusable && picker.isClickable)
        assertTrue(bounds.width() >= minimum && bounds.height() >= minimum)
        ui.tapId("import_source_picker")
        ui.tapText(context.getString(R.string.auto_source_observed, app))
        ui.tapId("import_calendar_picker")
        ui.tapText(calendarLabel)
    }

    private fun lastImport(context: Context): HistoricalImport.Session? =
        SqliteImportStore(context).use { store -> store.sessions(-1, 1).firstOrNull()?.session }

    private fun replaceBoundCalendar(
        fixture: ParityProviderTest,
        sink: AndroidCalendarSink,
        context: Context,
        oldCalendar: Long,
        bindings: Map<Long, String>,
    ): Long {
        fixture.removeCalendar(oldCalendar)
        val replacement = fixture.insertCalendar("Replacement destination")
        assertEquals("Calendar provider can reuse a deleted row ID", oldCalendar, replacement)
        assertTrue(sink.writable(replacement))
        assertFalse(AndroidCalendarSink(context, bindings).writable(replacement))
        return replacement
    }

    private fun readSharedCall(context: Context, appRow: Long, app: String): CallRecord {
        assertNull(AndroidCallLogSource(context).at(appRow))
        val shared = checkNotNull(AndroidCallLogSource(context, true).at(appRow))
        assertEquals(app, shared.source)
        assertTrue(
            AndroidCallSources.apps(context).any { it.packageName == app && it.observed },
        )
        return shared
    }

    private fun assertLegacyActivation(
        context: Context,
        account: String,
        combined: Long,
        now: Long,
    ) {
        val old = context.getSharedPreferences(account, Context.MODE_PRIVATE)
        assertTrue(
            old.edit()
                .putString(
                    "automaticActivation",
                    "{\"version\":1,\"generation\":8,\"enabled\":true,\"calendar\":$combined," +
                        "\"timezone\":\"UTC\",\"start\":$now,\"incoming\":true," +
                        "\"outgoing\":false,\"missed\":true}",
                )
                .commit(),
        )
        val upgraded =
            SettingsTestFixture(context, account).use { migration ->
                checkNotNull(migration.activation().get()).also {
                    assertTrue(migration.disk().hasAutomatic())
                    assertFalse(old.contains("automaticActivation"))
                }
            }
        assertEquals(mapOf(combined to setOf(CallRecord.PHONE)), upgraded.destinations)
        assertEquals("automatic", upgraded.operation().owner)
        assertEquals(8L, upgraded.generation)
        assertEquals(now, upgraded.start)
        assertTrue(upgraded.enabled)
        assertFalse(upgraded.outgoing)
    }

    private fun insertSharedHistory(
        context: Context,
        account: String,
        now: Long,
        app: String,
        other: String,
    ): Long {
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
                val id = ContentUris.parseId(checkNotNull(FixtureCallLog.insert(context, values)))
                if (source == app) appRow = id
            }
        }
        return appRow
    }

    private fun clearSharedHistory(context: Context, account: String) {
        val resolver = context.contentResolver
        val where = "${Calls.PHONE_ACCOUNT_ID}=?"
        val args = arrayOf(account)
        FixtureCallLog.asShell {
            resolver.delete(Calls.CONTENT_URI_WITH_VOIP_CALLS, where, args)
            resolver
                .query(Calls.CONTENT_URI_WITH_VOIP_CALLS, arrayOf(Calls._ID), where, args, null)
                .use { assertEquals(0, checkNotNull(it).count) }
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
