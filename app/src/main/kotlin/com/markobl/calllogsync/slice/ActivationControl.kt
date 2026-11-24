package com.markobl.calllogsync.slice

import java.util.Collections
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
        destinations: Map<Long, Set<String>>,
        timezone: String?,
        @JvmField val start: Long,
        @JvmField val incoming: Boolean,
        @JvmField val outgoing: Boolean,
        @JvmField val missed: Boolean,
        bindings: Map<Long, String> = emptyMap(),
    ) {
        @JvmField val timezone: String
        @JvmField val destinations: Map<Long, Set<String>>
        @JvmField val calendarBindings: Map<Long, String>
        @JvmField val calendar: Long

        constructor(
            generation: Long,
            enabled: Boolean,
            calendar: Long,
            timezone: String?,
            start: Long,
            incoming: Boolean,
            outgoing: Boolean,
            missed: Boolean,
        ) : this(
            generation,
            enabled,
            mapOf(calendar to setOf(CallRecord.PHONE)),
            timezone,
            start,
            incoming,
            outgoing,
            missed,
        )

        init {
            require(
                generation >= 1 &&
                    destinations.isNotEmpty() &&
                    destinations.all { (id, sources) ->
                        id >= 0 && sources.isNotEmpty() && sources.none { it.isBlank() }
                    } &&
                    (bindings.isEmpty() || bindings.keys == destinations.keys) &&
                    bindings.values.all { it.matches(CALENDAR_BINDING) } &&
                    start >= 0 &&
                    timezone != null &&
                    TimeZone.getAvailableIDs().contains(timezone),
            ) {
                "Invalid activation settings"
            }
            this.timezone = timezone
            this.destinations =
                Collections.unmodifiableMap(
                    destinations.toSortedMap().mapValues {
                        Collections.unmodifiableSet(it.value.toSet())
                    },
                )
            calendar = this.destinations.keys.first()
            calendarBindings = Collections.unmodifiableMap(bindings.toMap())
        }

        @JvmOverloads
        fun operation(calendar: Long = this.calendar): DurableDelivery.Operation {
            require(calendar in destinations)
            val owner = if (destinations.size == 1) "automatic" else "automatic/$calendar"
            return DurableDelivery.Operation(owner, generation)
        }

        @JvmOverloads
        fun policy(calendar: Long = this.calendar): ScanDelivery.Policy =
            ScanDelivery.Policy(start, incoming, outgoing, missed, destinations.getValue(calendar))

        fun paused(): Settings =
            Settings(
                generation,
                false,
                destinations,
                timezone,
                start,
                incoming,
                outgoing,
                missed,
                calendarBindings,
            )

        fun same(other: Settings?): Boolean =
            other != null &&
                generation == other.generation &&
                enabled == other.enabled &&
                destinations == other.destinations &&
                calendarBindings == other.calendarBindings &&
                start == other.start &&
                timezone == other.timezone &&
                incoming == other.incoming &&
                outgoing == other.outgoing &&
                missed == other.missed

        private companion object {
            val CALENDAR_BINDING = Regex("[a-f0-9]{64}")
        }
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
        activate(
            mapOf(calendar to setOf(CallRecord.PHONE)),
            timezone,
            now,
            incoming,
            outgoing,
            missed,
        )

    @JvmOverloads
    fun activate(
        destinations: Map<Long, Set<String>>,
        timezone: String?,
        now: Long,
        incoming: Boolean,
        outgoing: Boolean,
        missed: Boolean,
        calendarBindings: Map<Long, String> = emptyMap(),
    ): Settings =
        synchronized(DurableDelivery.DELIVERY_LOCK) {
            val old = store.get()
            val next =
                Settings(
                    if (old == null) 1 else Math.addExact(old.generation, 1),
                    true,
                    destinations,
                    timezone,
                    now,
                    incoming,
                    outgoing,
                    missed,
                    calendarBindings,
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
