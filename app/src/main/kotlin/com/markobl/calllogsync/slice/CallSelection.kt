package com.markobl.calllogsync.slice

import org.calltrack.core.CallTimeRange
import org.calltrack.core.CallTypeSelection

/** Independent CallLog type switches. Extended/unknown types are excluded, never reclassified. */
class CallSelection(
    @JvmField val incoming: Boolean,
    @JvmField val outgoing: Boolean,
    @JvmField val missed: Boolean,
) {
    private val rules = CallTypeSelection(incoming, outgoing, missed)

    fun accepts(call: CallRecord?): Boolean = rules.accepts(call!!.type)

    companion object {
        @JvmStatic
        fun inRange(call: CallRecord?, startInclusive: Long, endExclusive: Long): Boolean {
            val range = CallTimeRange(startInclusive, endExclusive)
            return range.contains(call!!.start)
        }
    }
}
