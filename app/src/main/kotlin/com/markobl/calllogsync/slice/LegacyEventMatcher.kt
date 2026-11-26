package com.markobl.calllogsync.slice

import java.util.Collections

/** Read-only heuristic; even STRONG_CANDIDATE does not prove ownership/adoption consent. */
class LegacyEventMatcher private constructor() {
    enum class Confidence {
        NONE,
        POSSIBLE,
        STRONG_CANDIDATE,
        AMBIGUOUS,
        INCOMPLETE,
        UNSUPPORTED_SOURCE,
    }

    class Event(
        @JvmField val id: Long,
        @JvmField val calendar: Long,
        @JvmField val start: Long,
        @JvmField val end: Long,
        @JvmField val title: String?,
        @JvmField val description: String?,
        @JvmField val marker: String?,
    )

    class Result(@JvmField val confidence: Confidence?, ids: List<Long>?) {
        @JvmField val eventIds: List<Long> = Collections.unmodifiableList(ArrayList(ids!!))
    }

    companion object {
        private const val MAX_LEGACY_BATCH_SIZE = 500
        private const val MIN_IDENTIFIABLE_DIGITS = 3
        private const val MILLIS_PER_SECOND = 1_000

        @JvmStatic
        fun match(
            source: CallRecord?,
            calendar: Long,
            expectedTitle: String?,
            expectedDescription: String?,
            events: List<Event?>?,
            eventsComplete: Boolean,
            nearby: List<CallRecord?>?,
            nearbyComplete: Boolean,
        ): Result {
            if (source == null || calendar < 0 || expectedTitle.isNullOrEmpty()) {
                return result(Confidence.UNSUPPORTED_SOURCE)
            }
            val tuple = legacyTimesOrNull(source) ?: return result(Confidence.UNSUPPORTED_SOURCE)
            if (!eventsComplete || !nearbyComplete) return result(Confidence.INCOMPLETE)
            if (events == null || nearby == null) return result(Confidence.INCOMPLETE)
            if (events.size > MAX_LEGACY_BATCH_SIZE || nearby.size > MAX_LEGACY_BATCH_SIZE) {
                return result(Confidence.INCOMPLETE)
            }
            if (!validEvents(events)) return result(Confidence.INCOMPLETE)
            val collision =
                nearbyCollision(source, tuple, nearby) ?: return result(Confidence.INCOMPLETE)
            return matchEvents(
                source,
                calendar,
                tuple,
                expectedTitle,
                expectedDescription,
                events,
                collision,
            )
        }

        internal fun validEvents(events: List<Event?>): Boolean {
            val ids = HashSet<Long>()
            return events.all { event ->
                event != null &&
                    event.id >= 0 &&
                    event.calendar >= 0 &&
                    event.start >= 0 &&
                    event.end >= event.start &&
                    ids.add(event.id)
            }
        }

        internal fun nearbyCollision(
            source: CallRecord,
            tuple: LongArray,
            nearby: List<CallRecord?>,
        ): Boolean? {
            var sourcePresent = false
            var collision = false
            val sourceKeys = HashSet<String>()
            val complete = nearby.all { neighbor ->
                if (neighbor == null || !sourceKeys.add(neighbor.key())) return@all false
                if (neighbor.key() == source.key()) {
                    sourcePresent = PagedCalls.binding(neighbor) == PagedCalls.binding(source)
                    return@all sourcePresent
                }
                val other = legacyTimesOrNull(neighbor) ?: return@all false
                if (tuple.contentEquals(other)) collision = true
                true
            }
            return collision.takeIf { complete && sourcePresent }
        }

        internal fun matchEvents(
            source: CallRecord,
            calendar: Long,
            tuple: LongArray,
            expectedTitle: String,
            expectedDescription: String?,
            events: List<Event?>,
            collision: Boolean,
        ): Result {
            val matches = ArrayList<Long>()
            var exact = false
            for (candidate in events) {
                val event = candidate!!
                if (!inCalendarSlot(event, calendar, tuple)) continue
                val title = expectedTitle == event.title
                val description =
                    !expectedDescription.isNullOrEmpty() && expectedDescription == event.description
                if (title || description) {
                    matches.add(event.id)
                    exact = title && description
                }
            }
            matches.sort()
            if (matches.isEmpty()) return result(Confidence.NONE)
            val identifiable =
                source.identityAllowed && (source.number?.length ?: 0) >= MIN_IDENTIFIABLE_DIGITS
            val confidence =
                when {
                    matches.size > 1 || collision -> Confidence.AMBIGUOUS
                    exact && identifiable -> Confidence.STRONG_CANDIDATE
                    else -> Confidence.POSSIBLE
                }
            return Result(confidence, matches)
        }

        private fun inCalendarSlot(event: Event, calendar: Long, tuple: LongArray): Boolean =
            event.calendar == calendar &&
                event.start == tuple[0] &&
                event.end == tuple[1] &&
                event.marker.isNullOrEmpty()

        internal fun legacyTimesOrNull(call: CallRecord): LongArray? =
            try {
                require(
                    call.type in CallRecord.INCOMING..CallRecord.MISSED &&
                        call.seconds <= Int.MAX_VALUE / MILLIS_PER_SECOND,
                ) {
                    "Unsupported legacy source"
                }
                val millis = MILLIS_PER_SECOND.toLong()
                val start = Math.multiplyExact(Math.round(call.start.toDouble() / millis), millis)
                longArrayOf(start, Math.addExact(start, Math.multiplyExact(call.seconds, millis)))
            } catch (ignored: ArithmeticException) {
                null
            } catch (ignored: IllegalArgumentException) {
                null
            }

        internal fun result(confidence: Confidence): Result =
            Result(confidence, Collections.emptyList())
    }
}
