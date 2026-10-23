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
            if (source == null || calendar < 0 || expectedTitle.isNullOrEmpty())
                return result(Confidence.UNSUPPORTED_SOURCE)
            val tuple =
                try {
                    legacyTimes(source)
                } catch (invalid: ArithmeticException) {
                    return result(Confidence.UNSUPPORTED_SOURCE)
                } catch (invalid: IllegalArgumentException) {
                    return result(Confidence.UNSUPPORTED_SOURCE)
                }
            if (
                !eventsComplete ||
                    !nearbyComplete ||
                    events == null ||
                    nearby == null ||
                    events.size > 500 ||
                    nearby.size > 500
            )
                return result(Confidence.INCOMPLETE)
            val eventIds = HashSet<Long>()
            for (event in events) if (
                event == null ||
                    event.id < 0 ||
                    event.calendar < 0 ||
                    event.start < 0 ||
                    event.end < event.start ||
                    !eventIds.add(event.id)
            )
                return result(Confidence.INCOMPLETE)
            var sourcePresent = false
            var collision = false
            val sourceKeys = HashSet<String>()
            for (neighbor in nearby) {
                if (neighbor == null || !sourceKeys.add(neighbor.key()))
                    return result(Confidence.INCOMPLETE)
                if (neighbor.key() == source.key()) {
                    if (PagedCalls.binding(neighbor) != PagedCalls.binding(source))
                        return result(Confidence.INCOMPLETE)
                    sourcePresent = true
                    continue
                }
                try {
                    val other = legacyTimes(neighbor)
                    if (tuple[0] == other[0] && tuple[1] == other[1]) collision = true
                } catch (invalid: ArithmeticException) {
                    return result(Confidence.INCOMPLETE)
                } catch (invalid: IllegalArgumentException) {
                    return result(Confidence.INCOMPLETE)
                }
            }
            if (!sourcePresent) return result(Confidence.INCOMPLETE)
            val matches = ArrayList<Long>()
            var exact = false
            for (candidate in events) {
                val event = candidate!!
                if (
                    event.calendar != calendar ||
                        event.start != tuple[0] ||
                        event.end != tuple[1] ||
                        !event.marker.isNullOrEmpty()
                )
                    continue
                val title = expectedTitle == event.title
                val description =
                    !expectedDescription.isNullOrEmpty() && expectedDescription == event.description
                if (!title && !description) continue
                matches.add(event.id)
                exact = title && description
            }
            Collections.sort(matches)
            if (matches.isEmpty()) return result(Confidence.NONE)
            val confidence =
                if (matches.size > 1 || collision) Confidence.AMBIGUOUS
                else if (
                    exact &&
                        source.identityAllowed &&
                        source.number != null &&
                        source.number.length >= 3
                )
                    Confidence.STRONG_CANDIDATE
                else Confidence.POSSIBLE
            return Result(confidence, matches)
        }

        private fun legacyTimes(call: CallRecord): LongArray {
            require(call.type in 1..3 && call.seconds <= Int.MAX_VALUE / 1000) {
                "Unsupported legacy source"
            }
            val start = Math.multiplyExact(Math.round(call.start.toDouble() / 1000.0), 1000L)
            return longArrayOf(start, Math.addExact(start, Math.multiplyExact(call.seconds, 1000L)))
        }

        private fun result(confidence: Confidence): Result =
            Result(confidence, Collections.emptyList())
    }
}
