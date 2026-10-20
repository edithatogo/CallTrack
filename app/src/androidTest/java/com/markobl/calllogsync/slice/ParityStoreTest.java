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
                assertEquals(2, ledger.getReadableDatabase().getVersion());
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
}
