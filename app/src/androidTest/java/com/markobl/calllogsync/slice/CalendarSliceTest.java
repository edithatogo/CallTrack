package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.Manifest;
import android.content.*;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

/** Synthetic source only. Never queries or changes the device's call history. */
public class CalendarSliceTest {
    private static void expectFailure(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("Unexpected failure type", failure);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private String account() {
        String token = InstrumentationRegistry.getArguments().getString("fixtureToken", "");
        if (!token.matches("[a-f0-9]{32}"))
            throw new IllegalArgumentException("Required isolated fixture token");
        return "org.calltrack.community.dev.fixture." + token;
    }

    private String calendarDisplayName() {
        String token = account();
        return "CallTrack isolated test " + token.substring(token.length() - 8);
    }

    private Uri syncUri(Uri uri) {
        return uri.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(Calendars.ACCOUNT_NAME, account())
                .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                .build();
    }

    private void cleanup() {
        context()
                .getContentResolver()
                .delete(
                        syncUri(Calendars.CONTENT_URI),
                        Calendars.ACCOUNT_NAME + "=? AND " + Calendars.ACCOUNT_TYPE + "=?",
                        new String[] {account(), CalendarContract.ACCOUNT_TYPE_LOCAL});
        try (Cursor c =
                context()
                        .getContentResolver()
                        .query(
                                Calendars.CONTENT_URI,
                                new String[] {Calendars._ID},
                                Calendars.ACCOUNT_NAME + "=? AND " + Calendars.ACCOUNT_TYPE + "=?",
                                new String[] {account(), CalendarContract.ACCOUNT_TYPE_LOCAL},
                                null)) {
            assertNotNull(c);
            assertEquals(0, c.getCount());
        }
    }

    @Test
    public void cleanupFixture() {
        cleanup();
    }

    @Test
    public void deniedCalendarPermission() {
        assertNotEquals(
                android.content.pm.PackageManager.PERMISSION_GRANTED,
                context().checkSelfPermission(Manifest.permission.READ_CALENDAR));
        assertEquals(
                SliceEngine.Result.PERMISSION_REQUIRED,
                new SliceEngine(
                                () -> {
                                    throw new AssertionError("Source must not be read");
                                },
                                new AndroidCalendarSink(context()),
                                new MemoryLedger(),
                                "UTC")
                        .run(1));
    }

    private void verifyPicker(long savedCalendar, boolean expectChoice) {
        context()
                .getSharedPreferences("slice", Context.MODE_PRIVATE)
                .edit()
                .putLong("calendar", savedCalendar)
                .commit();
        android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        RenderedUi ui = new RenderedUi(instrumentation);
        android.app.Activity activity =
                instrumentation.startActivitySync(
                        new Intent(context(), CalendarSliceActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            ui.await(
                    () -> {
                        android.view.accessibility.AccessibilityNodeInfo picker =
                                ui.find("slice_calendar_picker", true);
                        return picker != null && picker.isEnabled();
                    },
                    "Compose picker did not restore explicit choice / missing-destination"
                            + " placeholder");
            assertEquals(
                    expectChoice ? savedCalendar : -1,
                    context()
                            .getSharedPreferences("slice", Context.MODE_PRIVATE)
                            .getLong("calendar", -1));
            String expectedLabel =
                    expectChoice
                            ? calendarDisplayName()
                            : context().getString(com.markobl.calllogsync.R.string.slice_choose);
            ui.await(
                    () -> ui.find(expectedLabel, false) != null,
                    "Compose picker did not show the saved choice / placeholder label");
            instrumentation.runOnMainSync(
                    () -> {
                        long actual =
                                context()
                                        .getSharedPreferences("slice", Context.MODE_PRIVATE)
                                        .getLong("calendar", -1);
                        assertEquals(expectChoice ? savedCalendar : -1, actual);
                    });
        } finally {
            instrumentation.runOnMainSync(activity::finish);
        }
    }

    private void chooseCalendar(long expectedId) {
        android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        RenderedUi ui = new RenderedUi(instrumentation);
        android.app.Activity activity =
                instrumentation.startActivitySync(
                        new Intent(context(), CalendarSliceActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            ui.await(
                    () -> {
                        android.view.accessibility.AccessibilityNodeInfo picker =
                                ui.find("slice_calendar_picker", true);
                        return picker != null && picker.isEnabled();
                    },
                    "Manual Compose destination picker did not load provider choices");
            ui.tapVisibleId("slice_calendar_picker");
            ui.tapVisibleText(calendarDisplayName());
            assertEquals(
                    "Only the explicit provider option tap persists its calendar ID",
                    expectedId,
                    context()
                            .getSharedPreferences("slice", Context.MODE_PRIVATE)
                            .getLong("calendar", -1));
            ui.await(
                    () -> ui.find(calendarDisplayName(), false) != null,
                    "Manual Compose picker did not show the explicitly selected calendar");
        } finally {
            instrumentation.runOnMainSync(activity::finish);
        }
    }

    @Test
    public void nativeRoundtripAndReopenedLedger() {
        cleanup();
        String db = "slice-test-" + account() + ".db";
        try {
            ContentValues values = new ContentValues();
            values.put(Calendars.ACCOUNT_NAME, account());
            values.put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
            values.put(Calendars.NAME, account());
            values.put(Calendars.CALENDAR_DISPLAY_NAME, calendarDisplayName());
            values.put(Calendars.CALENDAR_COLOR, 0xff006699);
            values.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER);
            values.put(Calendars.OWNER_ACCOUNT, account());
            values.put(Calendars.CALENDAR_TIME_ZONE, "UTC");
            values.put(Calendars.VISIBLE, 1);
            values.put(Calendars.SYNC_EVENTS, 1);
            Uri uri = context().getContentResolver().insert(syncUri(Calendars.CONTENT_URI), values);
            assertNotNull(uri);
            long calendar = ContentUris.parseId(uri);
            AndroidCalendarSink sink = new AndroidCalendarSink(context());
            assertTrue(sink.writable(calendar));
            android.os.Bundle categories = new android.os.Bundle();
            int local = 0, google = 0, microsoft = 0, other = 0;
            for (AndroidCalendarSink.Choice choice : sink.choices()) {
                String type =
                        choice.accountType == null
                                ? ""
                                : choice.accountType.toLowerCase(java.util.Locale.ROOT);
                if (CalendarContract.ACCOUNT_TYPE_LOCAL.equals(choice.accountType)) local++;
                else if (type.equals("com.google")) google++;
                else if (type.contains("outlook")
                        || type.contains("exchange")
                        || type.contains("microsoft")) microsoft++;
                else other++;
            }
            categories.putString(
                    "calendarCategories",
                    "local="
                            + local
                            + ", google="
                            + google
                            + ", microsoft-like="
                            + microsoft
                            + ", other="
                            + other);
            InstrumentationRegistry.getInstrumentation().sendStatus(0, categories);
            verifyPicker(calendar, true);
            verifyPicker(Long.MAX_VALUE, false);
            chooseCalendar(calendar);
            CallRecord call =
                    new CallRecord(
                            "synthetic-" + account(),
                            "1",
                            1700000000123L,
                            65,
                            "Incoming",
                            "Fixture Person",
                            "0000000000",
                            true);
            try (SqliteLedger ledger = new SqliteLedger(context(), db)) {
                assertEquals(
                        SliceEngine.Result.SAVED,
                        new SliceEngine(() -> call, sink, ledger, "UTC").run(calendar));
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), db)) {
                assertEquals(
                        SliceEngine.Result.ALREADY_LOGGED,
                        new SliceEngine(() -> call, sink, ledger, "UTC").run(calendar));
            }
            java.util.List<SliceEngine.StoredEvent> rows =
                    sink.find(calendar, CallEvent.from(call, "UTC").marker);
            assertEquals(1, rows.size());
            assertEquals(
                    CallEvent.from(call, "UTC").fingerprint(), rows.get(0).event.fingerprint());
            assertEquals(1700000000123L, rows.get(0).event.start);
            assertEquals(1700000065123L, rows.get(0).event.end);
            // Prove marker recovery through the real provider after insertion without a receipt.
            CallRecord gap =
                    new CallRecord(
                            "synthetic-" + account(),
                            "2",
                            1700000100123L,
                            12,
                            "Outgoing",
                            null,
                            null,
                            true);
            SliceEngine.Ledger crashLedger =
                    new SliceEngine.Ledger() {
                        public SliceEngine.Result transaction(SliceEngine.Work w) {
                            return w.run();
                        }

                        public SliceEngine.Entry get(String key, long destination) {
                            return null;
                        }

                        public void put(String key, long destination, SliceEngine.Entry entry) {
                            throw new IllegalStateException("simulated interruption");
                        }
                    };
            assertEquals(
                    SliceEngine.Result.FAILED,
                    new SliceEngine(() -> gap, sink, crashLedger, "UTC").run(calendar));
            try (SqliteLedger ledger = new SqliteLedger(context(), db)) {
                assertEquals(
                        SliceEngine.Result.RECOVERED,
                        new SliceEngine(() -> gap, sink, ledger, "UTC").run(calendar));
            }
            assertEquals(1, sink.find(calendar, CallEvent.from(gap, "UTC").marker).size());
            long eventId = rows.get(0).id;
            ContentValues edited = new ContentValues();
            edited.put(CalendarContract.Events.TITLE, "Human edit");
            context()
                    .getContentResolver()
                    .update(
                            ContentUris.withAppendedId(
                                    CalendarContract.Events.CONTENT_URI, eventId),
                            edited,
                            null,
                            null);
            try (SqliteLedger ledger = new SqliteLedger(context(), db)) {
                assertEquals(
                        SliceEngine.Result.REVIEW_REQUIRED,
                        new SliceEngine(() -> call, sink, ledger, "UTC").run(calendar));
            }
            assertEquals("Human edit", sink.read(eventId).event.title);
            for (Uri table :
                    new Uri[] {
                        CalendarContract.Reminders.CONTENT_URI,
                        CalendarContract.Attendees.CONTENT_URI
                    }) {
                try (Cursor cursor =
                        context()
                                .getContentResolver()
                                .query(
                                        table,
                                        new String[] {"_id"},
                                        "event_id=?",
                                        new String[] {Long.toString(eventId)},
                                        null)) {
                    assertNotNull(cursor);
                    assertEquals(0, cursor.getCount());
                }
            }
            assertFalse(sink.writable(Long.MAX_VALUE));
            ContentValues access = new ContentValues();
            access.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_READ);
            context()
                    .getContentResolver()
                    .update(
                            syncUri(ContentUris.withAppendedId(Calendars.CONTENT_URI, calendar)),
                            access,
                            null,
                            null);
            assertFalse(sink.writable(calendar));
        } finally {
            cleanup();
            context().deleteDatabase(db);
        }
    }

    @Test
    public void syntheticCursorMapsPresentationWithoutPersonalQuery() {
        assertNotEquals(
                android.content.pm.PackageManager.PERMISSION_GRANTED,
                context().checkSelfPermission(Manifest.permission.READ_CALL_LOG));
        MatrixCursor cursor = new MatrixCursor(AndroidCallLogSource.PROJECTION);
        cursor.addRow(
                new Object[] {
                    "77",
                    "555",
                    android.provider.CallLog.Calls.INCOMING_TYPE,
                    1700000000123L,
                    65,
                    "Cached Secret",
                    android.provider.CallLog.Calls.PRESENTATION_RESTRICTED,
                    null
                });
        CallRecord call = AndroidCallLogSource.first(cursor, "fixture");
        assertNotNull(call);
        assertEquals("77", call.id);
        assertFalse(call.identityAllowed);
        assertTrue(call.presentationSupplied);
        assertEquals(
                Integer.valueOf(android.provider.CallLog.Calls.PRESENTATION_RESTRICTED),
                call.numberPresentation);
        assertFalse(CallEvent.from(call, "UTC").title.contains("Secret"));
        cursor.close();
    }

    @Test
    public void syntheticCursorRetainsNullAndExtendedPresentation() {
        assertNotEquals(
                android.content.pm.PackageManager.PERMISSION_GRANTED,
                context().checkSelfPermission(Manifest.permission.READ_CALL_LOG));
        for (Integer presentation : new Integer[] {null, 3, 4, 5, 99}) {
            try (MatrixCursor cursor = new MatrixCursor(AndroidCallLogSource.PROJECTION)) {
                cursor.addRow(
                        new Object[] {
                            "78",
                            "555",
                            1,
                            1700000000123L,
                            2147483652L,
                            "Cached Secret",
                            presentation,
                            "fixture-SIM"
                        });
                CallRecord call = AndroidCallLogSource.first(cursor, "fixture");
                assertTrue(call.presentationSupplied);
                assertEquals(presentation, call.numberPresentation);
                assertEquals(2147483652L, call.seconds);
                assertEquals("fixture-SIM", call.phoneAccountId);
                assertFalse(call.identityAllowed);
                assertFalse(CallEvent.from(call, "UTC").title.contains("Secret"));
                assertFalse(CallEvent.from(call, "UTC").description.contains("555"));
            }
        }
    }

    @Test
    public void syntheticPagingUsesBoundedQueriesAndPreservesEveryRow() {
        android.os.Bundle query =
                AndroidCallLogSource.pageQuery(
                        "_id>? AND _id<=?", new String[] {"-1", "7"}, true, 3);
        assertArrayEquals(
                new String[] {android.provider.CallLog.Calls._ID},
                query.getStringArray(android.content.ContentResolver.QUERY_ARG_SORT_COLUMNS));
        assertEquals(3, query.getInt(android.content.ContentResolver.QUERY_ARG_LIMIT));
        assertEquals(
                android.content.ContentResolver.QUERY_SORT_DIRECTION_ASCENDING,
                query.getInt(android.content.ContentResolver.QUERY_ARG_SORT_DIRECTION));
        assertArrayEquals(
                new String[] {"-1", "7"},
                query.getStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS));
        try (MatrixCursor cursor = new MatrixCursor(AndroidCallLogSource.PROJECTION)) {
            for (int i = 1; i <= 3; i++)
                cursor.addRow(
                        new Object[] {Integer.toString(i), "000", 1, 1000L + i, 0L, null, 3, null});
            java.util.List<CallRecord> rows = AndroidCallLogSource.pageRows(cursor, "fixture", 3);
            assertEquals(3, rows.size());
            assertEquals("1", rows.get(0).id);
            assertEquals("3", rows.get(2).id);
            assertEquals(Integer.valueOf(3), rows.get(2).numberPresentation);
        }
        try (MatrixCursor cursor = new MatrixCursor(AndroidCallLogSource.PROJECTION)) {
            for (int i = 1; i <= 4; i++)
                cursor.addRow(
                        new Object[] {Integer.toString(i), "000", 1, 1000L + i, 0L, null, 3, null});
            expectFailure(
                    PagedCalls.SourceChanged.class,
                    () -> AndroidCallLogSource.pageRows(cursor, "fixture", 3));
        }
        assertNotEquals(
                android.content.pm.PackageManager.PERMISSION_GRANTED,
                context().checkSelfPermission(Manifest.permission.READ_CALL_LOG));
    }

    private static class MemoryLedger implements SliceEngine.Ledger {
        public SliceEngine.Result transaction(SliceEngine.Work w) {
            return w.run();
        }

        public SliceEngine.Entry get(String key, long calendar) {
            return null;
        }

        public void put(String key, long calendar, SliceEngine.Entry e) {
            throw new AssertionError("No writes expected");
        }
    }
}
