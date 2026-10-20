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
    private Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private String account() {
        String token = InstrumentationRegistry.getArguments().getString("fixtureToken", "");
        if (!token.matches("[a-f0-9]{32}"))
            throw new IllegalArgumentException("Required isolated fixture token");
        return "org.calltrack.community.dev.fixture." + token;
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

    @Test
    public void nativeRoundtripAndReopenedLedger() {
        cleanup();
        String db = "slice-test-" + account() + ".db";
        try {
            ContentValues values = new ContentValues();
            values.put(Calendars.ACCOUNT_NAME, account());
            values.put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
            values.put(Calendars.NAME, account());
            values.put(Calendars.CALENDAR_DISPLAY_NAME, "CallTrack isolated test");
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
                    android.provider.CallLog.Calls.PRESENTATION_RESTRICTED
                });
        CallRecord call = AndroidCallLogSource.first(cursor, "fixture");
        assertNotNull(call);
        assertEquals("77", call.id);
        assertFalse(call.identityAllowed);
        assertFalse(CallEvent.from(call, "UTC").title.contains("Secret"));
        cursor.close();
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
