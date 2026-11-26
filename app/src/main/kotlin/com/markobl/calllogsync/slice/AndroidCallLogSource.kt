package com.markobl.calllogsync.slice

import android.Manifest
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.CallLog.Calls

/** Call-only adapter; queries occur only on explicit coordinator requests. */
class AndroidCallLogSource
private constructor(context: Context?, includeAppCalls: Boolean, app: String?) :
    SliceEngine.CallSource, PagedCalls.Source {
    @JvmOverloads
    constructor(
        context: Context?,
        includeAppCalls: Boolean = false,
    ) : this(context, includeAppCalls, null)

    private val appComponentPattern = app?.let {
        require(CallRecord.isAppSource(it)) { "Invalid import app package" }
        "$it/*"
    }
    private val context = context!!.applicationContext
    private val uri = if (includeAppCalls) Calls.CONTENT_URI_WITH_VOIP_CALLS else Calls.CONTENT_URI
    private val projection =
        if (includeAppCalls) {
            PROJECTION + arrayOf(Calls.UUID, Calls.PHONE_ACCOUNT_COMPONENT_NAME)
        } else {
            PROJECTION
        }

    override fun latest(): CallRecord? {
        permission()
        val query = Bundle()
        // The platform's structured sort can apply DESC only to the last column.
        query.putString(
            ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
            "${Calls.DATE} DESC, ${Calls._ID} DESC",
        )
        query.putInt(ContentResolver.QUERY_ARG_LIMIT, 1)
        context.contentResolver
            .query(
                uri.buildUpon().appendQueryParameter(Calls.LIMIT_PARAM_KEY, "1").build(),
                projection,
                query,
                null,
            )
            .use { cursor ->
                checkNotNull(cursor) { "Call provider unavailable" }
                return first(cursor, namespace())
            }
    }

    override fun namespace(): String = AndroidSettingsStore(context).namespace()

    internal fun observedAppPackages(): Set<String> {
        permission()
        val query = pageQuery("${Calls.UUID} IS NOT NULL", null, false, PagedCalls.MAX_PAGE_SIZE)
        return context.contentResolver
            .query(
                Calls.CONTENT_URI_WITH_VOIP_CALLS.buildUpon()
                    .appendQueryParameter(
                        Calls.LIMIT_PARAM_KEY,
                        PagedCalls.MAX_PAGE_SIZE.toString(),
                    )
                    .build(),
                arrayOf(Calls.PHONE_ACCOUNT_COMPONENT_NAME),
                query,
                null,
            )
            .use { cursor ->
                checkNotNull(cursor) { "Call provider unavailable" }
                buildSet {
                    while (cursor.moveToNext()) appPackage(cursor.getString(0))?.let(::add)
                }
            }
    }

    override fun upper(): CallRecord? = bounded(null, null, false, 1).firstOrNull()

    override fun at(id: Long): CallRecord? {
        require(id >= 0) { "Invalid source ID" }
        return bounded(Calls._ID + "=?", arrayOf(id.toString()), true, 1).firstOrNull()
    }

    override fun page(afterExclusive: Long, upperInclusive: Long, limit: Int): List<CallRecord> {
        require(
            afterExclusive >= -1 &&
                upperInclusive >= 0 &&
                afterExclusive < upperInclusive &&
                limit in 1..PagedCalls.MAX_PAGE_SIZE,
        ) {
            "Invalid source page"
        }
        return bounded(
            Calls._ID + ">? AND " + Calls._ID + "<=?",
            arrayOf(afterExclusive.toString(), upperInclusive.toString()),
            true,
            limit,
        )
    }

    private fun permission() {
        if (
            context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Call-log permission required")
        }
    }

    private fun bounded(
        selection: String?,
        args: Array<String?>?,
        ascending: Boolean,
        limit: Int,
    ): List<CallRecord> {
        permission()
        val pattern = appComponentPattern
        val scopedSelection =
            if (pattern == null) {
                selection
            } else {
                listOfNotNull(
                        selection,
                        "${Calls.UUID} IS NOT NULL AND ${Calls.PHONE_ACCOUNT_COMPONENT_NAME} GLOB ?",
                    )
                    .joinToString(" AND ")
            }
        val scopedArgs = if (pattern == null) args else arrayOf(*args.orEmpty(), pattern)
        context.contentResolver
            .query(
                uri.buildUpon()
                    .appendQueryParameter(Calls.LIMIT_PARAM_KEY, limit.toString())
                    .build(),
                projection,
                pageQuery(scopedSelection, scopedArgs, ascending, limit),
                null,
            )
            .use { cursor ->
                checkNotNull(cursor) { "Call provider unavailable" }
                return pageRows(cursor, namespace(), limit)
            }
    }

    companion object {
        internal fun forApp(context: Context, app: String): PagedCalls.Source =
            AndroidCallLogSource(context, true, app)

        @JvmField
        val PROJECTION =
            arrayOf(
                Calls._ID,
                Calls.NUMBER,
                Calls.TYPE,
                Calls.DATE,
                Calls.DURATION,
                Calls.CACHED_NAME,
                Calls.NUMBER_PRESENTATION,
                Calls.PHONE_ACCOUNT_ID,
            )

        @JvmStatic
        fun pageQuery(
            selection: String?,
            args: Array<String?>?,
            ascending: Boolean,
            limit: Int,
        ): Bundle {
            require(limit in 1..PagedCalls.MAX_PAGE_SIZE) { "Invalid page limit" }
            val query = Bundle()
            query.putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(Calls._ID))
            query.putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                if (ascending) {
                    ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
                } else {
                    ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                },
            )
            query.putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            if (selection != null) {
                query.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                query.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, args)
            }
            return query
        }

        @JvmStatic
        fun pageRows(cursor: Cursor?, namespace: String?, limit: Int): List<CallRecord> {
            require(limit in 1..PagedCalls.MAX_PAGE_SIZE) { "Invalid page limit" }
            val rows = ArrayList<CallRecord>()
            while (cursor!!.moveToNext()) {
                if (rows.size == limit) throw PagedCalls.SourceChanged()
                rows.add(current(cursor, namespace))
            }
            return rows
        }

        @JvmStatic
        fun first(cursor: Cursor?, namespace: String?): CallRecord? =
            if (!cursor!!.moveToFirst()) null else current(cursor, namespace)

        internal fun current(cursor: Cursor, namespace: String?): CallRecord {
            val appMarker = cursor.getColumnIndex(Calls.UUID)
            val source =
                if (appMarker < 0 || cursor.isNull(appMarker)) {
                    CallRecord.PHONE
                } else {
                    appPackage(
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(Calls.PHONE_ACCOUNT_COMPONENT_NAME),
                        ),
                    )
                }
            val presentation = cursor.getColumnIndexOrThrow(Calls.NUMBER_PRESENTATION)
            return CallRecord(
                namespace,
                cursor.getString(cursor.getColumnIndexOrThrow(Calls._ID)),
                cursor.getLong(cursor.getColumnIndexOrThrow(Calls.DATE)),
                cursor.getLong(cursor.getColumnIndexOrThrow(Calls.DURATION)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Calls.TYPE)),
                cursor.getString(cursor.getColumnIndexOrThrow(Calls.CACHED_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(Calls.NUMBER)),
                if (cursor.isNull(presentation)) null else cursor.getInt(presentation),
                cursor.getString(cursor.getColumnIndexOrThrow(Calls.PHONE_ACCOUNT_ID)),
                source,
            )
        }

        internal fun appPackage(component: String?): String? =
            component?.let(ComponentName::unflattenFromString)?.packageName?.takeIf {
                it.isNotBlank()
            }
    }
}
