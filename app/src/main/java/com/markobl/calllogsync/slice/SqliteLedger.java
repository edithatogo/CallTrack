package com.markobl.calllogsync.slice;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Durable receipt per source/destination. A writer transaction serializes concurrent instances. */
public final class SqliteLedger extends SQLiteOpenHelper implements SliceEngine.Ledger {
    public SqliteLedger(Context context) {
        this(context, "calltrack-ledger.db");
    }

    public SqliteLedger(Context context, String name) {
        super(context, name, null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE receipts (source TEXT NOT NULL, calendar INTEGER NOT NULL, event"
                        + " INTEGER NOT NULL, fingerprint TEXT NOT NULL, PRIMARY"
                        + " KEY(source,calendar))");
        createOutbox(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != 1 || newVersion != 2)
            throw new IllegalStateException("A preserving ledger migration is required");
        createOutbox(db);
        db.execSQL(
                "INSERT INTO outbox (source,calendar,state,event,fingerprint) SELECT"
                    + " source,calendar,'LEGACY',event,fingerprint FROM receipts");
    }

    @Override
    public SliceEngine.Result transaction(SliceEngine.Work work) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            SliceEngine.Result result = work.run();
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public SliceEngine.Entry get(String key, long calendar) {
        try (Cursor c =
                getReadableDatabase()
                        .query(
                                "receipts",
                                new String[] {"event", "fingerprint"},
                                "source=? AND calendar=?",
                                new String[] {key, Long.toString(calendar)},
                                null,
                                null,
                                null)) {
            return c.moveToFirst() ? new SliceEngine.Entry(c.getLong(0), c.getString(1)) : null;
        }
    }

    @Override
    public void put(String key, long calendar, SliceEngine.Entry entry) {
        ContentValues v = new ContentValues();
        v.put("source", key);
        v.put("calendar", calendar);
        v.put("event", entry.id);
        v.put("fingerprint", entry.fingerprint);
        getWritableDatabase().insertOrThrow("receipts", null, v);
    }

    private static void createOutbox(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE outbox (source TEXT NOT NULL, calendar INTEGER NOT NULL, state TEXT"
                    + " NOT NULL, event INTEGER NOT NULL, signature TEXT, marker TEXT, fingerprint"
                    + " TEXT NOT NULL, payload_title TEXT, payload_description TEXT, payload_start"
                    + " INTEGER, payload_end INTEGER, payload_timezone TEXT, PRIMARY"
                    + " KEY(source,calendar))");
        db.execSQL("CREATE INDEX outbox_state ON outbox(calendar,state)");
    }

    /** Each write is its own committed transition; never wrap delivery in transaction(). */
    public DurableDelivery.Store outbox() {
        return new DurableDelivery.Store() {
            @Override
            public DurableDelivery.Intent get(String key, long calendar) {
                return readIntent(key, calendar);
            }

            @Override
            public void put(DurableDelivery.Intent intent) {
                writeIntent(intent);
            }
        };
    }

    private DurableDelivery.Intent readIntent(String key, long calendar) {
        try (Cursor c =
                getReadableDatabase()
                        .query(
                                "outbox",
                                new String[] {
                                    "state",
                                    "event",
                                    "signature",
                                    "marker",
                                    "fingerprint",
                                    "payload_title",
                                    "payload_description",
                                    "payload_start",
                                    "payload_end",
                                    "payload_timezone"
                                },
                                "source=? AND calendar=?",
                                new String[] {key, Long.toString(calendar)},
                                null,
                                null,
                                null)) {
            if (c.moveToFirst()) {
                CallEvent payload =
                        c.isNull(5)
                                ? null
                                : new CallEvent(
                                        c.getString(5),
                                        c.getString(6),
                                        c.getLong(7),
                                        c.getLong(8),
                                        c.getString(9),
                                        c.getString(3));
                return new DurableDelivery.Intent(
                        DurableDelivery.State.valueOf(c.getString(0)),
                        key,
                        calendar,
                        c.getString(2),
                        c.getString(3),
                        c.getString(4),
                        c.getLong(1),
                        payload);
            }
        }
        // Preserve receipts even if an older retained entry point wrote after schema migration.
        SliceEngine.Entry prior = get(key, calendar);
        return prior == null
                ? null
                : new DurableDelivery.Intent(
                        DurableDelivery.State.LEGACY,
                        key,
                        calendar,
                        null,
                        null,
                        prior.fingerprint,
                        prior.id,
                        null);
    }

    private void writeIntent(DurableDelivery.Intent intent) {
        SQLiteDatabase db = getWritableDatabase();
        if (db.inTransaction())
            throw new IllegalStateException("Outbox intent must commit before provider access");
        ContentValues values = new ContentValues();
        values.put("source", intent.key);
        values.put("calendar", intent.calendar);
        values.put("state", intent.state.name());
        values.put("event", intent.eventId);
        values.put("signature", intent.sourceSignature);
        values.put("marker", intent.marker);
        values.put("fingerprint", intent.fingerprint);
        CallEvent payload = intent.payload;
        if (payload != null) {
            values.put("payload_title", payload.title);
            values.put("payload_description", payload.description);
            values.put("payload_start", payload.start);
            values.put("payload_end", payload.end);
            values.put("payload_timezone", payload.timezone);
        } else {
            for (String column :
                    new String[] {
                        "payload_title",
                        "payload_description",
                        "payload_start",
                        "payload_end",
                        "payload_timezone"
                    }) values.putNull(column);
        }
        if (db.insertWithOnConflict("outbox", null, values, SQLiteDatabase.CONFLICT_REPLACE) < 0)
            throw new IllegalStateException("Outbox write was not committed");
    }
}
