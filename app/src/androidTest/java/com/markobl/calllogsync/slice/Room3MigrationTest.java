package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Synthetic files only: preserves old receipts without invoking CalendarContract. */
@RunWith(AndroidJUnit4.class)
public class Room3MigrationTest {
    @Test
    public void preservesLedgerSixAndImportTwoOnRoomUpgradeWithoutProviderEffects() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String token = UUID.randomUUID().toString();
        String ledgerFile = "room3-" + token + "-ledger.db";
        String importFile = "room3-" + token + "-imports.db";
        try {
            try (SQLiteDatabase sqlite = context.openOrCreateDatabase(ledgerFile, 0, null)) {
                sqlite.execSQL("CREATE TABLE receipts (source TEXT NOT NULL, calendar INTEGER NOT NULL, event INTEGER NOT NULL, fingerprint TEXT NOT NULL, PRIMARY KEY(source,calendar))");
                sqlite.execSQL("CREATE TABLE outbox (source TEXT NOT NULL, calendar INTEGER NOT NULL, state TEXT NOT NULL, event INTEGER NOT NULL, signature TEXT, marker TEXT, fingerprint TEXT NOT NULL, payload_title TEXT, payload_description TEXT, payload_start INTEGER, payload_end INTEGER, payload_timezone TEXT, operation_owner TEXT, operation_generation INTEGER NOT NULL DEFAULT 0, source_start INTEGER, source_seconds INTEGER, source_type INTEGER, source_presentation INTEGER, source_presentation_supplied INTEGER, payload_saved_at INTEGER, PRIMARY KEY(source,calendar))");
                sqlite.execSQL("CREATE INDEX outbox_state ON outbox(calendar,state)");
                sqlite.execSQL("CREATE TABLE scan_cursors (owner TEXT NOT NULL, generation INTEGER NOT NULL, namespace TEXT NOT NULL, upper_id INTEGER NOT NULL, upper_binding TEXT, last_id INTEGER NOT NULL, last_binding TEXT, PRIMARY KEY(owner,generation))");
                sqlite.execSQL("INSERT INTO receipts (source,calendar,event,fingerprint) VALUES ('legacy',7,42,'old-fingerprint')");
                sqlite.execSQL("INSERT INTO outbox (source,calendar,state,event,signature,marker,fingerprint,operation_owner,operation_generation,source_start,source_seconds,source_type,source_presentation_supplied,payload_saved_at) VALUES ('uncertain',7,'ATTEMPTED',-1,'signature','marker','fingerprint','automatic',1,1000,60,1,0,1000)");
                sqlite.setVersion(6);
            }
            try (SQLiteDatabase sqlite = context.openOrCreateDatabase(importFile, 0, null)) {
                sqlite.execSQL("PRAGMA foreign_keys=ON");
                sqlite.execSQL("CREATE TABLE sessions (id TEXT PRIMARY KEY NOT NULL, revision INTEGER NOT NULL, body TEXT NOT NULL)");
                sqlite.execSQL("CREATE TABLE candidates (session TEXT NOT NULL REFERENCES sessions(id), source_id INTEGER NOT NULL, source_key TEXT NOT NULL, binding TEXT NOT NULL, outcome TEXT, PRIMARY KEY(session,source_id))");
                sqlite.execSQL("INSERT INTO sessions (id,revision,body) VALUES ('session',3,'synthetic-record')");
                sqlite.execSQL("INSERT INTO candidates (session,source_id,source_key,binding,outcome) VALUES ('session',1,'key','binding','SAVED')");
                sqlite.setVersion(2);
            }
            for (int iteration = 0; iteration < 2; iteration++) {
                try (SqliteLedger ledger = new SqliteLedger(context, ledgerFile)) {
                    assertEquals(7, ledger.getReadableDatabase().getVersion());
                    assertEquals(42L, ledger.get("legacy", 7).id);
                    DurableDelivery.Intent attempted = ledger.outbox().get("uncertain", 7);
                    assertNotNull(attempted);
                    assertEquals(DurableDelivery.State.ATTEMPTED, attempted.state);
                    assertEquals(-1L, attempted.eventId);
                }
                try (SqliteImportStore imports = new SqliteImportStore(context, importFile)) {
                    assertEquals(3, imports.getReadableDatabase().getVersion());
                    try (Cursor cursor = imports.getReadableDatabase().rawQuery("SELECT revision,body FROM sessions WHERE id='session'", null)) {
                        assertTrue(cursor.moveToFirst());
                        assertEquals(3L, cursor.getLong(0));
                        assertEquals("synthetic-record", cursor.getString(1));
                    }
                    try (Cursor cursor = imports.getReadableDatabase().rawQuery("SELECT outcome FROM candidates WHERE session='session'", null)) {
                        assertTrue(cursor.moveToFirst());
                        assertEquals("SAVED", cursor.getString(0));
                    }
                }
            }
        } finally {
            assertTrue(context.deleteDatabase(ledgerFile));
            assertTrue(context.deleteDatabase(importFile));
        }
    }
}
