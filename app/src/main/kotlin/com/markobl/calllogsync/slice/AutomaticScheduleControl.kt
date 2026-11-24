package com.markobl.calllogsync.slice

import java.util.concurrent.Future
import java.util.function.BooleanSupplier
import java.util.function.Function

/** Orders policy publication and scheduling against pause and newer activations. */
internal class AutomaticScheduleControl(
    private val activation: ActivationControl?,
    private val schedule: Function<ActivationControl.Settings, Array<Future<*>>?>?,
    private val cancel: Runnable?,
) {
    fun activate(
        calendar: Long,
        timezone: String?,
        now: Long,
        incoming: Boolean,
        outgoing: Boolean,
        missed: Boolean,
    ): ActivationControl.Settings =
        activate(
            mapOf(calendar to setOf(CallRecord.PHONE)),
            timezone,
            now,
            incoming,
            outgoing,
            missed,
        )

    fun activate(
        destinations: Map<Long, Set<String>>,
        timezone: String?,
        now: Long,
        incoming: Boolean,
        outgoing: Boolean,
        missed: Boolean,
        calendarBindings: Map<Long, String> = emptyMap(),
    ): ActivationControl.Settings =
        synchronized(DurableDelivery.DELIVERY_LOCK) {
            val settings =
                activation!!.activate(
                    destinations,
                    timezone,
                    now,
                    incoming,
                    outgoing,
                    missed,
                    calendarBindings,
                )
            schedule!!.apply(settings)
            settings
        }

    fun pause() =
        synchronized(DurableDelivery.DELIVERY_LOCK) {
            activation!!.pause()
            cancel!!.run()
        }

    fun reconcile(): Array<Future<*>>? = reconcile(BooleanSupplier { true })

    fun reconcile(permitted: BooleanSupplier?): Array<Future<*>>? {
        if (!permitted!!.asBoolean) return emptyArray()
        return synchronized(DurableDelivery.DELIVERY_LOCK) {
            if (!permitted.asBoolean) return emptyArray()
            val settings = activation!!.current()
            if (settings != null && settings.enabled && permitted.asBoolean)
                schedule!!.apply(settings)
            else emptyArray()
        }
    }
}
