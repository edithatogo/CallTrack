package com.markobl.calllogsync.slice

/** Native Android CallLog selection; unknown/extended types are never reclassified. */
class CallSelection(
    @JvmField val incoming: Boolean,
    @JvmField val outgoing: Boolean,
    @JvmField val missed: Boolean,
) {
    fun accepts(call: CallRecord?): Boolean =
        when (call!!.type) {
            CallRecord.INCOMING -> incoming
            CallRecord.OUTGOING -> outgoing
            CallRecord.MISSED -> missed
            else -> false
        }

    companion object {
        @JvmStatic
        fun inRange(call: CallRecord?, startInclusive: Long, endExclusive: Long): Boolean {
            require(startInclusive >= 0 && endExclusive > startInclusive) {
                "Invalid call range"
            }
            return call!!.start >= startInclusive && call.start < endExclusive
        }
    }
}
