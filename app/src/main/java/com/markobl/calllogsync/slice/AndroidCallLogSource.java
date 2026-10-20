package com.markobl.calllogsync.slice;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CallLog.Calls;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Call-only provider adapter. Queries occur only when explicitly requested by a coordinator. */
public final class AndroidCallLogSource implements SliceEngine.CallSource, PagedCalls.Source {
    static final String[] PROJECTION = {
        Calls._ID,
        Calls.NUMBER,
        Calls.TYPE,
        Calls.DATE,
        Calls.DURATION,
        Calls.CACHED_NAME,
        Calls.NUMBER_PRESENTATION,
        Calls.PHONE_ACCOUNT_ID
    };
    private final Context context;

    public AndroidCallLogSource(Context context) {
        this.context = context.getApplicationContext();
    }

    private static synchronized String namespace(Context context) {
        SharedPreferences p = context.getSharedPreferences("slice", Context.MODE_PRIVATE);
        String id = p.getString("sourceNamespace", null);
        if (id == null) {
            id = "android-call-log/" + UUID.randomUUID();
            if (!p.edit().putString("sourceNamespace", id).commit())
                throw new IllegalStateException("Identity not persisted");
        }
        return id;
    }

    @Override
    public CallRecord latest() {
        if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED)
            throw new SecurityException("Call-log permission required");
        Bundle query = new Bundle();
        query.putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS, new String[] {Calls.DATE, Calls._ID});
        query.putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING);
        query.putInt(ContentResolver.QUERY_ARG_LIMIT, 1);
        try (Cursor c =
                context.getContentResolver()
                        .query(
                                Calls.CONTENT_URI
                                        .buildUpon()
                                        .appendQueryParameter(Calls.LIMIT_PARAM_KEY, "1")
                                        .build(),
                                PROJECTION,
                                query,
                                null)) {
            if (c == null) throw new IllegalStateException("Call provider unavailable");
            return first(c, namespace(context));
        }
    }

    @Override
    public String namespace() {
        return namespace(context);
    }

    @Override
    public CallRecord upper() {
        List<CallRecord> rows = bounded(null, null, false, 1);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public CallRecord at(long id) {
        if (id < 0) throw new IllegalArgumentException("Invalid source ID");
        List<CallRecord> rows =
                bounded(Calls._ID + "=?", new String[] {Long.toString(id)}, true, 1);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public List<CallRecord> page(long after, long upper, int limit) {
        if (after < -1 || upper < 0 || after >= upper || limit < 1 || limit > 500)
            throw new IllegalArgumentException("Invalid source page");
        return bounded(
                Calls._ID + ">? AND " + Calls._ID + "<=?",
                new String[] {Long.toString(after), Long.toString(upper)},
                true,
                limit);
    }

    static Bundle pageQuery(String selection, String[] args, boolean ascending, int limit) {
        if (limit < 1 || limit > 500) throw new IllegalArgumentException("Invalid page limit");
        Bundle query = new Bundle();
        query.putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, new String[] {Calls._ID});
        query.putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ascending
                        ? ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
                        : ContentResolver.QUERY_SORT_DIRECTION_DESCENDING);
        query.putInt(ContentResolver.QUERY_ARG_LIMIT, limit);
        if (selection != null) {
            query.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection);
            query.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, args);
        }
        return query;
    }

    private List<CallRecord> bounded(
            String selection, String[] args, boolean ascending, int limit) {
        if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED)
            throw new SecurityException("Call-log permission required");
        try (Cursor cursor =
                context.getContentResolver()
                        .query(
                                Calls.CONTENT_URI
                                        .buildUpon()
                                        .appendQueryParameter(
                                                Calls.LIMIT_PARAM_KEY, Integer.toString(limit))
                                        .build(),
                                PROJECTION,
                                pageQuery(selection, args, ascending, limit),
                                null)) {
            if (cursor == null) throw new IllegalStateException("Call provider unavailable");
            return pageRows(cursor, namespace(), limit);
        }
    }

    static List<CallRecord> pageRows(Cursor cursor, String namespace, int limit) {
        if (limit < 1 || limit > 500) throw new IllegalArgumentException("Invalid page limit");
        List<CallRecord> rows = new ArrayList<>();
        while (cursor.moveToNext()) {
            if (rows.size() == limit) throw new PagedCalls.SourceChanged();
            rows.add(current(cursor, namespace));
        }
        return rows;
    }

    static CallRecord first(Cursor c, String namespace) {
        if (!c.moveToFirst()) return null;
        return current(c, namespace);
    }

    private static CallRecord current(Cursor c, String namespace) {
        return new CallRecord(
                namespace,
                c.getString(0),
                c.getLong(3),
                c.getLong(4),
                c.getInt(2),
                c.getString(5),
                c.getString(1),
                c.isNull(6) ? (Integer) null : Integer.valueOf(c.getInt(6)),
                c.getString(7));
    }
}
