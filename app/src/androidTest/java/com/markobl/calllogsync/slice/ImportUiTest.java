package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.CallLog.Calls;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;

import com.markobl.calllogsync.R;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.TimeZone;

/** Tagged synthetic source, real calendar effects and rendered historical-import controls. */
public class ImportUiTest {
    private static final int PARTIAL_COUNT = 120;
    private final ParityProviderTest fixture = new ParityProviderTest();

    private Context context() {
        return fixture.context();
    }

    private String tag() {
        return fixture.account() + "-ui-import";
    }

    private SharedPreferences state() {
        return context().getSharedPreferences(tag(), Context.MODE_PRIVATE);
    }

    private ZoneId zone() {
        return ZoneId.of(TimeZone.getDefault().getID());
    }

    private LocalDate today() {
        return LocalDate.parse(state().getString("today", LocalDate.now(zone()).toString()));
    }

    private long calendar() {
        return state().getLong("calendar", -1);
    }

    private long day(LocalDate value) {
        return value.atStartOfDay(zone()).toInstant().toEpochMilli();
    }

    private void shellCalls(Runnable action) {
        FixtureCallLog.asShell(action);
    }

    private long add(long at, int type) {
        ContentValues row = new ContentValues();
        row.put(Calls.NUMBER, "0000000000");
        row.put(Calls.CACHED_NAME, "Synthetic import fixture");
        row.put(Calls.DATE, at);
        row.put(Calls.DURATION, type == 3 ? 0 : 7);
        row.put(Calls.TYPE, type);
        row.put(Calls.NUMBER_PRESENTATION, Calls.PRESENTATION_ALLOWED);
        row.put(Calls.PHONE_ACCOUNT_ID, tag());
        android.net.Uri uri = FixtureCallLog.insert(context(), row);
        assertNotNull(uri);
        return ContentUris.parseId(uri);
    }

    private HistoricalImport.Session latest() {
        try (SqliteImportStore store = new SqliteImportStore(context())) {
            java.util.List<SqliteImportStore.ListedSession> rows = store.sessions(-1, 1);
            return rows.isEmpty() ? null : rows.get(0).session;
        }
    }

    private boolean phase(HistoricalImport.Phase phase) {
        HistoricalImport.Session s = latest();
        return s != null && s.phase == phase;
    }

    private Activity open(Instrumentation instrumentation) {
        return instrumentation.startActivitySync(
                new Intent(context(), HistoricalImportActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private void close(Instrumentation instrumentation, Activity activity) {
        if (activity != null) instrumentation.runOnMainSync(activity::finish);
    }

    private void preview(RenderedUi ui) {
        ui.tapId("import_create");
        ui.await(() -> phase(HistoricalImport.Phase.PREVIEW), "Read-only preview did not complete");
        assertEquals(3, latest().count);
        ui.revealId("import_summary_text");
    }

    private void confirm(RenderedUi ui) {
        ui.revealId("import_confirm");
        ui.tapId("import_confirm");
        ui.tapDialogPositive();
        ui.await(
                () -> phase(HistoricalImport.Phase.COMPLETE),
                "Confirmed UI import did not complete");
        assertEquals(
                "The import must retain its explicitly selected destination",
                calendar(),
                latest().calendar);
        assertEquals(3, latest().saved);
        assertEquals(0, latest().review);
        ui.revealId("import_summary_text");
    }

    @Test
    public void prepareCalendarAndTaggedHistory() {
        assertNull(new ActivationControl(new AndroidActivationStore(context())).current());
        long id = fixture.createCalendar();
        shellCalls(() -> {
            try (Cursor c =
                    FixtureCallLog.query(context(), new String[] {Calls._ID}, null, null, null)) {
                assertNotNull(c);
                assertEquals("Refusing nonempty call provider", 0, c.getCount());
            }
            assertTrue(
                    state().edit()
                            .putLong("calendar", id)
                            .putString("today", LocalDate.now(zone()).toString())
                            .putBoolean("ownsCalls", true)
                            .commit());
            long yesterday = day(today().minusDays(1)) + 12 * 60 * 60 * 1000;
            for (int type = 1; type <= 3; type++) add(yesterday + type * 1000, type);
            add(day(today().minusDays(8)), 1);
        });
    }

    @Test
    public void latestCallUsesNewestDateAndHighestIdOnTies() {
        long at = day(today().minusDays(1)) + 12 * 60 * 60 * 1000 + 3000;
        long[] tiedId = new long[1];
        shellCalls(() -> tiedId[0] = add(at, 2));
        try {
            CallRecord latest = new AndroidCallLogSource(context()).latest();
            assertNotNull(latest);
            assertEquals("Latest call must use the newest timestamp", at, latest.start);
            assertEquals(
                    "Equal timestamps must use the highest ID",
                    Long.toString(tiedId[0]),
                    latest.id);
        } finally {
            shellCalls(() -> {
                assertEquals(
                        1,
                        FixtureCallLog.delete(
                                context(),
                                Calls._ID + "=? AND " + Calls.PHONE_ACCOUNT_ID + "=?",
                                new String[] {Long.toString(tiedId[0]), tag()}));
            });
        }
    }

    @Test
    public void renderedDestinationAndFiltersSurviveProcessDeathWithoutPreview() {
        RenderedUi ui = new RenderedUi(InstrumentationRegistry.getInstrumentation());
        String from = today().minusDays(1).toString(), to = today().toString();
        ui.openFromLauncher("open_imports");
        ui.await(
                () -> ui.find("import_calendar_picker", true) != null,
                "Import did not render",
                90000);
        ui.tapId("import_calendar_picker");
        ui.tapText(fixture.calendarDisplayName());
        ui.setTextId("import_from", from);
        ui.setTextId("import_to", to);
        ui.tapId("import_outgoing");
        ui.await(
                () -> {
                    android.view.accessibility.AccessibilityNodeInfo node =
                            ui.shown("import_outgoing", true);
                    return node != null
                            && node.isCheckable()
                            && node.getChecked() == AccessibilityNodeInfo.CHECKED_STATE_FALSE;
                },
                "Outgoing filter did not switch off");
        ui.dismissKeyboard();
        assertNull("Editing must not create a preview", latest());
        assertEquals(0, fixture.events(calendar()));
        ui.capture("import-before-process-death");

        ui.killStoppedProcessAndReturn(CalendarSliceActivity.class);
        ui.await(() -> ui.find("calltrack_screen", true) != null, "Import did not restore", 90000);
        ui.revealId("import_calendar_picker");
        ui.await(
                () -> ui.find(fixture.calendarDisplayName(), false) != null,
                "Selected destination did not return after process death");
        assertEquals(from, ui.shown("import_from", true).getText().toString());
        assertEquals(to, ui.shown("import_to", true).getText().toString());
        assertEquals(
                AccessibilityNodeInfo.CHECKED_STATE_FALSE,
                ui.shown("import_outgoing", true).getChecked());
        assertNull("Process restoration must not create a preview", latest());
        assertEquals(0, fixture.events(calendar()));
        ui.capture("import-after-process-death");
    }

    @Test
    public void renderedDestinationAndFiltersSurviveRecreationBeforePreview() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        RenderedUi ui = new RenderedUi(instrumentation);
        String from = today().minusDays(1).toString(), to = today().toString();
        try (ActivityScenario<HistoricalImportActivity> scenario =
                ActivityScenario.launch(HistoricalImportActivity.class)) {
            ui.tapId("import_calendar_picker");
            ui.tapText(fixture.calendarDisplayName());
            ui.setTextId("import_from", from);
            ui.setTextId("import_to", to);
            ui.tapId("import_outgoing");
            ui.await(
                    () -> {
                        android.view.accessibility.AccessibilityNodeInfo node =
                                ui.shown("import_outgoing", true);
                        return node != null
                                && node.isCheckable()
                                && node.getChecked() == AccessibilityNodeInfo.CHECKED_STATE_FALSE;
                    },
                    "Outgoing filter did not switch off");
            ui.dismissKeyboard();
            assertNull("Editing must not create a preview", latest());
            assertEquals(0, fixture.events(calendar()));
            ui.capture("import-before-recreation");

            scenario.recreate();
            ui.revealId("import_calendar_picker");
            ui.await(
                    () -> ui.find(fixture.calendarDisplayName(), false) != null,
                    "Selected destination did not return after recreation");
            assertEquals(from, ui.shown("import_from", true).getText().toString());
            assertEquals(to, ui.shown("import_to", true).getText().toString());
            assertEquals(
                    AccessibilityNodeInfo.CHECKED_STATE_FALSE,
                    ui.shown("import_outgoing", true).getChecked());
            assertNull("Recreation must not create a preview", latest());
            assertEquals(0, fixture.events(calendar()));
            ui.capture("import-after-recreation");

            ui.tapId("import_create");
            ui.await(
                    () -> phase(HistoricalImport.Phase.PREVIEW), "Restored preview did not finish");
            assertEquals(calendar(), latest().calendar);
            assertEquals(2, latest().count);
            assertEquals(0, fixture.events(calendar()));
            ui.dismissKeyboard();
            ui.revealId("import_cancel");
            ui.tapId("import_cancel");
            ui.tapDialogPositive();
            ui.await(
                    () -> phase(HistoricalImport.Phase.CANCELLED),
                    "Restored preview did not cancel");
            assertEquals(0, fixture.events(calendar()));
        }
    }

    @Test
    public void renderedRangePreviewCancelConfirmAndRepeat() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        RenderedUi ui = new RenderedUi(instrumentation);
        CalendarSliceActivity activity = null;
        try {
            activity =
                    (CalendarSliceActivity)
                            instrumentation.startActivitySync(
                                    new Intent(context(), CalendarSliceActivity.class)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            ui.tapId("open_automatic");
            ui.tapId("open_imports");
            assertTrue(
                    "Import keyboard journey must run inside the launcher",
                    RenderedUi.isResumed(instrumentation, activity));
            ui.tapId("import_calendar_picker");
            ui.tapText(fixture.calendarDisplayName());
            String from = today().minusDays(1).toString(), to = today().toString();
            ui.setTextId("import_from", from);
            ui.setTextId("import_to", to);
            ui.capture("import-edited-range");
            preview(ui);
            assertEquals(day(LocalDate.parse(from)), latest().from);
            assertEquals(day(LocalDate.parse(to)), latest().to);
            assertEquals(0, fixture.events(calendar()));
            ui.await(() -> !ui.keyboardVisible(), "Preview left the date keyboard open");
            ui.capture("import-read-only-preview");
            ui.revealId("import_confirm");
            ui.tapId("import_confirm");
            ui.capture("import-confirmation-preview");
            ui.tapId("calltrack_dialog_cancel");
            ui.await(
                    () -> ui.find("calltrack_screen", true) != null,
                    "Import screen did not regain its active window after confirmation");
            assertEquals(HistoricalImport.Phase.PREVIEW, latest().phase);
            assertEquals(0, fixture.events(calendar()));
            String previewId = latest().id;
            ui.revealId("import_confirm");
            ui.tapId("import_confirm");
            ui.await(() -> ui.find("calltrack_dialog_confirm", true) != null, "Review missing");
            CalendarSliceActivity importActivity = activity;
            assertTrue(ui.automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME));
            ui.await(
                    () -> !RenderedUi.isResumed(instrumentation, importActivity),
                    "Import did not pause while the review was displayed");
            ui.startActivity(CalendarSliceActivity.class);
            ui.await(
                    () ->
                            RenderedUi.isResumed(instrumentation, importActivity)
                                    && ui.find("import_summary_text", true) != null
                                    && ui.find("calltrack_dialog_confirm", true) == null,
                    "Import did not return to its preview with the review closed");
            assertEquals(previewId, latest().id);
            assertEquals(HistoricalImport.Phase.PREVIEW, latest().phase);
            assertEquals(0, fixture.events(calendar()));
            // Dismissing the dialog can restore the end field and its keyboard.
            ui.dismissKeyboard();
            ui.revealId("import_from");
            ui.tapId("import_from");
            ui.await(
                    () -> {
                        android.view.accessibility.AccessibilityNodeInfo field =
                                ui.find("import_from", true);
                        return field != null && field.isFocused();
                    },
                    "Start date did not receive input focus for cancellation");
            ui.await(
                    ui::keyboardVisible, "Date keyboard not visibly present for cancellation test");
            ui.capture("import-keyboard-visible-before-cancel");
            ui.tapAboveKeyboard("import_cancel");
            ui.tapDialogPositive();
            ui.await(() -> phase(HistoricalImport.Phase.CANCELLED), "Cancel not persisted");
            assertEquals(0, fixture.events(calendar()));
            ui.revealId("import_summary_text");
            ui.capture("import-cancelled");
            ui.dismissKeyboard();
            ui.revealId("import_create");
            preview(ui);
            confirm(ui);
            assertEquals(3, fixture.events(calendar()));
            ui.capture("import-complete");
            preview(ui);
            confirm(ui);
            assertEquals(3, fixture.events(calendar()));
            ui.capture("import-repeated-without-duplicates");
        } catch (Throwable failure) {
            try {
                ui.capture("import-failure");
            } catch (Throwable capture) {
                failure.addSuppressed(capture);
            }
            throw failure;
        } finally {
            close(instrumentation, activity);
        }
    }

    @Test
    public void preparePersistedPartialImport() {
        assertEquals(3, fixture.events(calendar()));
        long today = day(today()), tomorrow = day(today().plusDays(1));
        shellCalls(() -> {
            for (int item = 1; item <= PARTIAL_COUNT; item++)
                add(today + 60 * 60 * 1000 + item * 1000, (item - 1) % 3 + 1);
        });
        try (SqliteImportStore store = new SqliteImportStore(context());
                SqliteLedger ledger = new SqliteLedger(context())) {
            HistoricalImport importer =
                    new HistoricalImport(
                            store,
                            new AndroidCallLogSource(context()),
                            ledger.outbox(),
                            new DurableDelivery(
                                    ledger.outbox(), new AndroidCalendarSink(context())));
            HistoricalImport.Session session =
                    importer.start(calendar(), zone().getId(), today, tomorrow, true, true, true);
            while (session.phase == HistoricalImport.Phase.BUILDING)
                session = importer.preview(session.id, 20);
            assertEquals(PARTIAL_COUNT, session.count);
            importer.confirm(session.id);
            session = importer.deliver(session.id, 1);
            assertEquals(1, session.saved);
            assertEquals(HistoricalImport.Phase.CONFIRMED, session.phase);
            assertTrue(state().edit().putString("partial", session.id).commit());
        }
        assertEquals(4, fixture.events(calendar()));
    }

    @Test
    public void renderedExplicitHistoryResumeCompletesPartialWork() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        RenderedUi ui = new RenderedUi(instrumentation);
        Activity activity = null;
        try {
            activity = open(instrumentation);
            String dates = today() + " → " + today().plusDays(1) + " (" + zone().getId() + ")";
            ui.tapText(
                    context()
                            .getString(
                                    R.string.import_history_item,
                                    dates,
                                    context().getString(R.string.import_confirmed)));
            assertEquals(
                    "The selected history row must resolve to the prepared session",
                    state().getString("partial", null),
                    latest().id);
            assertEquals(HistoricalImport.Phase.CONFIRMED, latest().phase);
            assertEquals(1, latest().saved);
            assertEquals(4, fixture.events(calendar()));
            ui.revealId("import_summary_text");
            ui.capture("import-persisted-partial");
            ui.revealId("import_stop");
            ui.tapId("import_continue");
            ui.await(() -> latest().saved >= 2, "Resumed work did not start", 60000);
            ui.tapIdWhileBusy("import_stop");
            ui.await(
                    () ->
                            ui.shown("import_continue", true) != null
                                    && ui.shown("import_continue", true).isEnabled(),
                    "Stopped run did not settle");
            HistoricalImport.Session stopped = latest();
            assertEquals(state().getString("partial", null), stopped.id);
            assertEquals(HistoricalImport.Phase.CONFIRMED, stopped.phase);
            assertTrue(stopped.saved >= 2 && stopped.saved < PARTIAL_COUNT);
            assertEquals(3 + stopped.saved, fixture.events(calendar()));
            ui.revealId("import_summary_text");
            ui.capture("import-user-stopped-partial");
            close(instrumentation, activity);
            activity = open(instrumentation);
            ui.tapText(
                    context()
                            .getString(
                                    R.string.import_history_item,
                                    dates,
                                    context().getString(R.string.import_confirmed)));
            assertEquals(stopped.saved, latest().saved);
            ui.tapId("import_continue");
            ui.await(
                    () -> phase(HistoricalImport.Phase.COMPLETE),
                    "Explicit history resume did not complete",
                    150000);
            HistoricalImport.Session done = latest();
            assertEquals(state().getString("partial", null), done.id);
            assertEquals(PARTIAL_COUNT, done.saved + done.review);
            assertTrue(
                    "Only the interrupted boundary may need conservative review", done.review <= 1);
            assertEquals(3 + done.saved, fixture.events(calendar()));
            ui.revealId("import_summary_text");
            ui.capture("import-resumed-complete");
            ui.await(
                    () ->
                            ui.find(context().getString(R.string.import_stopping), false) == null
                                    && ui.find(context().getString(R.string.import_blocked), false)
                                            == null,
                    "Completed import must clear stale stop feedback");
            android.os.Bundle observation = new android.os.Bundle();
            observation.putLong("stoppedSaved", stopped.saved);
            observation.putLong("finalSaved", done.saved);
            observation.putLong("finalReview", done.review);
            observation.putLong("verifiedEvents", fixture.events(calendar()));
            instrumentation.sendStatus(0, observation);
        } catch (Throwable failure) {
            try {
                ui.capture("resume-failure");
            } catch (Throwable capture) {
                failure.addSuppressed(capture);
            }
            throw failure;
        } finally {
            close(instrumentation, activity);
        }
    }

    @Test
    public void removedDestinationBlocksDeliveryWithoutSwitchingCalendars() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        RenderedUi ui = new RenderedUi(instrumentation);
        String removedLabel = fixture.calendarDisplayName() + " temporary";
        long removed = fixture.insertCalendar(removedLabel);
        long remainingEvents = fixture.events(calendar());
        try (ActivityScenario<HistoricalImportActivity> scenario =
                ActivityScenario.launch(HistoricalImportActivity.class)) {
            ui.tapId("import_calendar_picker");
            ui.tapText(removedLabel);
            ui.setTextId("import_from", today().minusDays(1).toString());
            ui.setTextId("import_to", today().toString());
            ui.dismissKeyboard();
            preview(ui);
            String sessionId = latest().id;
            assertEquals(removed, latest().calendar);
            assertEquals(0, fixture.events(removed));

            fixture.removeCalendar(removed);
            scenario.recreate();
            assertEquals(sessionId, latest().id);
            assertEquals(HistoricalImport.Phase.PREVIEW, latest().phase);
            assertEquals(remainingEvents, fixture.events(calendar()));

            ui.revealId("import_confirm");
            ui.tapId("import_confirm");
            String unavailable = context().getString(R.string.auto_destination_unavailable);
            ui.await(
                    () -> {
                        android.view.accessibility.AccessibilityNodeInfo message =
                                ui.find("calltrack_dialog_message", true);
                        return message != null
                                && message.getText() != null
                                && message.getText().toString().contains(unavailable);
                    },
                    "Confirmation did not disclose the missing calendar");
            ui.capture("import-missing-destination-confirmation");
            ui.tapDialogPositive();
            ui.await(
                    () -> latest().stopped == DurableDelivery.Result.DESTINATION_UNAVAILABLE,
                    "Delivery did not stop when its approved calendar disappeared");
            assertEquals(sessionId, latest().id);
            assertEquals(removed, latest().calendar);
            assertEquals(0, latest().saved);
            assertEquals(0, fixture.events(removed));
            assertEquals(remainingEvents, fixture.events(calendar()));
            ui.revealId("import_summary_text");
            ui.capture("import-destination-removed");
        }
    }

    @Test
    public void cleanupFixture() {
        new ActivationControl(new AndroidActivationStore(context())).pause();
        if (state().getBoolean("ownsCalls", false))
            shellCalls(() -> {
                FixtureCallLog.delete(
                        context(), Calls.PHONE_ACCOUNT_ID + "=?", new String[] {tag()});
                try (Cursor c =
                        FixtureCallLog.query(
                                context(),
                                new String[] {Calls._ID},
                                Calls.PHONE_ACCOUNT_ID + "=?",
                                new String[] {tag()},
                                null)) {
                    assertNotNull(c);
                    assertEquals(0, c.getCount());
                }
            });
        fixture.cleanupFixture();
    }
}
