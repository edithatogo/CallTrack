package com.markobl.calllogsync.slice

/** Provider ports and value types shared by call sources and delivery. */
object SliceEngine {
    enum class Result {
        SAVED,
        ALREADY_LOGGED,
        RECOVERED,
        NO_CALL,
        PERMISSION_REQUIRED,
        DESTINATION_UNAVAILABLE,
        REVIEW_REQUIRED,
        INVALID_CALL,
        FAILED,
    }

    fun interface CallSource {
        fun latest(): CallRecord?
    }

    interface CalendarSink {
        fun writable(calendar: Long): Boolean

        fun find(calendar: Long, marker: String?): List<StoredEvent?>?

        fun insert(calendar: Long, event: CallEvent?): Long

        fun read(id: Long): StoredEvent?
    }

    class Entry(@JvmField val id: Long, @JvmField val fingerprint: String?)

    class StoredEvent(
        @JvmField val id: Long,
        @JvmField val calendar: Long,
        @JvmField val event: CallEvent?,
    )
}
