package com.markobl.calllogsync.slice

/** Explicit latest-call action shares durable receipts with background work. */
class ManualDelivery(
    private val source: SliceEngine.CallSource?,
    private val delivery: DurableDelivery?,
    private val timezone: String?,
) {
    fun run(calendar: Long): SliceEngine.Result =
        try {
            if (delivery!!.writableDestination(calendar)) {
                val call = source!!.latest()
                if (call == null) {
                    SliceEngine.Result.NO_CALL
                } else {
                    resultFor(delivery.deliver(call, calendar, timezone) { true })
                }
            } else {
                SliceEngine.Result.DESTINATION_UNAVAILABLE
            }
        } catch (ignored: SecurityException) {
            SliceEngine.Result.PERMISSION_REQUIRED
        } catch (ignored: RuntimeException) {
            SliceEngine.Result.FAILED
        }

    private fun resultFor(result: DurableDelivery.Result): SliceEngine.Result =
        when (result) {
            DurableDelivery.Result.SOURCE_CHANGED,
            DurableDelivery.Result.PAUSED,
            DurableDelivery.Result.REVIEW_REQUIRED -> SliceEngine.Result.REVIEW_REQUIRED

            DurableDelivery.Result.SAVED -> SliceEngine.Result.SAVED

            DurableDelivery.Result.RECOVERED -> SliceEngine.Result.RECOVERED

            DurableDelivery.Result.ALREADY_LOGGED -> SliceEngine.Result.ALREADY_LOGGED

            DurableDelivery.Result.PERMISSION_REQUIRED -> SliceEngine.Result.PERMISSION_REQUIRED

            DurableDelivery.Result.DESTINATION_UNAVAILABLE ->
                SliceEngine.Result.DESTINATION_UNAVAILABLE

            DurableDelivery.Result.INVALID_CALL -> SliceEngine.Result.INVALID_CALL

            else -> SliceEngine.Result.FAILED
        }
}
