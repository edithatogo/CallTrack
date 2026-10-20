package com.markobl.calllogsync.slice;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Durable receipt per source/destination. A writer transaction serializes concurrent instances. */
public final class SqliteLedger extends SQLiteOpenHelper
        implements SliceEngine.Ledger, AutomaticDelivery.Progress {
    public SqliteLedger(Context context) {
        this(context, "calltrack-ledger.db");
    }

    public SqliteLedger(Context context, String name) {
        super(context, name, null, 5);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE receipts (source TEXT NOT NULL, calendar INTEGER NOT NULL, event"
                        + " INTEGER NOT NULL, fingerprint TEXT NOT NULL, PRIMARY"
                        + " KEY(source,calendar))");
        createOutbox(db);
        createScans(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion != 5 || oldVersion < 1 || oldVersion > 4)
            throw new IllegalStateException("A preserving ledger migration is required");
        if (oldVersion == 1) {
            createOutbox(db);
            db.execSQL(
                    "INSERT INTO outbox (source,calendar,state,event,fingerprint) SELECT"
                            + " source,calendar,'LEGACY',event,fingerprint FROM receipts");
        } else {
            if (oldVersion == 2) {
                // Existing pending rows remain unowned, never assigned to a new activation.
                db.execSQL("ALTER TABLE outbox ADD COLUMN operation_owner TEXT");
                db.execSQL(
                        "ALTER TABLE outbox ADD COLUMN operation_generation INTEGER NOT NULL"
                                + " DEFAULT 0");
            }
            if (oldVersion < 4) {
                db.execSQL("ALTER TABLE outbox ADD COLUMN source_start INTEGER");
                db.execSQL("ALTER TABLE outbox ADD COLUMN source_seconds INTEGER");
                db.execSQL("ALTER TABLE outbox ADD COLUMN source_type INTEGER");
                db.execSQL("ALTER TABLE outbox ADD COLUMN source_presentation INTEGER");
                db.execSQL("ALTER TABLE outbox ADD COLUMN source_presentation_supplied INTEGER");
            }
        }
        createScans(db);
    }

    private static void createScans(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE scan_cursors (owner TEXT NOT NULL, generation INTEGER NOT NULL,"
                        + " namespace TEXT NOT NULL, upper_id INTEGER NOT NULL, upper_binding TEXT,"
                        + " last_id INTEGER NOT NULL, last_binding TEXT, PRIMARY"
                        + " KEY(owner,generation))");
    }

    public DeliveryCounts counts() {
        long pending = 0, verified = 0, review = 0, suppressed = 0;
        try (Cursor cursor =
                getReadableDatabase()
                        .rawQuery(
                                "SELECT state,COUNT(*) FROM (SELECT state FROM outbox UNION ALL"
                                    + " SELECT 'LEGACY' AS state FROM receipts r WHERE NOT EXISTS"
                                    + " (SELECT 1 FROM outbox o WHERE o.source=r.source AND"
                                    + " o.calendar=r.calendar)) GROUP BY state",
                                null)) {
            while (cursor.moveToNext()) {
                long count = cursor.getLong(1);
                switch (DurableDelivery.State.valueOf(cursor.getString(0))) {
                    case READY:
                    case IN_FLIGHT:
                    case ATTEMPTED:
                        pending = Math.addExact(pending, count);
                        break;
                    case CONFIRMED:
                        verified = Math.addExact(verified, count);
                        break;
                    case REVIEW:
                    case LEGACY:
                        review = Math.addExact(review, count);
                        break;
                    case SUPPRESSED:
                        suppressed = Math.addExact(suppressed, count);
                        break;
                }
            }
        }
        return new DeliveryCounts(pending, verified, review, suppressed);
    }

    /** Bounded source-independent recovery, restricted to this owner/generation/destination. */
    @Override
    public java.util.List<String> pending(
            DurableDelivery.Operation operation, long calendar, int limit) {
        if (operation == null || calendar < 0 || limit < 1 || limit > 500)
            throw new IllegalArgumentException("Invalid pending query");
        java.util.List<String> keys = new java.util.ArrayList<>();
        try (Cursor cursor =
                getReadableDatabase()
                        .query(
                                "outbox",
                                new String[] {"source"},
                                "calendar=? AND operation_owner=? AND operation_generation=? AND"
                                        + " state IN (?,?,?)",
                                new String[] {
                                    Long.toString(calendar),
                                    operation.owner,
                                    Long.toString(operation.generation),
                                    "READY",
                                    "IN_FLIGHT",
                                    "ATTEMPTED"
                                },
                                null,
                                null,
                                "source ASC",
                                Integer.toString(limit))) {
            while (cursor.moveToNext()) keys.add(cursor.getString(0));
        }
        return java.util.Collections.unmodifiableList(keys);
    }

    @Override
    public PagedCalls.Checkpoint get(DurableDelivery.Operation operation) {
        return checkpoint(operation);
    }

    @Override
    public void put(DurableDelivery.Operation operation, PagedCalls.Checkpoint checkpoint) {
        checkpoint(operation, checkpoint);
    }

    public PagedCalls.Checkpoint checkpoint(DurableDelivery.Operation operation) {
        if (operation == null) throw new IllegalArgumentException("Operation required");
        try (Cursor cursor =
                getReadableDatabase()
                        .query(
                                "scan_cursors",
                                new String[] {
                                    "namespace",
                                    "upper_id",
                                    "upper_binding",
                                    "last_id",
                                    "last_binding"
                                },
                                "owner=? AND generation=?",
                                new String[] {operation.owner, Long.toString(operation.generation)},
                                null,
                                null,
                                null)) {
            return !cursor.moveToFirst()
                    ? null
                    : new PagedCalls.Checkpoint(
                            cursor.getString(0),
                            cursor.getLong(1),
                            cursor.getString(2),
                            cursor.getLong(3),
                            cursor.getString(4));
        }
    }

    /**
     * Commit after handling a row, never in the provider-effect transaction. Snapshot is immutable.
     */
    public void checkpoint(DurableDelivery.Operation operation, PagedCalls.Checkpoint next) {
        if (operation == null || next == null)
            throw new IllegalArgumentException("Checkpoint required");
        SQLiteDatabase db = getWritableDatabase();
        if (db.inTransaction())
            throw new IllegalStateException("Checkpoint must commit independently");
        db.beginTransaction();
        try {
            PagedCalls.Checkpoint prior = checkpoint(operation);
            if (prior != null
                    && (!prior.namespace.equals(next.namespace)
                            || prior.upperId != next.upperId
                            || !java.util.Objects.equals(prior.upperBinding, next.upperBinding)
                            || next.lastId < prior.lastId
                            || next.lastId == prior.lastId
                                    && !java.util.Objects.equals(
                                            prior.lastBinding, next.lastBinding)))
                throw new IllegalStateException("Frozen checkpoint cannot be reset or rebound");
            ContentValues values = new ContentValues();
            values.put("owner", operation.owner);
            values.put("generation", operation.generation);
            values.put("namespace", next.namespace);
            values.put("upper_id", next.upperId);
            values.put("upper_binding", next.upperBinding);
            values.put("last_id", next.lastId);
            values.put("last_binding", next.lastBinding);
            if (db.insertWithOnConflict(
                            "scan_cursors", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                    < 0) throw new IllegalStateException("Checkpoint not stored");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Compare and extend a completed snapshot without weakening normal progress writes. */
    @Override
    public void rollover(
            DurableDelivery.Operation operation,
            PagedCalls.Checkpoint expected,
            PagedCalls.Checkpoint next) {
        if (operation == null
                || expected == null
                || next == null
                || expected.lastId != expected.upperId
                || !expected.namespace.equals(next.namespace)
                || next.upperId < expected.upperId
                || next.lastId != expected.lastId
                || !java.util.Objects.equals(next.lastBinding, expected.lastBinding)
                || next.upperId == expected.upperId
                        && !java.util.Objects.equals(next.upperBinding, expected.upperBinding))
            throw new IllegalArgumentException("Invalid scan rollover");
        SQLiteDatabase db = getWritableDatabase();
        if (db.inTransaction())
            throw new IllegalStateException("Rollover must commit independently");
        db.beginTransaction();
        try {
            if (!PagedCalls.equal(checkpoint(operation), expected))
                throw new IllegalStateException("Scan changed before rollover");
            ContentValues values = new ContentValues();
            values.put("upper_id", next.upperId);
            values.put("upper_binding", next.upperBinding);
            if (db.update(
                            "scan_cursors",
                            values,
                            "owner=? AND generation=?",
                            new String[] {operation.owner, Long.toString(operation.generation)})
                    != 1) throw new IllegalStateException("Rollover not stored");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
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
                    + " INTEGER, payload_end INTEGER, payload_timezone TEXT, operation_owner TEXT,"
                    + " operation_generation INTEGER NOT NULL DEFAULT 0, source_start INTEGER,"
                    + " source_seconds INTEGER, source_type INTEGER, source_presentation INTEGER,"
                    + " source_presentation_supplied INTEGER, PRIMARY KEY(source,calendar))");
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
                                    "payload_timezone",
                                    "operation_owner",
                                    "operation_generation",
                                    "source_start",
                                    "source_seconds",
                                    "source_type",
                                    "source_presentation",
                                    "source_presentation_supplied"
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
                        payload,
                        c.isNull(10)
                                ? null
                                : new DurableDelivery.Operation(c.getString(10), c.getLong(11)),
                        c.isNull(12)
                                ? null
                                : new DurableDelivery.SourceFacts(
                                        c.getLong(12),
                                        c.getLong(13),
                                        c.getInt(14),
                                        c.isNull(15) ? null : Integer.valueOf(c.getInt(15)),
                                        c.getInt(16) != 0));
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
        values.put("operation_owner", intent.operation == null ? null : intent.operation.owner);
        values.put(
                "operation_generation", intent.operation == null ? 0 : intent.operation.generation);
        DurableDelivery.SourceFacts facts = intent.facts;
        if (facts != null) {
            values.put("source_start", facts.start);
            values.put("source_seconds", facts.seconds);
            values.put("source_type", facts.type);
            values.put("source_presentation", facts.presentation);
            values.put("source_presentation_supplied", facts.presentationSupplied ? 1 : 0);
        } else {
            for (String column :
                    new String[] {
                        "source_start",
                        "source_seconds",
                        "source_type",
                        "source_presentation",
                        "source_presentation_supplied"
                    }) values.putNull(column);
        }
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
