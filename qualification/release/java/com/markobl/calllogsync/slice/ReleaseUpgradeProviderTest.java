package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.provider.CalendarContract.Events;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/** Synthetic, source-pinned historical schemas across a real package replacement. */
public class ReleaseUpgradeProviderTest extends ParityProviderTest {

    private ReceiptRecovery recovery(SqliteLedger ledger, AndroidCalendarSink sink) {
        return new ReceiptRecovery(
                ledger.outbox(),
                new ReceiptRecovery.ExistingEvents() {
                    public SliceEngine.StoredEvent read(long id) {
                        return sink.read(id);
                    }

                    public java.util.List<SliceEngine.StoredEvent> find(long id, String marker) {
                        return sink.find(id, marker);
                    }
                });
    }

    private SharedPreferences preferences() {
        return context().getSharedPreferences(account(), 0);
    }

    private String database(int version) {
        return account() + "-v" + version + ".db";
    }

    private CallRecord record(int version, int id) {
        return new CallRecord(
                account() + "/v" + version,
                Integer.toString(id),
                1700000000000L + version * 1000000L + id * 100000L,
                60,
                2,
                "Synthetic Upgrade",
                "0000000000",
                Integer.valueOf(1),
                null);
    }

    private long calendar() {
        long id = preferences().getLong("calendar", -1);
        assertTrue(id >= 0);
        return id;
    }

    private SQLiteDatabase historical(int version, String name) throws Exception {
        assertFalse(context().getDatabasePath(name).exists());
        SQLiteDatabase db = context().openOrCreateDatabase(name, 0, null);
        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                InstrumentationRegistry.getInstrumentation()
                                        .getContext()
                                        .getAssets()
                                        .open("release-schema-" + version + ".sql"),
                                "UTF-8"))) {
            String sql;
            while ((sql = reader.readLine()) != null) db.execSQL(sql);
        }
        db.setVersion(version);
        return db;
    }

    private void outbox(SQLiteDatabase db, int version, int id, long eventId, String state) {
        CallRecord call = record(version, id);
        CallEvent event = CallEvent.from(call, "UTC");
        ContentValues row = new ContentValues();
        row.put("source", call.key());
        row.put("calendar", calendar());
        row.put("state", state);
        row.put("event", eventId);
        row.put("signature", CallIdentity.signature(call));
        row.put("marker", event.marker);
        row.put("fingerprint", event.fingerprint());
        if (id == 4) {
            row.put("payload_title", event.title);
            row.put("payload_description", event.description);
            row.put("payload_start", event.start);
            row.put("payload_end", event.end);
            row.put("payload_timezone", event.timezone);
        }
        if (version >= 3) {
            row.put("operation_owner", "release-fixture/v" + version);
            row.put("operation_generation", 9);
        }
        if (version >= 4) {
            row.put("source_start", call.start);
            row.put("source_seconds", call.seconds);
            row.put("source_type", call.type);
            row.put("source_presentation", call.numberPresentation);
            row.put("source_presentation_supplied", 1);
        }
        db.insertOrThrow("outbox", null, row);
    }

    @Test
    public void seedHistoricalSchemasAndProviderState() throws Exception {
        assertFalse(preferences().contains("calendar"));
        long destination = createCalendar();
        assertTrue(preferences().edit().putLong("calendar", destination).commit());
        AndroidCalendarSink sink = new AndroidCalendarSink(context());
        for (int version = 1; version <= 5; version++) {
            try (SQLiteDatabase db = historical(version, database(version))) {
                for (int id = 0; id < (version == 1 ? 3 : 4); id++) {
                    CallRecord call = record(version, id);
                    CallEvent event = CallEvent.from(call, "UTC");
                    long eventId = sink.insert(destination, event);
                    assertTrue(preferences().edit().putLong(version + "/" + id, eventId).commit());
                    if (id < 3) {
                        ContentValues row = new ContentValues();
                        row.put("source", call.key());
                        row.put("calendar", destination);
                        row.put("event", eventId);
                        row.put("fingerprint", event.fingerprint());
                        db.insertOrThrow("receipts", null, row);
                    }
                    if (version >= 2)
                        outbox(
                                db,
                                version,
                                id,
                                id == 3 ? -1 : eventId,
                                id == 3 ? "IN_FLIGHT" : "CONFIRMED");
                    if (id == 0) {
                        ContentValues edit = new ContentValues();
                        edit.put(Events.TITLE, "Human edited title");
                        edit.put(
                                Events.DESCRIPTION,
                                "Human notes must survive upgrade and recovery");
                        assertEquals(
                                1,
                                context()
                                        .getContentResolver()
                                        .update(
                                                ContentUris.withAppendedId(
                                                        Events.CONTENT_URI, eventId),
                                                edit,
                                                null,
                                                null));
                        assertTrue(
                                preferences()
                                        .edit()
                                        .putString(
                                                "edited/" + version,
                                                sink.read(eventId).event.fingerprint())
                                        .commit());
                    } else if (id == 1) {
                        assertEquals(
                                1,
                                context()
                                        .getContentResolver()
                                        .delete(
                                                ContentUris.withAppendedId(
                                                        Events.CONTENT_URI, eventId),
                                                null,
                                                null));
                        assertNull(sink.read(eventId));
                    }
                }
                if (version >= 2) outbox(db, version, 4, -1, "READY");
                if (version == 5)
                    db.execSQL(
                            "INSERT INTO scan_cursors VALUES "
                                + "('release-fixture/v5',9,'synthetic-upgrade',5,'upper',2,'last')");
            }
        }
        // A real migration transaction must roll back DDL and version on failure.
        try (SQLiteDatabase db = historical(2, database(2) + "-fault")) {
            outbox(db, 2, 4, -1, "READY");
            db.execSQL(
                    "CREATE TRIGGER release_migration_fault BEFORE UPDATE ON outbox "
                            + "BEGIN SELECT RAISE(ABORT,'synthetic migration fault'); END");
        }
        try (SQLiteDatabase db = context().openOrCreateDatabase(database(7), 0, null)) {
            db.execSQL("CREATE TABLE sentinel (value TEXT)");
            db.execSQL("INSERT INTO sentinel VALUES ('must survive unsupported downgrade')");
            db.setVersion(7);
        }
        assertEquals(14, events(destination));
    }

    @Test
    public void migrateAndRecoverWithoutChangingHumanEvents() {
        long destination = calendar();
        AndroidCalendarSink sink = new AndroidCalendarSink(context());
        assertEquals(14, events(destination));
        for (int version = 1; version <= 5; version++) {
            try (SqliteLedger ledger = new SqliteLedger(context(), database(version))) {
                assertEquals(6, ledger.getWritableDatabase().getVersion());
                ReceiptRecovery recovery = recovery(ledger, sink);
                for (int id = 0; id < 3; id++) {
                    long eventId = preferences().getLong(version + "/" + id, -1);
                    assertEquals(eventId, ledger.get(record(version, id).key(), destination).id);
                    assertEquals(
                            id < 2
                                    ? ReceiptRecovery.Result.REVIEW_REQUIRED
                                    : ReceiptRecovery.Result.VERIFIED,
                            recovery.reconcile(record(version, id).key(), destination));
                }
                long edited = preferences().getLong(version + "/0", -1);
                assertEquals(
                        preferences().getString("edited/" + version, null),
                        sink.read(edited).event.fingerprint());
                assertNull(sink.read(preferences().getLong(version + "/1", -1)));
                if (version >= 2) {
                    assertEquals(
                            ReceiptRecovery.Result.VERIFIED,
                            recovery.reconcile(record(version, 3).key(), destination));
                    assertEquals(
                            preferences().getLong(version + "/3", -1),
                            ledger.outbox().get(record(version, 3).key(), destination).eventId);
                    assertEquals(
                            ReceiptRecovery.Result.NEVER_ATTEMPTED,
                            recovery.reconcile(record(version, 4).key(), destination));
                    DurableDelivery.Intent pending =
                            ledger.outbox().get(record(version, 4).key(), destination);
                    assertEquals(DurableDelivery.State.READY, pending.state);
                    assertNotNull(pending.payload);
                    assertEquals(
                            version >= 3 ? "release-fixture/v" + version : null,
                            pending.operation == null ? null : pending.operation.owner);
                    if (version >= 4) assertEquals(record(version, 4).start, pending.facts.start);
                    assertEquals(
                            DurableDelivery.Result.PAUSED,
                            new DurableDelivery(ledger.outbox(), sink)
                                    .resume(
                                            pending.key,
                                            destination,
                                            pending.operation,
                                            () -> false));
                }
                if (version == 5)
                    assertEquals(
                            2,
                            ledger.checkpoint(
                                            new DurableDelivery.Operation("release-fixture/v5", 9))
                                    .lastId);
            }
        }
        assertEquals(14, events(destination));
    }

    @Test
    public void migrationFailureRollsBackThenRetries() {
        String name = database(2) + "-fault";
        try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
            try {
                ledger.getWritableDatabase();
                fail("Fault must abort migration");
            } catch (SQLiteException expected) {
            }
        }
        try (SQLiteDatabase db = context().openOrCreateDatabase(name, 0, null)) {
            assertEquals(2, db.getVersion());
            try (Cursor columns = db.rawQuery("PRAGMA table_info(outbox)", null)) {
                while (columns.moveToNext())
                    assertNotEquals("operation_owner", columns.getString(1));
            }
            db.execSQL("DROP TRIGGER release_migration_fault");
        }
        try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
            assertEquals(6, ledger.getWritableDatabase().getVersion());
            assertNotNull(ledger.outbox().get(record(2, 4).key(), calendar()).payload);
        }
    }

    @Test
    public void unsupportedNewerSchemaIsPreserved() {
        try (SqliteLedger ledger = new SqliteLedger(context(), database(7))) {
            try {
                ledger.getWritableDatabase();
                fail("No blind schema downgrade");
            } catch (SQLiteException expected) {
            }
        }
        try (SQLiteDatabase db = context().openOrCreateDatabase(database(7), 0, null);
                Cursor row = db.rawQuery("SELECT value FROM sentinel", null)) {
            assertEquals(7, db.getVersion());
            assertTrue(row.moveToFirst());
            assertEquals("must survive unsupported downgrade", row.getString(0));
        }
    }

    @Test
    public void deniedPermissionCannotReplayAfterUpgrade() {
        AndroidCalendarSink sink = new AndroidCalendarSink(context());
        for (int version = 2; version <= 5; version++) {
            try (SqliteLedger ledger = new SqliteLedger(context(), database(version))) {
                DurableDelivery.Intent pending =
                        ledger.outbox().get(record(version, 4).key(), calendar());
                assertEquals(
                        ReceiptRecovery.Result.PERMISSION_REQUIRED,
                        recovery(ledger, sink).reconcile(record(version, 2).key(), calendar()));
                assertEquals(
                        DurableDelivery.Result.PERMISSION_REQUIRED,
                        new DurableDelivery(ledger.outbox(), sink)
                                .resume(pending.key, calendar(), pending.operation, () -> true));
                assertEquals(
                        DurableDelivery.State.READY,
                        ledger.outbox().get(pending.key, calendar()).state);
            }
        }
    }

    @Test
    public void explicitPendingResumeIsExactlyOnce() {
        AndroidCalendarSink sink = new AndroidCalendarSink(context());
        assertEquals(14, events(calendar()));
        for (int version = 2; version <= 5; version++) {
            try (SqliteLedger ledger = new SqliteLedger(context(), database(version))) {
                DurableDelivery.Intent pending =
                        ledger.outbox().get(record(version, 4).key(), calendar());
                DurableDelivery delivery = new DurableDelivery(ledger.outbox(), sink);
                assertEquals(
                        DurableDelivery.Result.SAVED,
                        delivery.resume(pending.key, calendar(), pending.operation, () -> true));
                assertEquals(
                        DurableDelivery.Result.ALREADY_LOGGED,
                        delivery.resume(pending.key, calendar(), pending.operation, () -> true));
            }
        }
        assertEquals(18, events(calendar()));
    }

    @Test
    public void removedDestinationCannotRetargetOrEraseReceipts() {
        assertEquals(0, events(calendar()));
        AndroidCalendarSink sink = new AndroidCalendarSink(context());
        for (int version = 2; version <= 5; version++) {
            try (SqliteLedger ledger = new SqliteLedger(context(), database(version))) {
                DurableDelivery.Intent saved =
                        ledger.outbox().get(record(version, 4).key(), calendar());
                assertEquals(
                        DurableDelivery.Result.DESTINATION_UNAVAILABLE,
                        new DurableDelivery(ledger.outbox(), sink)
                                .resume(saved.key, calendar(), saved.operation, () -> true));
                assertEquals(
                        DurableDelivery.State.CONFIRMED,
                        ledger.outbox().get(saved.key, calendar()).state);
            }
        }
    }
}
