package com.markobl.calllogsync.slice;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CallLog.Calls;

import java.util.UUID;

/** Invoked only by the manual UI action. Does not read contacts or historical rows. */
public final class AndroidCallLogSource implements SliceEngine.CallSource {
    static final String[] PROJECTION = {
        Calls._ID,
        Calls.NUMBER,
        Calls.TYPE,
        Calls.DATE,
        Calls.DURATION,
        Calls.CACHED_NAME,
        Calls.NUMBER_PRESENTATION
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
                context.getContentResolver().query(Calls.CONTENT_URI, PROJECTION, query, null)) {
            if (c == null) throw new IllegalStateException("Call provider unavailable");
            return first(c, namespace(context));
        }
    }

    static CallRecord first(Cursor c, String namespace) {
        if (!c.moveToFirst()) return null;
        int type = c.getInt(2);
        String direction =
                type == Calls.INCOMING_TYPE
                        ? "Incoming"
                        : type == Calls.OUTGOING_TYPE
                                ? "Outgoing"
                                : type == Calls.MISSED_TYPE ? "Missed" : "Unknown";
        return new CallRecord(
                namespace,
                c.getString(0),
                c.getLong(3),
                c.getLong(4),
                direction,
                c.getString(5),
                c.getString(1),
                c.getInt(6) == Calls.PRESENTATION_ALLOWED);
    }
}
