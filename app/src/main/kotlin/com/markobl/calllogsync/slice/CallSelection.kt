package com.markobl.calllogsync.slice

/** Independent CallLog type switches. Extended/unknown types are excluded, never reclassified. */
class CallSelection(
    @JvmField val incoming: Boolean,
    @JvmField val outgoing: Boolean,
    @JvmField val missed: Boolean,
) {
    fun accepts(call: CallRecord?): Boolean =
        when (call!!.type) {
            1 -> incoming
            2 -> outgoing
            3 -> missed
            else -> false
        }

    companion object {
        @JvmStatic
        fun inRange(call: CallRecord?, startInclusive: Long, endExclusive: Long): Boolean {
            require(startInclusive >= 0 && endExclusive > startInclusive) { "Invalid call range" }
            return call!!.start >= startInclusive && call.start < endExclusive
        }
    }
}
