package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

/** Own private fixture database only; no calls, calendars or device settings are changed. */
public class ParityStoreTest {
    private Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private String database() {
        String token = InstrumentationRegistry.getArguments().getString("fixtureToken", "");
        if (!token.matches("[a-f0-9]{32}"))
            throw new IllegalArgumentException("Required fixture token");
        return "parity-ledger-" + token + ".db";
    }

    @Test
    public void preservesV1ReceiptsAndCreatesLegacyBindings() {
        String name = database();
        context().deleteDatabase(name);
        try {
            try (SQLiteDatabase db =
                    context().openOrCreateDatabase(name, Context.MODE_PRIVATE, null)) {
                db.execSQL(
                        "CREATE TABLE receipts (source TEXT NOT NULL, calendar INTEGER NOT NULL,"
                                + " event INTEGER NOT NULL, fingerprint TEXT NOT NULL, PRIMARY"
                                + " KEY(source,calendar))");
                db.execSQL("INSERT INTO receipts VALUES ('fixture-key',7,42,'old-fingerprint')");
                db.setVersion(1);
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                assertEquals(42, ledger.get("fixture-key", 7).id);
                DurableDelivery.Intent migrated = ledger.outbox().get("fixture-key", 7);
                assertEquals(DurableDelivery.State.LEGACY, migrated.state);
                assertEquals(42, migrated.eventId);
                assertEquals("old-fingerprint", migrated.fingerprint);
                assertNull(migrated.sourceSignature);
                assertEquals(3, ledger.getReadableDatabase().getVersion());
            }
        } finally {
            context().deleteDatabase(name);
        }
    }

    @Test
    public void outboxCommitsInflightAndPurgesConfirmedPayloadAcrossReopen() {
        String name = database();
        context().deleteDatabase(name);
        CallRecord call =
                new CallRecord(
                        "fixture",
                        "1",
                        1700000000123L,
                        65,
                        "Incoming",
                        "Fixture Name",
                        "0000000000",
                        true);
        CallEvent event = CallEvent.from(call, "UTC");
        try {
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                ledger.outbox()
                        .put(
                                new DurableDelivery.Intent(
                                        DurableDelivery.State.IN_FLIGHT,
                                        call.key(),
                                        7,
                                        CallIdentity.signature(call),
                                        event.marker,
                                        event.fingerprint(),
                                        -1,
                                        event));
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                DurableDelivery.Intent intent = ledger.outbox().get(call.key(), 7);
                assertEquals(DurableDelivery.State.IN_FLIGHT, intent.state);
                assertEquals(event.fingerprint(), intent.payload.fingerprint());
                ledger.outbox().put(intent.state(DurableDelivery.State.CONFIRMED, 42));
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                DurableDelivery.Intent receipt = ledger.outbox().get(call.key(), 7);
                assertEquals(42, receipt.eventId);
                assertNull(receipt.payload);
                try (Cursor cursor =
                        ledger.getReadableDatabase()
                                .rawQuery(
                                        "SELECT payload_title,payload_description FROM outbox",
                                        null)) {
                    assertTrue(cursor.moveToFirst());
                    assertTrue(cursor.isNull(0));
                    assertTrue(cursor.isNull(1));
                }
            }
        } finally {
            context().deleteDatabase(name);
        }
    }

    @Test
    public void rejectsOutboxWriteInsideAnUncommittedTransaction() {
        String name = database();
        context().deleteDatabase(name);
        try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
            CallRecord call = new CallRecord("fixture", "1", 1, 0, "Missed", null, null, false);
            CallEvent event = CallEvent.from(call, "UTC");
            ledger.transaction(
                    () -> {
                        try {
                            ledger.outbox()
                                    .put(
                                            new DurableDelivery.Intent(
                                                    DurableDelivery.State.READY,
                                                    call.key(),
                                                    7,
                                                    CallIdentity.signature(call),
                                                    event.marker,
                                                    event.fingerprint(),
                                                    -1,
                                                    event));
                            fail("Outbox boundary must not hide an uncommitted intent");
                        } catch (IllegalStateException expected) {
                            /* Required guard. */
                        }
                        return SliceEngine.Result.NO_CALL;
                    });
        } finally {
            context().deleteDatabase(name);
        }
    }

    @Test
    public void kotlinMappingRecognisesPreservedJavaReceiptWithoutReinsert() {
        String name = database();
        context().deleteDatabase(name);
        CallRecord call = new CallRecord("fixture", "1", 1234, 0, 1, "Cached", "000", true, "SIM");
        String javaFingerprint = "c19af50b5118168637d7c85bc6d34bce6739086cbc4510d416bbd2d7799539b1";
        CallEvent original = CallEvent.from(call, "UTC");
        assertEquals(javaFingerprint, original.fingerprint());
        CallEvent[] observed = {original};
        SliceEngine.CalendarSink sink =
                new SliceEngine.CalendarSink() {
                    @Override
                    public boolean writable(long calendar) {
                        return calendar == 7;
                    }

                    @Override
                    public java.util.List<SliceEngine.StoredEvent> find(
                            long calendar, String marker) {
                        throw new AssertionError("Known Java receipt must use its event ID");
                    }

                    @Override
                    public long insert(long calendar, CallEvent event) {
                        throw new AssertionError("Migration must not create a second event");
                    }

                    @Override
                    public SliceEngine.StoredEvent read(long id) {
                        assertEquals(42, id);
                        return new SliceEngine.StoredEvent(42, 7, observed[0]);
                    }
                };
        try {
            try (SQLiteDatabase db =
                    context().openOrCreateDatabase(name, Context.MODE_PRIVATE, null)) {
                db.execSQL(
                        "CREATE TABLE receipts (source TEXT NOT NULL, calendar INTEGER NOT NULL,"
                                + " event INTEGER NOT NULL, fingerprint TEXT NOT NULL, PRIMARY"
                                + " KEY(source,calendar))");
                db.execSQL(
                        "INSERT INTO receipts VALUES (?,7,42,?)",
                        new Object[] {call.key(), javaFingerprint});
                db.setVersion(1);
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                assertEquals(
                        SliceEngine.Result.ALREADY_LOGGED,
                        new SliceEngine(() -> call, sink, ledger, "UTC").run(7));
                assertEquals(javaFingerprint, ledger.get(call.key(), 7).fingerprint);
            }
            observed[0] =
                    new CallEvent(
                            "Human edit",
                            original.description,
                            original.start,
                            original.end,
                            original.timezone,
                            original.marker);
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                assertEquals(
                        SliceEngine.Result.REVIEW_REQUIRED,
                        new SliceEngine(() -> call, sink, ledger, "UTC").run(7));
                assertEquals(javaFingerprint, ledger.get(call.key(), 7).fingerprint);
                assertEquals(3, ledger.getReadableDatabase().getVersion());
            }
        } finally {
            context().deleteDatabase(name);
        }
    }

    @Test
    public void preservesV2PendingRowsWithoutInventingOperationOwnership() {
        String name = database();
        context().deleteDatabase(name);
        try {
            try (SQLiteDatabase db =
                    context().openOrCreateDatabase(name, Context.MODE_PRIVATE, null)) {
                db.execSQL(
                        "CREATE TABLE receipts (source TEXT NOT NULL, calendar INTEGER NOT NULL,"
                                + " event INTEGER NOT NULL, fingerprint TEXT NOT NULL, PRIMARY"
                                + " KEY(source,calendar))");
                db.execSQL(
                        "CREATE TABLE outbox (source TEXT NOT NULL, calendar INTEGER NOT NULL,"
                            + " state TEXT NOT NULL, event INTEGER NOT NULL, signature TEXT, marker"
                            + " TEXT, fingerprint TEXT NOT NULL, payload_title TEXT,"
                            + " payload_description TEXT, payload_start INTEGER, payload_end"
                            + " INTEGER, payload_timezone TEXT, PRIMARY KEY(source,calendar))");
                db.execSQL("CREATE INDEX outbox_state ON outbox(calendar,state)");
                db.execSQL(
                        "INSERT INTO outbox VALUES"
                                + " ('source',7,'READY',-1,'sig','marker','fp','Synthetic"
                                + " title','Synthetic description',1,2,'UTC')");
                db.setVersion(2);
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                DurableDelivery.Intent intent = ledger.outbox().get("source", 7);
                assertEquals(3, ledger.getReadableDatabase().getVersion());
                assertNull(intent.operation);
                assertEquals(DurableDelivery.State.READY, intent.state);
                assertEquals("Synthetic title", intent.payload.title);
                assertEquals("sig", intent.sourceSignature);
                assertEquals("fp", intent.fingerprint);
            }
        } finally {
            context().deleteDatabase(name);
        }
    }

    @Test
    public void operationIdentitySurvivesReopenAndPayloadPurge() {
        String name = database();
        context().deleteDatabase(name);
        try {
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                ledger.outbox()
                        .put(
                                new DurableDelivery.Intent(
                                        DurableDelivery.State.READY,
                                        "source",
                                        7,
                                        "sig",
                                        "marker",
                                        "fp",
                                        -1,
                                        new CallEvent("Synthetic", "", 1, 2, "UTC", "marker"),
                                        new DurableDelivery.Operation("import-1", 42)));
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                DurableDelivery.Intent intent = ledger.outbox().get("source", 7);
                assertEquals("import-1", intent.operation.owner);
                assertEquals(42, intent.operation.generation);
                ledger.outbox().put(intent.state(DurableDelivery.State.CONFIRMED, 123));
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                DurableDelivery.Intent intent = ledger.outbox().get("source", 7);
                assertEquals("import-1", intent.operation.owner);
                assertEquals(42, intent.operation.generation);
                assertNull(intent.payload);
                assertEquals(123, intent.eventId);
            }
        } finally {
            context().deleteDatabase(name);
        }
    }
}
