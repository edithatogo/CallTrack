package com.markobl.calllogsync.slice;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Separate versioned import-session store; delivery receipts remain in the existing ledger. */
public final class SqliteImportStore extends SQLiteOpenHelper implements HistoricalImport.Store {
    public SqliteImportStore(Context context) {
        this(context, "calltrack-imports.db");
    }

    public SqliteImportStore(Context context, String name) {
        super(context, name, null, 1);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE sessions (id TEXT PRIMARY KEY NOT NULL, revision INTEGER NOT NULL,"
                        + " body TEXT NOT NULL)");
        db.execSQL(
                "CREATE TABLE candidates (session TEXT NOT NULL REFERENCES sessions(id), source_id"
                    + " INTEGER NOT NULL, source_key TEXT NOT NULL, binding TEXT NOT NULL, PRIMARY"
                    + " KEY(session,source_id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Preserving import migration required");
    }

    @Override
    public void create(HistoricalImport.Session session) {
        if (session.phase != HistoricalImport.Phase.BUILDING
                || session.revision != 0
                || session.count != 0
                || session.processedId != -1
                || session.saved != 0
                || session.review != 0
                || session.checkpoint.lastId != -1)
            throw new IllegalArgumentException("Invalid initial import");
        ContentValues values = new ContentValues();
        values.put("id", session.id);
        values.put("revision", 0);
        values.put("body", encode(session));
        SQLiteDatabase db = getWritableDatabase();
        if (db.inTransaction())
            throw new IllegalStateException("Import creation must commit independently");
        db.insertOrThrow("sessions", null, values);
    }

    @Override
    public HistoricalImport.Session get(String id) {
        try (Cursor c =
                getReadableDatabase()
                        .query(
                                "sessions",
                                new String[] {"body"},
                                "id=?",
                                new String[] {id},
                                null,
                                null,
                                null)) {
            return c.moveToFirst() ? decode(c.getString(0)) : null;
        }
    }

    @Override
    public void save(
            HistoricalImport.Session expected,
            HistoricalImport.Session next,
            List<HistoricalImport.Candidate> added) {
        validate(expected, next, added);
        SQLiteDatabase db = getWritableDatabase();
        if (db.inTransaction())
            throw new IllegalStateException("Import progress must commit independently");
        db.beginTransaction();
        try {
            HistoricalImport.Session stored = get(expected.id);
            if (stored == null || !encode(stored).equals(encode(expected)))
                throw new IllegalStateException("Import changed before publication");
            for (HistoricalImport.Candidate candidate : added) {
                ContentValues row = new ContentValues();
                row.put("session", next.id);
                row.put("source_id", candidate.sourceId);
                row.put("source_key", candidate.key);
                row.put("binding", candidate.binding);
                db.insertOrThrow("candidates", null, row);
            }
            ContentValues values = new ContentValues();
            values.put("revision", next.revision);
            values.put("body", encode(next));
            if (db.update(
                            "sessions",
                            values,
                            "id=? AND revision=?",
                            new String[] {expected.id, Long.toString(expected.revision)})
                    != 1) throw new IllegalStateException("Import progress not stored");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public List<HistoricalImport.Candidate> candidates(String id, long after, int limit) {
        if (after < -1 || limit < 1 || limit > 500)
            throw new IllegalArgumentException("Invalid candidate page");
        List<HistoricalImport.Candidate> rows = new ArrayList<>();
        try (Cursor c =
                getReadableDatabase()
                        .query(
                                "candidates",
                                new String[] {"source_id", "source_key", "binding"},
                                "session=? AND source_id>?",
                                new String[] {id, Long.toString(after)},
                                null,
                                null,
                                "source_id ASC",
                                Integer.toString(limit))) {
            while (c.moveToNext())
                rows.add(
                        new HistoricalImport.Candidate(
                                c.getLong(0), c.getString(1), c.getString(2)));
        }
        return java.util.Collections.unmodifiableList(rows);
    }

    private static void validate(
            HistoricalImport.Session old,
            HistoricalImport.Session next,
            List<HistoricalImport.Candidate> added) {
        if (old == null
                || next == null
                || added == null
                || added.size() > 500
                || !old.id.equals(next.id)
                || next.revision != Math.addExact(old.revision, 1)
                || old.calendar != next.calendar
                || !old.timezone.equals(next.timezone)
                || old.from != next.from
                || old.to != next.to
                || old.incoming != next.incoming
                || old.outgoing != next.outgoing
                || old.missed != next.missed
                || !old.checkpoint.namespace.equals(next.checkpoint.namespace)
                || old.checkpoint.upperId != next.checkpoint.upperId
                || !Objects.equals(old.checkpoint.upperBinding, next.checkpoint.upperBinding)
                || next.checkpoint.lastId < old.checkpoint.lastId
                || next.processedId < old.processedId
                || next.saved < old.saved
                || next.review < old.review
                || next.count != Math.addExact(old.count, added.size()))
            throw new IllegalArgumentException("Invalid import transition");
        boolean permitted = false;
        switch (old.phase) {
            case BUILDING:
                permitted =
                        next.phase == HistoricalImport.Phase.BUILDING
                                || next.phase == HistoricalImport.Phase.PREVIEW
                                || next.phase == HistoricalImport.Phase.REVIEW
                                || next.phase == HistoricalImport.Phase.CANCELLED;
                break;
            case PREVIEW:
                permitted =
                        next.phase == HistoricalImport.Phase.CONFIRMED
                                || next.phase == HistoricalImport.Phase.COMPLETE
                                || next.phase == HistoricalImport.Phase.CANCELLED;
                break;
            case CONFIRMED:
                permitted =
                        next.phase == HistoricalImport.Phase.CONFIRMED
                                || next.phase == HistoricalImport.Phase.COMPLETE
                                || next.phase == HistoricalImport.Phase.CANCELLED;
                break;
            case REVIEW:
                permitted = next.phase == HistoricalImport.Phase.CANCELLED;
                break;
            default:
                break;
        }
        if (!permitted
                || old.phase != HistoricalImport.Phase.BUILDING
                        && (!added.isEmpty() || !PagedCalls.equal(old.checkpoint, next.checkpoint)))
            throw new IllegalArgumentException("Import phase cannot change this way");
        if (next.phase == HistoricalImport.Phase.PREVIEW
                        && next.checkpoint.lastId != next.checkpoint.upperId
                || next.phase == HistoricalImport.Phase.COMPLETE
                        && next.saved + next.review != next.count)
            throw new IllegalArgumentException("Import is incomplete");
        long oldDone = old.saved + old.review, nextDone = next.saved + next.review;
        if (old.phase != HistoricalImport.Phase.CONFIRMED
                || next.phase == HistoricalImport.Phase.CANCELLED) {
            if (next.processedId != old.processedId || nextDone != oldDone)
                throw new IllegalArgumentException("Import progress requires confirmation");
        } else if (next.processedId == old.processedId
                ? nextDone != oldDone
                : nextDone != Math.addExact(oldDone, 1)) {
            throw new IllegalArgumentException("Import progress must handle one candidate");
        }
        long previous = old.checkpoint.lastId;
        for (HistoricalImport.Candidate candidate : added) {
            if (candidate.sourceId <= previous || candidate.sourceId > next.checkpoint.lastId)
                throw new IllegalArgumentException("Invalid candidate order");
            previous = candidate.sourceId;
        }
        if (next.checkpoint.lastId == old.checkpoint.lastId
                && !Objects.equals(next.checkpoint.lastBinding, old.checkpoint.lastBinding))
            throw new IllegalArgumentException("Import anchor cannot be rebound");
    }

    private static String encode(HistoricalImport.Session s) {
        try {
            JSONObject j = new JSONObject();
            j.put("version", 1);
            j.put("id", s.id);
            j.put("calendar", s.calendar);
            j.put("timezone", s.timezone);
            j.put("from", s.from);
            j.put("to", s.to);
            j.put("incoming", s.incoming);
            j.put("outgoing", s.outgoing);
            j.put("missed", s.missed);
            j.put("phase", s.phase.name());
            j.put("revision", s.revision);
            j.put("count", s.count);
            j.put("processed", s.processedId);
            j.put("saved", s.saved);
            j.put("review", s.review);
            j.put("stopped", s.stopped == null ? JSONObject.NULL : s.stopped.name());
            j.put("namespace", s.checkpoint.namespace);
            j.put("upper", s.checkpoint.upperId);
            j.put(
                    "upperBinding",
                    s.checkpoint.upperBinding == null
                            ? JSONObject.NULL
                            : s.checkpoint.upperBinding);
            j.put("last", s.checkpoint.lastId);
            j.put(
                    "lastBinding",
                    s.checkpoint.lastBinding == null ? JSONObject.NULL : s.checkpoint.lastBinding);
            return j.toString();
        } catch (JSONException invalid) {
            throw new IllegalStateException("Import encoding failed", invalid);
        }
    }

    private static HistoricalImport.Session decode(String body) {
        try {
            JSONObject j = new JSONObject(body);
            if (j.getInt("version") != 1)
                throw new IllegalStateException("Import document migration required");
            return new HistoricalImport.Session(
                    j.getString("id"),
                    j.getLong("calendar"),
                    j.getString("timezone"),
                    j.getLong("from"),
                    j.getLong("to"),
                    j.getBoolean("incoming"),
                    j.getBoolean("outgoing"),
                    j.getBoolean("missed"),
                    HistoricalImport.Phase.valueOf(j.getString("phase")),
                    new PagedCalls.Checkpoint(
                            j.getString("namespace"),
                            j.getLong("upper"),
                            j.isNull("upperBinding") ? null : j.getString("upperBinding"),
                            j.getLong("last"),
                            j.isNull("lastBinding") ? null : j.getString("lastBinding")),
                    j.getLong("revision"),
                    j.getLong("count"),
                    j.getLong("processed"),
                    j.getLong("saved"),
                    j.getLong("review"),
                    j.isNull("stopped")
                            ? null
                            : DurableDelivery.Result.valueOf(j.getString("stopped")));
        } catch (JSONException | IllegalArgumentException invalid) {
            throw new IllegalStateException("Import document unavailable", invalid);
        }
    }
}
