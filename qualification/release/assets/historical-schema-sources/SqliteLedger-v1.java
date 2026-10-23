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
        super(context, name, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE receipts (source TEXT NOT NULL, calendar INTEGER NOT NULL, event"
                        + " INTEGER NOT NULL, fingerprint TEXT NOT NULL, PRIMARY"
                        + " KEY(source,calendar))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("A preserving ledger migration is required");
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
}
