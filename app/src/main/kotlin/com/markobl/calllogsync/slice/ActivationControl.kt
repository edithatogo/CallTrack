package com.markobl.calllogsync.slice

import java.util.TimeZone

/** Activation publication shares the delivery lock with the final provider-effect check. */
class ActivationControl(store: Store?) {
    interface Store {
        fun get(): Settings?

        fun put(settings: Settings?)
    }

    class Settings(
        @JvmField val generation: Long,
        @JvmField val enabled: Boolean,
        @JvmField val calendar: Long,
        timezone: String?,
        @JvmField val start: Long,
        @JvmField val incoming: Boolean,
        @JvmField val outgoing: Boolean,
        @JvmField val missed: Boolean,
    ) {
        @JvmField val timezone: String

        init {
            require(
                generation >= 1 &&
                    calendar >= 0 &&
                    start >= 0 &&
                    timezone != null &&
                    TimeZone.getAvailableIDs().contains(timezone)
            ) {
                "Invalid activation settings"
            }
            this.timezone = timezone
        }

        fun operation(): DurableDelivery.Operation =
            DurableDelivery.Operation("automatic", generation)

        fun policy(): ScanDelivery.Policy = ScanDelivery.Policy(start, incoming, outgoing, missed)

        fun paused(): Settings =
            Settings(generation, false, calendar, timezone, start, incoming, outgoing, missed)

        fun same(other: Settings?): Boolean =
            other != null &&
                generation == other.generation &&
                enabled == other.enabled &&
                calendar == other.calendar &&
                start == other.start &&
                timezone == other.timezone &&
                incoming == other.incoming &&
                outgoing == other.outgoing &&
                missed == other.missed
    }

    private val store: Store = store ?: throw NullPointerException()

    fun current(): Settings? = synchronized(DurableDelivery.DELIVERY_LOCK) { store.get() }

    fun activate(
        calendar: Long,
        timezone: String?,
        now: Long,
        incoming: Boolean,
        outgoing: Boolean,
        missed: Boolean,
    ): Settings =
        synchronized(DurableDelivery.DELIVERY_LOCK) {
            val old = store.get()
            val next =
                Settings(
                    if (old == null) 1 else Math.addExact(old.generation, 1),
                    true,
                    calendar,
                    timezone,
                    now,
                    incoming,
                    outgoing,
                    missed,
                )
            store.put(next)
            next
        }

    fun pause() =
        synchronized(DurableDelivery.DELIVERY_LOCK) {
            val old = store.get()
            if (old != null) store.put(old.paused())
        }

    fun allowed(worker: Settings?): Boolean =
        synchronized(DurableDelivery.DELIVERY_LOCK) {
            worker != null && worker.enabled && worker.same(store.get())
        }
}
