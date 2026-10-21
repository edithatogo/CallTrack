package com.markobl.calllogsync.slice

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events

/** Public provider fields only; never creates an account, attendee or reminder. */
class AndroidCalendarSink(suppliedContext: Context?) : SliceEngine.CalendarSink {
    class Choice(
        @JvmField val id: Long,
        @JvmField val name: String?,
        @JvmField val accountType: String?,
    ) : NullableCalendarLabel() {
        protected override fun nullableLabel(): String? = name
    }

    private val context = suppliedContext!!.applicationContext
    private val resolver = suppliedContext!!.contentResolver

    private fun readPermission() {
        if (
            context.checkSelfPermission(Manifest.permission.READ_CALENDAR) !=
                PackageManager.PERMISSION_GRANTED
        )
            throw SecurityException("Calendar read permission required")
    }

    private fun permission() {
        if (
            context.checkSelfPermission(Manifest.permission.READ_CALENDAR) !=
                PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) !=
                    PackageManager.PERMISSION_GRANTED
        )
            throw SecurityException("Calendar permissions required")
    }

    fun choices(): List<Choice> {
        permission()
        val result = ArrayList<Choice>()
        resolver
            .query(
                Calendars.CONTENT_URI,
                arrayOf(Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME, Calendars.ACCOUNT_TYPE),
                Calendars.VISIBLE +
                    "=1 AND " +
                    Calendars.SYNC_EVENTS +
                    "=1 AND " +
                    Calendars.CALENDAR_ACCESS_LEVEL +
                    ">=?",
                arrayOf(Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
                Calendars.CALENDAR_DISPLAY_NAME + " ASC",
            )
            .use { cursor ->
                checkNotNull(cursor) { "Calendar provider unavailable" }
                while (cursor.moveToNext()) result.add(
                    Choice(cursor.getLong(0), cursor.getString(1), cursor.getString(2))
                )
            }
        return result
    }

    fun calendarLabel(id: Long): String? {
        readPermission()
        resolver
            .query(
                ContentUris.withAppendedId(Calendars.CONTENT_URI, id),
                arrayOf(Calendars.CALENDAR_DISPLAY_NAME),
                null,
                null,
                null,
            )
            .use { cursor ->
                checkNotNull(cursor) { "Calendar provider unavailable" }
                return if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }

    override fun writable(calendar: Long): Boolean {
        permission()
        resolver
            .query(
                ContentUris.withAppendedId(Calendars.CONTENT_URI, calendar),
                arrayOf(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.VISIBLE, Calendars.SYNC_EVENTS),
                null,
                null,
                null,
            )
            .use { cursor ->
                checkNotNull(cursor) { "Calendar provider unavailable" }
                return cursor.moveToFirst() &&
                    cursor.getInt(0) >= Calendars.CAL_ACCESS_CONTRIBUTOR &&
                    cursor.getInt(1) == 1 &&
                    cursor.getInt(2) == 1
            }
    }

    override fun insert(calendar: Long, event: CallEvent?): Long {
        check(writable(calendar)) { "Destination unavailable" }
        val values = ContentValues()
        values.put(Events.CALENDAR_ID, calendar)
        values.put(Events.TITLE, event!!.title)
        values.put(Events.DESCRIPTION, event.description)
        values.put(Events.DTSTART, event.start)
        values.put(Events.DTEND, event.end)
        values.put(Events.EVENT_TIMEZONE, event.timezone)
        values.put(Events.ACCESS_LEVEL, Events.ACCESS_PRIVATE)
        values.put(Events.AVAILABILITY, Events.AVAILABILITY_FREE)
        values.put(Events.HAS_ALARM, 0)
        values.put(Events.CUSTOM_APP_PACKAGE, context.packageName)
        values.put(Events.CUSTOM_APP_URI, event.marker)
        val uri =
            resolver.insert(Events.CONTENT_URI, values)
                ?: throw IllegalStateException("Event not inserted")
        return ContentUris.parseId(uri)
    }

    private fun stored(cursor: Cursor): SliceEngine.StoredEvent? {
        if (
            context.packageName != cursor.getString(8) ||
                cursor.getInt(9) != Events.ACCESS_PRIVATE ||
                cursor.getInt(10) != Events.AVAILABILITY_FREE ||
                cursor.getInt(11) != 0
        )
            return null
        return SliceEngine.StoredEvent(
            cursor.getLong(0),
            cursor.getLong(1),
            CallEvent(
                cursor.getString(2),
                cursor.getString(3),
                cursor.getLong(4),
                cursor.getLong(5),
                cursor.getString(6),
                cursor.getString(7),
            ),
        )
    }

    override fun read(id: Long): SliceEngine.StoredEvent? {
        readPermission()
        resolver
            .query(
                ContentUris.withAppendedId(Events.CONTENT_URI, id),
                FIELDS,
                Events.DELETED + "=0",
                null,
                null,
            )
            .use { cursor ->
                checkNotNull(cursor) { "Event provider unavailable" }
                return if (cursor.moveToFirst()) stored(cursor) else null
            }
    }

    override fun find(calendar: Long, marker: String?): List<SliceEngine.StoredEvent?> {
        readPermission()
        val result = ArrayList<SliceEngine.StoredEvent?>()
        resolver
            .query(
                Events.CONTENT_URI,
                FIELDS,
                Events.CALENDAR_ID +
                    "=? AND " +
                    Events.CUSTOM_APP_URI +
                    "=? AND " +
                    Events.CUSTOM_APP_PACKAGE +
                    "=? AND " +
                    Events.DELETED +
                    "=0",
                arrayOf(calendar.toString(), marker, context.packageName),
                null,
            )
            .use { cursor ->
                checkNotNull(cursor) { "Event provider unavailable" }
                while (result.size < 2 && cursor.moveToNext()) result.add(stored(cursor))
            }
        return result
    }

    companion object {
        private val FIELDS =
            arrayOf(
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
                Events.HAS_ALARM,
            )
    }
}
