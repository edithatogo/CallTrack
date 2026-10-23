package com.markobl.calllogsync.slice

/** Manual single-call orchestration with explicit insertion/receipt recovery. */
class SliceEngine(
    private val source: CallSource?,
    private val sink: CalendarSink?,
    private val ledger: Ledger?,
    private val timezone: String?,
) {
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

    fun interface Work {
        fun run(): Result?
    }

    interface Ledger {
        fun transaction(work: Work?): Result?

        fun get(sourceKey: String?, calendar: Long): Entry?

        fun put(sourceKey: String?, calendar: Long, entry: Entry?)
    }

    class Entry(@JvmField val id: Long, @JvmField val fingerprint: String?)

    class StoredEvent(
        @JvmField val id: Long,
        @JvmField val calendar: Long,
        @JvmField val event: CallEvent?,
    )

    fun run(calendar: Long): Result? =
        try {
            if (calendar < 0 || !sink!!.writable(calendar)) Result.DESTINATION_UNAVAILABLE
            else {
                val call = source!!.latest()
                if (call == null) Result.NO_CALL
                else {
                    val event = CallEvent.from(call, timezone)
                    ledger!!.transaction { save(call.key(), calendar, event) }
                }
            }
        } catch (denied: SecurityException) {
            Result.PERMISSION_REQUIRED
        } catch (invalid: ArithmeticException) {
            Result.INVALID_CALL
        } catch (invalid: IllegalArgumentException) {
            Result.INVALID_CALL
        } catch (failed: RuntimeException) {
            Result.FAILED
        }

    private fun matches(stored: StoredEvent?, calendar: Long, event: CallEvent): Boolean =
        stored != null &&
            stored.calendar == calendar &&
            event.fingerprint() == stored.event!!.fingerprint()

    private fun save(key: String, calendar: Long, event: CallEvent): Result {
        if (!sink!!.writable(calendar)) return Result.DESTINATION_UNAVAILABLE
        val prior = ledger!!.get(key, calendar)
        if (prior != null) {
            if (
                !prior.fingerprint!!.equals(event.fingerprint()) ||
                    !matches(sink.read(prior.id), calendar, event)
            )
                return Result.REVIEW_REQUIRED
            return Result.ALREADY_LOGGED
        }
        val found = sink.find(calendar, event.marker)!!
        if (found.size > 1) return Result.REVIEW_REQUIRED
        val recovered = found.isNotEmpty()
        if (recovered && !matches(found[0], calendar, event)) return Result.REVIEW_REQUIRED
        val id = if (recovered) found[0]!!.id else sink.insert(calendar, event)
        ledger.put(key, calendar, Entry(id, event.fingerprint()))
        return try {
            if (!matches(sink.read(id), calendar, event)) Result.REVIEW_REQUIRED
            else if (recovered) Result.RECOVERED else Result.SAVED
        } catch (denied: SecurityException) {
            Result.PERMISSION_REQUIRED
        } catch (failed: RuntimeException) {
            Result.FAILED
        }
    }
}
