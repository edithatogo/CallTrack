package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

/** Actual LOCAL calendar and SQLite, synthetic source only; no device-call access. */
public class ParityProviderTest {
    Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    String account() {
        String token = InstrumentationRegistry.getArguments().getString("fixtureToken", "");
        if (!token.matches("[a-f0-9]{32}")) throw new IllegalArgumentException("Fixture required");
        return "org.calltrack.community.dev.parity." + token;
    }

    String calendarDisplayName() {
        String token = account();
        return "CallTrack parity fixture " + token.substring(token.length() - 8);
    }

    private Uri sync(Uri uri) {
        return uri.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(Calendars.ACCOUNT_NAME, account())
                .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                .build();
    }

    @Test
    public void cleanupFixture() {
        java.util.List<Long> calendarIds = new java.util.ArrayList<>();
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
            while (c.moveToNext()) calendarIds.add(c.getLong(0));
        }
        context()
                .getContentResolver()
                .delete(
                        sync(Calendars.CONTENT_URI),
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
        for (long id : calendarIds) {
            try (Cursor c =
                    context()
                            .getContentResolver()
                            .query(
                                    Events.CONTENT_URI,
                                    new String[] {Events._ID},
                                    Events.CALENDAR_ID + "=?",
                                    new String[] {Long.toString(id)},
                                    null)) {
                assertNotNull(c);
                assertEquals("Calendar cleanup must remove its events too", 0, c.getCount());
            }
        }
    }

    long createCalendar() {
        cleanupFixture();
        assertEquals(
                PackageManager.PERMISSION_DENIED,
                context().checkSelfPermission(Manifest.permission.READ_CALL_LOG));
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
        Uri created = context().getContentResolver().insert(sync(Calendars.CONTENT_URI), values);
        assertNotNull(created);
        return ContentUris.parseId(created);
    }

    private CallRecord call(long id) {
        return new CallRecord(
                account(),
                Long.toString(id),
                1700000000123L + id * 100000,
                id == 1 ? 0 : 65,
                id == 1 ? 3 : 2,
                "Synthetic Person",
                "0000000000",
                Integer.valueOf(id == 1 ? 2 : 1),
                null);
    }

    private void failState(SqliteLedger ledger, String state) {
        // State comes only from the test's fixed enum names, never application/provider data.
        ledger.getWritableDatabase()
                .execSQL(
                        "CREATE TRIGGER fail_fixture BEFORE INSERT ON outbox "
                                + "WHEN NEW.state='"
                                + state
                                + "' BEGIN SELECT RAISE(ABORT,'fixture'); END");
    }

    long events(long calendar) {
        try (Cursor c =
                context()
                        .getContentResolver()
                        .query(
                                Events.CONTENT_URI,
                                new String[] {Events._ID},
                                Events.CALENDAR_ID + "=? AND " + Events.DELETED + "=0",
                                new String[] {Long.toString(calendar)},
                                null)) {
            assertNotNull(c);
            return c.getCount();
        }
    }

    @Test
    public void deniedCalendarPermissionCannotInsert() {
        assertEquals(
                PackageManager.PERMISSION_DENIED,
                context().checkSelfPermission(Manifest.permission.READ_CALENDAR));
        try {
            new AndroidCalendarSink(context()).writable(1);
            fail("Calendar access must be denied");
        } catch (SecurityException expected) {
        }
    }

    @Test
    public void durableCommitFailuresRecoverRealEffectsWithoutReinsertion() {
        long calendar = createCalendar();
        String name = account() + "-faults.db";
        AndroidCalendarSink sink = new AndroidCalendarSink(context());
        CallRecord first = call(1), second = call(2);
        try {
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                failState(ledger, "READY");
                assertEquals(
                        DurableDelivery.Result.FAILED,
                        new DurableDelivery(ledger.outbox(), sink)
                                .deliver(first, calendar, "UTC", () -> true));
                assertNull(ledger.outbox().get(first.key(), calendar));
                assertEquals(0, events(calendar));
                ledger.getWritableDatabase().execSQL("DROP TRIGGER fail_fixture");
                failState(ledger, "ATTEMPTED");
                SliceEngine.CalendarSink boundary =
                        new SliceEngine.CalendarSink() {
                            public boolean writable(long id) {
                                return sink.writable(id);
                            }

                            public java.util.List<SliceEngine.StoredEvent> find(
                                    long id, String marker) {
                                return sink.find(id, marker);
                            }

                            public SliceEngine.StoredEvent read(long id) {
                                return sink.read(id);
                            }

                            public long insert(long id, CallEvent event) {
                                // A separate connection must observe the committed pre-effect
                                // boundary.
                                try (SqliteLedger reopened = new SqliteLedger(context(), name)) {
                                    DurableDelivery.Intent intent =
                                            reopened.outbox().get(first.key(), id);
                                    assertEquals(DurableDelivery.State.IN_FLIGHT, intent.state);
                                    assertEquals(-1, intent.eventId);
                                    assertNotNull(intent.payload);
                                }
                                return sink.insert(id, event);
                            }
                        };
                assertEquals(
                        DurableDelivery.Result.FAILED,
                        new DurableDelivery(ledger.outbox(), boundary)
                                .deliver(first, calendar, "UTC", () -> true));
                assertEquals(
                        DurableDelivery.State.IN_FLIGHT,
                        ledger.outbox().get(first.key(), calendar).state);
                assertEquals(1, events(calendar));
                ledger.getWritableDatabase().execSQL("DROP TRIGGER fail_fixture");
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                DurableDelivery delivery = new DurableDelivery(ledger.outbox(), sink);
                assertEquals(
                        DurableDelivery.Result.RECOVERED,
                        delivery.deliver(first, calendar, "UTC", () -> true));
                assertNull(ledger.outbox().get(first.key(), calendar).payload);
                assertEquals(1, events(calendar));
                failState(ledger, "CONFIRMED");
                assertEquals(
                        DurableDelivery.Result.FAILED,
                        delivery.deliver(second, calendar, "UTC", () -> true));
                DurableDelivery.Intent known = ledger.outbox().get(second.key(), calendar);
                assertEquals(DurableDelivery.State.ATTEMPTED, known.state);
                assertTrue(known.eventId >= 0);
                assertNotNull(sink.read(known.eventId));
                assertEquals(2, events(calendar));
                ledger.getWritableDatabase().execSQL("DROP TRIGGER fail_fixture");
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                assertEquals(
                        DurableDelivery.Result.ALREADY_LOGGED,
                        new DurableDelivery(ledger.outbox(), sink)
                                .deliver(second, calendar, "UTC", () -> true));
                assertEquals(
                        DurableDelivery.State.CONFIRMED,
                        ledger.outbox().get(second.key(), calendar).state);
                assertEquals(2, events(calendar));
            }
        } finally {
            cleanupFixture();
            assertTrue(context().deleteDatabase(name));
        }
    }

    @Test
    public void durableDeliveryPreservesRealEditsDeletionAndUnavailableDestination() {
        long calendar = createCalendar();
        String name = account() + "-edits.db";
        AndroidCalendarSink sink = new AndroidCalendarSink(context());
        CallRecord first = call(1), second = call(2);
        try {
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                DurableDelivery delivery = new DurableDelivery(ledger.outbox(), sink);
                assertEquals(
                        DurableDelivery.Result.SAVED,
                        delivery.deliver(first, calendar, "UTC", () -> true));
                long id = ledger.outbox().get(first.key(), calendar).eventId;
                CallEvent actual = sink.read(id).event;
                assertEquals(first.start, actual.start);
                assertEquals(first.start + 60000, actual.end);
                assertEquals(0, ledger.outbox().get(first.key(), calendar).facts.seconds);
                assertFalse(actual.title.contains("Synthetic Person"));
                assertFalse(actual.title.contains("0000000000"));
                for (Uri table :
                        new Uri[] {
                            CalendarContract.Reminders.CONTENT_URI,
                            CalendarContract.Attendees.CONTENT_URI
                        }) {
                    try (Cursor c =
                            context()
                                    .getContentResolver()
                                    .query(
                                            table,
                                            new String[] {"_id"},
                                            "event_id=?",
                                            new String[] {Long.toString(id)},
                                            null)) {
                        assertNotNull(c);
                        assertEquals(0, c.getCount());
                    }
                }
                ContentValues edit = new ContentValues();
                edit.put(Events.TITLE, "Human edit");
                assertEquals(
                        1,
                        context()
                                .getContentResolver()
                                .update(
                                        ContentUris.withAppendedId(Events.CONTENT_URI, id),
                                        edit,
                                        null,
                                        null));
                assertEquals(
                        DurableDelivery.Result.REVIEW_REQUIRED,
                        delivery.deliver(first, calendar, "UTC", () -> true));
                assertEquals("Human edit", sink.read(id).event.title);
                assertEquals(
                        DurableDelivery.Result.SAVED,
                        delivery.deliver(second, calendar, "UTC", () -> true));
                long deleted = ledger.outbox().get(second.key(), calendar).eventId;
                assertEquals(
                        1,
                        context()
                                .getContentResolver()
                                .delete(
                                        ContentUris.withAppendedId(Events.CONTENT_URI, deleted),
                                        null,
                                        null));
                assertEquals(
                        DurableDelivery.Result.REVIEW_REQUIRED,
                        delivery.deliver(second, calendar, "UTC", () -> true));
                assertEquals(deleted, ledger.outbox().get(second.key(), calendar).eventId);
                assertEquals(1, events(calendar));
                ContentValues access = new ContentValues();
                access.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_READ);
                assertEquals(
                        1,
                        context()
                                .getContentResolver()
                                .update(
                                        sync(
                                                ContentUris.withAppendedId(
                                                        Calendars.CONTENT_URI, calendar)),
                                        access,
                                        null,
                                        null));
                assertEquals(
                        DurableDelivery.Result.DESTINATION_UNAVAILABLE,
                        delivery.deliver(call(3), calendar, "UTC", () -> true));
                assertNull(ledger.outbox().get(call(3).key(), calendar));
                assertEquals(1, events(calendar));
            }
        } finally {
            cleanupFixture();
            assertTrue(context().deleteDatabase(name));
        }
    }

    @Test
    public void historicalImportPreviewResumeRepeatAndCancelMatchRealEffects() {
        long calendar = createCalendar();
        String name = account() + "-import";
        AndroidCalendarSink sink = new AndroidCalendarSink(context());
        HistoricalImportStoreTest.Source source =
                new HistoricalImportStoreTest.Source() {
                    @Override
                    public String namespace() {
                        return account();
                    }
                };
        for (long i = 1; i <= 12; i++) source.rows.put(i, call(i));
        String id;
        try {
            try (SqliteImportStore imports = new SqliteImportStore(context(), name + ".db");
                    SqliteLedger ledger = new SqliteLedger(context(), name + "-ledger.db")) {
                HistoricalImport importer =
                        new HistoricalImport(
                                imports,
                                source,
                                ledger.outbox(),
                                new DurableDelivery(ledger.outbox(), sink));
                id =
                        importer.start(
                                        calendar,
                                        "UTC",
                                        call(1).start,
                                        call(13).start,
                                        true,
                                        true,
                                        true)
                                .id;
                while (imports.get(id).phase == HistoricalImport.Phase.BUILDING)
                    importer.preview(id, 3);
                assertEquals(12, imports.get(id).count);
                assertEquals(0, events(calendar));
                importer.confirm(id);
                importer.deliver(id, 2);
                assertEquals(2, imports.get(id).saved);
                assertEquals(2, events(calendar));
            }
            try (SqliteImportStore imports = new SqliteImportStore(context(), name + ".db");
                    SqliteLedger ledger = new SqliteLedger(context(), name + "-ledger.db")) {
                HistoricalImport importer =
                        new HistoricalImport(
                                imports,
                                source,
                                ledger.outbox(),
                                new DurableDelivery(ledger.outbox(), sink));
                assertEquals(2, imports.get(id).saved);
                while (imports.get(id).phase == HistoricalImport.Phase.CONFIRMED)
                    importer.deliver(id, 2);
                assertEquals(12, imports.get(id).saved);
                assertEquals(12, events(calendar));
                String repeat =
                        importer.start(
                                        calendar,
                                        "UTC",
                                        call(1).start,
                                        call(13).start,
                                        true,
                                        true,
                                        true)
                                .id;
                while (imports.get(repeat).phase == HistoricalImport.Phase.BUILDING)
                    importer.preview(repeat, 3);
                importer.confirm(repeat);
                while (imports.get(repeat).phase == HistoricalImport.Phase.CONFIRMED)
                    importer.deliver(repeat, 2);
                assertEquals(12, imports.get(repeat).saved);
                assertEquals(12, events(calendar));
                source.rows.put(13L, call(13));
                source.rows.put(14L, call(14));
                String cancel =
                        importer.start(
                                        calendar,
                                        "UTC",
                                        call(13).start,
                                        call(15).start,
                                        true,
                                        true,
                                        true)
                                .id;
                while (imports.get(cancel).phase == HistoricalImport.Phase.BUILDING)
                    importer.preview(cancel, 3);
                assertEquals(2, imports.get(cancel).count);
                assertEquals(12, events(calendar));
                importer.confirm(cancel);
                importer.deliver(cancel, 1);
                assertEquals(1, imports.get(cancel).saved);
                importer.cancel(cancel);
                importer.deliver(cancel, 3);
                assertEquals(HistoricalImport.Phase.CANCELLED, imports.get(cancel).phase);
                assertEquals(1, imports.get(cancel).saved);
                assertEquals(13, events(calendar));
                assertNull(ledger.outbox().get(call(14).key(), calendar));
            }
        } finally {
            cleanupFixture();
            assertTrue(context().deleteDatabase(name + ".db"));
            assertTrue(context().deleteDatabase(name + "-ledger.db"));
        }
    }

    @Test
    public void prepareInterruptedEffectForReadOnlyRecovery() {
        long calendar = createCalendar();
        try (SqliteLedger ledger = new SqliteLedger(context(), account() + "-readonly.db")) {
            failState(ledger, "ATTEMPTED");
            assertEquals(
                    DurableDelivery.Result.FAILED,
                    new DurableDelivery(ledger.outbox(), new AndroidCalendarSink(context()))
                            .deliver(call(1), calendar, "UTC", () -> true));
            assertEquals(
                    DurableDelivery.State.IN_FLIGHT,
                    ledger.outbox().get(call(1).key(), calendar).state);
            assertEquals(1, events(calendar));
            ledger.getWritableDatabase().execSQL("DROP TRIGGER fail_fixture");
        }
        // Deliberately remains for the next instrumentation process after write revocation.
        // The harness restores write access and verifies scoped cleanup even on failure.
    }

    @Test
    public void readOnlyRecoveryVerifiesExistingEffectWithoutWritePermission() {
        assertEquals(
                PackageManager.PERMISSION_GRANTED,
                context().checkSelfPermission(Manifest.permission.READ_CALENDAR));
        assertEquals(
                PackageManager.PERMISSION_DENIED,
                context().checkSelfPermission(Manifest.permission.WRITE_CALENDAR));
        long calendar;
        try (Cursor c =
                context()
                        .getContentResolver()
                        .query(
                                Calendars.CONTENT_URI,
                                new String[] {Calendars._ID},
                                Calendars.ACCOUNT_NAME + "=?",
                                new String[] {account()},
                                null)) {
            assertNotNull(c);
            assertEquals(1, c.getCount());
            assertTrue(c.moveToFirst());
            calendar = c.getLong(0);
        }
        AndroidCalendarSink sink = new AndroidCalendarSink(context());
        try (SqliteLedger ledger = new SqliteLedger(context(), account() + "-readonly.db")) {
            assertEquals(
                    DurableDelivery.State.IN_FLIGHT,
                    ledger.outbox().get(call(1).key(), calendar).state);
            ReceiptRecovery recovery =
                    new ReceiptRecovery(
                            ledger.outbox(),
                            new ReceiptRecovery.ExistingEvents() {
                                public SliceEngine.StoredEvent read(long id) {
                                    return sink.read(id);
                                }

                                public java.util.List<SliceEngine.StoredEvent> find(
                                        long id, String marker) {
                                    return sink.find(id, marker);
                                }
                            });
            assertEquals(
                    ReceiptRecovery.Result.VERIFIED, recovery.reconcile(call(1).key(), calendar));
            assertEquals(1, events(calendar));
            DurableDelivery.Intent saved = ledger.outbox().get(call(1).key(), calendar);
            assertEquals(DurableDelivery.State.CONFIRMED, saved.state);
            assertTrue(saved.eventId >= 0);
            assertNull(saved.payload);
        }
    }
}
