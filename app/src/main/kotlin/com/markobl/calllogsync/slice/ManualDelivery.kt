package com.markobl.calllogsync.slice

/** Explicit latest-call action shares durable receipts with background work. */
class ManualDelivery(
    private val source: SliceEngine.CallSource?,
    private val delivery: DurableDelivery?,
    private val timezone: String?,
) {
    fun run(calendar: Long): SliceEngine.Result =
        try {
            val call = source!!.latest()
            if (call == null) SliceEngine.Result.NO_CALL
            else
                when (delivery!!.deliver(call, calendar, timezone) { true }!!) {
                    DurableDelivery.Result.SOURCE_CHANGED,
                    DurableDelivery.Result.PAUSED,
                    DurableDelivery.Result.REVIEW_REQUIRED -> SliceEngine.Result.REVIEW_REQUIRED
                    DurableDelivery.Result.SAVED -> SliceEngine.Result.SAVED
                    DurableDelivery.Result.RECOVERED -> SliceEngine.Result.RECOVERED
                    DurableDelivery.Result.ALREADY_LOGGED -> SliceEngine.Result.ALREADY_LOGGED
                    DurableDelivery.Result.PERMISSION_REQUIRED ->
                        SliceEngine.Result.PERMISSION_REQUIRED
                    DurableDelivery.Result.DESTINATION_UNAVAILABLE ->
                        SliceEngine.Result.DESTINATION_UNAVAILABLE
                    DurableDelivery.Result.INVALID_CALL -> SliceEngine.Result.INVALID_CALL
                    else -> SliceEngine.Result.FAILED
                }
        } catch (denied: SecurityException) {
            SliceEngine.Result.PERMISSION_REQUIRED
        } catch (failed: RuntimeException) {
            SliceEngine.Result.FAILED
        }
}
