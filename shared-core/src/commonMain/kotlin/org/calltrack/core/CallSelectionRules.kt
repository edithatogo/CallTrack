package org.calltrack.core

/** Portable selection policy for the Android CallLog numeric call types. */
data class CallTypeSelection(val incoming: Boolean, val outgoing: Boolean, val missed: Boolean) {
    fun accepts(rawType: Int): Boolean =
        when (rawType) {
            1 -> incoming
            2 -> outgoing
            3 -> missed
            else -> false
        }
}

/** Validated half-open timestamp range [startInclusive, endExclusive). */
data class CallTimeRange(val startInclusive: Long, val endExclusive: Long) {
    init {
        require(startInclusive >= 0 && endExclusive > startInclusive) { "Invalid call range" }
    }

    fun contains(timestamp: Long): Boolean = timestamp >= startInclusive && timestamp < endExclusive
}
