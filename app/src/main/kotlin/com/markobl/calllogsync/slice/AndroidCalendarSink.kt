package com.markobl.calllogsync.slice

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.OperationApplicationException
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.RemoteException
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import java.security.MessageDigest
import org.json.JSONArray

/** Public provider fields only; never creates an account, attendee or reminder. */
class AndroidCalendarSink
@JvmOverloads
constructor(
    suppliedContext: Context?,
    private val expectedCalendars: Map<Long, String> = emptyMap(),
) : SliceEngine.CalendarSink {
    class Choice
    @JvmOverloads
    constructor(
        @JvmField val id: Long,
        @JvmField val name: String?,
        @JvmField val accountType: String?,
        @JvmField val accountName: String? = null,
        internal val binding: String? = null,
    ) : NullableCalendarLabel() {
        protected override fun nullableLabel(): String? = name
    }

    private val context = suppliedContext!!.applicationContext
    private val resolver = suppliedContext!!.contentResolver

    private fun readPermission() {
        if (
            context.checkSelfPermission(Manifest.permission.READ_CALENDAR) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Calendar read permission required")
        }
    }

    private fun permission() {
        if (
            context.checkSelfPermission(Manifest.permission.READ_CALENDAR) !=
                PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) !=
                    PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Calendar permissions required")
        }
    }

    fun choices(): List<Choice> {
        permission()
        val result = ArrayList<Choice>()
        resolver
            .query(
                Calendars.CONTENT_URI,
                arrayOf(Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME) + IDENTITY,
                WRITABLE,
                WRITABLE_ARGS,
                Calendars.CALENDAR_DISPLAY_NAME + " ASC",
            )
            .use { cursor ->
                checkNotNull(cursor) { "Calendar provider unavailable" }
                while (cursor.moveToNext()) {
                    val identity = readIdentity(cursor, 2)
                    result.add(
                        Choice(
                            cursor.getLong(0),
                            cursor.getString(1),
                            identity[1],
                            identity[0],
                            bind(identity),
                        ),
                    )
                }
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

    private fun writableIdentity(calendar: Long): List<String?>? {
        permission()
        return resolver
            .query(
                ContentUris.withAppendedId(Calendars.CONTENT_URI, calendar),
                IDENTITY,
                WRITABLE,
                WRITABLE_ARGS,
                null,
            )
            .use { cursor ->
                checkNotNull(cursor) { "Calendar provider unavailable" }
                if (cursor.moveToFirst()) readIdentity(cursor, 0) else null
            }
    }

    internal fun writableBinding(calendar: Long): String? = writableIdentity(calendar)?.let(::bind)

    override fun writable(calendar: Long): Boolean {
        val identity = writableIdentity(calendar) ?: return false
        return expectedCalendars[calendar]?.let { it == bind(identity) } ?: true
    }

    override fun insert(calendar: Long, event: CallEvent?): Long {
        val identity = checkNotNull(writableIdentity(calendar)) { "Destination unavailable" }
        val expected = expectedCalendars[calendar]
        check(expected == null || expected == bind(identity)) { "Destination changed" }
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
            if (expected == null) {
                resolver.insert(Events.CONTENT_URI, values)
            } else {
                insertBound(calendar, identity, values)
            }
        checkNotNull(uri) { "Event not inserted" }
        return ContentUris.parseId(uri)
    }

    private fun insertBound(
        calendar: Long,
        identity: List<String?>,
        values: ContentValues,
    ): android.net.Uri? {
        // The provider checks the writable identity and inserts in the same batch.
        val matches =
            IDENTITY.zip(identity).map { (field, value) ->
                if (value == null) "$field IS NULL" else "$field=?"
            }
        val assertCalendar =
            ContentProviderOperation.newAssertQuery(
                    ContentUris.withAppendedId(Calendars.CONTENT_URI, calendar),
                )
                .withSelection(
                    (listOf(WRITABLE) + matches).joinToString(" AND "),
                    WRITABLE_ARGS + identity.filterNotNull(),
                )
                .withExpectedCount(1)
                .build()
        val insertEvent =
            ContentProviderOperation.newInsert(Events.CONTENT_URI).withValues(values).build()
        return try {
            resolver
                .applyBatch(CalendarContract.AUTHORITY, arrayListOf(assertCalendar, insertEvent))[1]
                .uri
        } catch (changed: OperationApplicationException) {
            throw IllegalStateException("Calendar batch could not be confirmed", changed)
        } catch (unavailable: RemoteException) {
            throw IllegalStateException("Calendar provider unavailable", unavailable)
        }
    }

    private fun stored(cursor: Cursor): SliceEngine.StoredEvent? {
        val packageColumn = cursor.getColumnIndexOrThrow(Events.CUSTOM_APP_PACKAGE)
        if (context.packageName != cursor.getString(packageColumn)) return null
        if (
            cursor.getInt(cursor.getColumnIndexOrThrow(Events.ACCESS_LEVEL)) !=
                Events.ACCESS_PRIVATE ||
                cursor.getInt(cursor.getColumnIndexOrThrow(Events.AVAILABILITY)) !=
                    Events.AVAILABILITY_FREE ||
                cursor.getInt(cursor.getColumnIndexOrThrow(Events.HAS_ALARM)) != 0
        ) {
            return null
        }
        return SliceEngine.StoredEvent(
            cursor.getLong(cursor.getColumnIndexOrThrow(Events._ID)),
            cursor.getLong(cursor.getColumnIndexOrThrow(Events.CALENDAR_ID)),
            CallEvent(
                cursor.getString(cursor.getColumnIndexOrThrow(Events.TITLE)),
                cursor.getString(cursor.getColumnIndexOrThrow(Events.DESCRIPTION)),
                cursor.getLong(cursor.getColumnIndexOrThrow(Events.DTSTART)),
                cursor.getLong(cursor.getColumnIndexOrThrow(Events.DTEND)),
                cursor.getString(cursor.getColumnIndexOrThrow(Events.EVENT_TIMEZONE)),
                cursor.getString(cursor.getColumnIndexOrThrow(Events.CUSTOM_APP_URI)),
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
        private const val WRITABLE =
            "${Calendars.VISIBLE}=1 AND ${Calendars.SYNC_EVENTS}=1 AND ${Calendars.CALENDAR_ACCESS_LEVEL}>=?"
        private val WRITABLE_ARGS = arrayOf(Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
        private val IDENTITY =
            arrayOf(
                Calendars.ACCOUNT_NAME,
                Calendars.ACCOUNT_TYPE,
                Calendars.NAME,
                Calendars.OWNER_ACCOUNT,
            )

        internal fun readIdentity(cursor: Cursor, offset: Int): List<String?> =
            IDENTITY.indices.map { cursor.getString(offset + it) }

        internal fun bind(identity: List<String?>): String =
            MessageDigest.getInstance("SHA-256")
                .digest(JSONArray(identity).toString().toByteArray())
                .toHexString()

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
