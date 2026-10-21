package com.markobl.calllogsync.slice

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.CallLog.Calls
import java.util.UUID

/** Call-only adapter; queries occur only on explicit coordinator requests. */
class AndroidCallLogSource(context: Context?) : SliceEngine.CallSource, PagedCalls.Source {
    private val context = context!!.applicationContext

    override fun latest(): CallRecord? {
        permission()
        val query = Bundle()
        query.putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(Calls.DATE, Calls._ID))
        query.putInt(
            ContentResolver.QUERY_ARG_SORT_DIRECTION,
            ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
        )
        query.putInt(ContentResolver.QUERY_ARG_LIMIT, 1)
        context.contentResolver
            .query(
                Calls.CONTENT_URI.buildUpon()
                    .appendQueryParameter(Calls.LIMIT_PARAM_KEY, "1")
                    .build(),
                PROJECTION,
                query,
                null,
            )
            .use { cursor ->
                checkNotNull(cursor) { "Call provider unavailable" }
                return first(cursor, namespace(context))
            }
    }

    override fun namespace(): String = namespace(context)

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
                limit in 1..500
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
        )
            throw SecurityException("Call-log permission required")
    }

    private fun bounded(
        selection: String?,
        args: Array<String?>?,
        ascending: Boolean,
        limit: Int,
    ): List<CallRecord> {
        permission()
        context.contentResolver
            .query(
                Calls.CONTENT_URI.buildUpon()
                    .appendQueryParameter(Calls.LIMIT_PARAM_KEY, limit.toString())
                    .build(),
                PROJECTION,
                pageQuery(selection, args, ascending, limit),
                null,
            )
            .use { cursor ->
                checkNotNull(cursor) { "Call provider unavailable" }
                return pageRows(cursor, namespace(), limit)
            }
    }

    companion object {
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
        private var namespaceCommitUncertain = false

        private fun namespace(context: Context): String =
            synchronized(AndroidCallLogSource::class.java) {
                if (namespaceCommitUncertain)
                    throw IllegalStateException(
                        "Source identity commit uncertain; restart required"
                    )
                val prefs = context.getSharedPreferences("slice", Context.MODE_PRIVATE)
                var id = prefs.getString("sourceNamespace", null)
                if (id == null) {
                    id = "android-call-log/" + UUID.randomUUID()
                    namespaceCommitUncertain = true
                    if (!prefs.edit().putString("sourceNamespace", id).commit())
                        throw IllegalStateException("Identity not persisted")
                    namespaceCommitUncertain = false
                }
                id
            }

        @JvmStatic
        fun pageQuery(
            selection: String?,
            args: Array<String?>?,
            ascending: Boolean,
            limit: Int,
        ): Bundle {
            require(limit in 1..500) { "Invalid page limit" }
            val query = Bundle()
            query.putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(Calls._ID))
            query.putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                if (ascending) ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
                else ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
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
            require(limit in 1..500) { "Invalid page limit" }
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

        private fun current(cursor: Cursor, namespace: String?): CallRecord =
            CallRecord(
                namespace,
                cursor.getString(0),
                cursor.getLong(3),
                cursor.getLong(4),
                cursor.getInt(2),
                cursor.getString(5),
                cursor.getString(1),
                if (cursor.isNull(6)) null else cursor.getInt(6),
                cursor.getString(7),
            )
    }
}
