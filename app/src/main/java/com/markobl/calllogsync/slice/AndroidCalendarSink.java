package com.markobl.calllogsync.slice;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;

import java.util.ArrayList;
import java.util.List;

/** Public provider fields only. Never creates an account, attendee or reminder. */
public final class AndroidCalendarSink implements SliceEngine.CalendarSink {
    public static final class Choice {
        public final long id;
        public final String name, accountType;

        Choice(long id, String name, String accountType) {
            this.id = id;
            this.name = name;
            this.accountType = accountType;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Context context;
    private final ContentResolver resolver;

    public AndroidCalendarSink(Context context) {
        this.context = context.getApplicationContext();
        resolver = context.getContentResolver();
    }

    private void readPermission() {
        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED)
            throw new SecurityException("Calendar read permission required");
    }

    private void permission() {
        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR)
                        != PackageManager.PERMISSION_GRANTED
                || context.checkSelfPermission(Manifest.permission.WRITE_CALENDAR)
                        != PackageManager.PERMISSION_GRANTED)
            throw new SecurityException("Calendar permissions required");
    }

    public List<Choice> choices() {
        permission();
        List<Choice> result = new ArrayList<>();
        try (Cursor c =
                resolver.query(
                        Calendars.CONTENT_URI,
                        new String[] {
                            Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME, Calendars.ACCOUNT_TYPE
                        },
                        Calendars.VISIBLE
                                + "=1 AND "
                                + Calendars.SYNC_EVENTS
                                + "=1 AND "
                                + Calendars.CALENDAR_ACCESS_LEVEL
                                + ">=?",
                        new String[] {Integer.toString(Calendars.CAL_ACCESS_CONTRIBUTOR)},
                        Calendars.CALENDAR_DISPLAY_NAME + " ASC")) {
            if (c == null) throw new IllegalStateException("Calendar provider unavailable");
            while (c.moveToNext())
                result.add(new Choice(c.getLong(0), c.getString(1), c.getString(2)));
        }
        return result;
    }

    public String calendarLabel(long id) {
        readPermission();
        try (Cursor cursor =
                resolver.query(
                        ContentUris.withAppendedId(Calendars.CONTENT_URI, id),
                        new String[] {Calendars.CALENDAR_DISPLAY_NAME},
                        null,
                        null,
                        null)) {
            if (cursor == null) throw new IllegalStateException("Calendar provider unavailable");
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        }
    }

    @Override
    public boolean writable(long id) {
        permission();
        try (Cursor c =
                resolver.query(
                        ContentUris.withAppendedId(Calendars.CONTENT_URI, id),
                        new String[] {
                            Calendars.CALENDAR_ACCESS_LEVEL,
                            Calendars.VISIBLE,
                            Calendars.SYNC_EVENTS
                        },
                        null,
                        null,
                        null)) {
            if (c == null) throw new IllegalStateException("Calendar provider unavailable");
            return c.moveToFirst()
                    && c.getInt(0) >= Calendars.CAL_ACCESS_CONTRIBUTOR
                    && c.getInt(1) == 1
                    && c.getInt(2) == 1;
        }
    }

    @Override
    public long insert(long calendar, CallEvent event) {
        if (!writable(calendar)) throw new IllegalStateException("Destination unavailable");
        ContentValues v = new ContentValues();
        v.put(Events.CALENDAR_ID, calendar);
        v.put(Events.TITLE, event.title);
        v.put(Events.DESCRIPTION, event.description);
        v.put(Events.DTSTART, event.start);
        v.put(Events.DTEND, event.end);
        v.put(Events.EVENT_TIMEZONE, event.timezone);
        v.put(Events.ACCESS_LEVEL, Events.ACCESS_PRIVATE);
        v.put(Events.AVAILABILITY, Events.AVAILABILITY_FREE);
        v.put(Events.HAS_ALARM, 0);
        v.put(Events.CUSTOM_APP_PACKAGE, context.getPackageName());
        v.put(Events.CUSTOM_APP_URI, event.marker);
        Uri uri = resolver.insert(Events.CONTENT_URI, v);
        if (uri == null) throw new IllegalStateException("Event not inserted");
        return ContentUris.parseId(uri);
    }

    private static final String[] FIELDS = {
        Events._ID,
        Events.CALENDAR_ID,
        Events.TITLE,
        Events.DESCRIPTION,
        Events.DTSTART,
        Events.DTEND,
        Events.EVENT_TIMEZONE,
        Events.CUSTOM_APP_URI,
        Events.CUSTOM_APP_PACKAGE,
        Events.ACCESS_LEVEL,
        Events.AVAILABILITY,
        Events.HAS_ALARM
    };

    private SliceEngine.StoredEvent stored(Cursor c) {
        // A modified privacy/reminder policy is also a human edit and must not be overwritten.
        if (!context.getPackageName().equals(c.getString(8))
                || c.getInt(9) != Events.ACCESS_PRIVATE
                || c.getInt(10) != Events.AVAILABILITY_FREE
                || c.getInt(11) != 0) return null;
        return new SliceEngine.StoredEvent(
                c.getLong(0),
                c.getLong(1),
                new CallEvent(
                        c.getString(2),
                        c.getString(3),
                        c.getLong(4),
                        c.getLong(5),
                        c.getString(6),
                        c.getString(7)));
    }

    @Override
    public SliceEngine.StoredEvent read(long id) {
        readPermission();
        try (Cursor c =
                resolver.query(
                        ContentUris.withAppendedId(Events.CONTENT_URI, id),
                        FIELDS,
                        Events.DELETED + "=0",
                        null,
                        null)) {
            if (c == null) throw new IllegalStateException("Event provider unavailable");
            return c.moveToFirst() ? stored(c) : null;
        }
    }

    @Override
    public List<SliceEngine.StoredEvent> find(long calendar, String marker) {
        readPermission();
        List<SliceEngine.StoredEvent> result = new ArrayList<>();
        try (Cursor c =
                resolver.query(
                        Events.CONTENT_URI,
                        FIELDS,
                        Events.CALENDAR_ID
                                + "=? AND "
                                + Events.CUSTOM_APP_URI
                                + "=? AND "
                                + Events.CUSTOM_APP_PACKAGE
                                + "=? AND "
                                + Events.DELETED
                                + "=0",
                        new String[] {Long.toString(calendar), marker, context.getPackageName()},
                        null)) {
            if (c == null) throw new IllegalStateException("Event provider unavailable");
            // More than one marker match is ambiguous; never materialise an unbounded duplicate
            // set.
            while (result.size() < 2 && c.moveToNext()) result.add(stored(c));
        }
        return result;
    }
}
