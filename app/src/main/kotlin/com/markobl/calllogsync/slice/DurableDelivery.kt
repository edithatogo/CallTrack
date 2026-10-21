package com.markobl.calllogsync.slice

import java.util.Objects
import java.util.function.BooleanSupplier

/** Every store write commits before returning; one process lock guards provider effects. */
class DurableDelivery(private val store: Store?, private val sink: SliceEngine.CalendarSink?) {
    enum class State {
        READY,
        IN_FLIGHT,
        ATTEMPTED,
        CONFIRMED,
        REVIEW,
        SUPPRESSED,
        LEGACY,
        EXPIRED_READY,
    }

    enum class Result {
        SAVED,
        RECOVERED,
        ALREADY_LOGGED,
        REVIEW_REQUIRED,
        SOURCE_CHANGED,
        PAUSED,
        PERMISSION_REQUIRED,
        DESTINATION_UNAVAILABLE,
        INVALID_CALL,
        FAILED,
        NOTHING_PENDING,
    }

    interface Store {
        fun get(key: String?, calendar: Long): Intent?

        fun put(intent: Intent?)
    }

    class Operation(owner: String?, @JvmField val generation: Long) {
        @JvmField val owner: String

        init {
            require(!owner.isNullOrEmpty() && generation >= 0) { "Invalid operation identity" }
            this.owner = owner
        }
    }

    class SourceFacts(
        @JvmField val start: Long,
        @JvmField val seconds: Long,
        @JvmField val type: Int,
        @JvmField val presentation: Int?,
        @JvmField val presentationSupplied: Boolean,
    ) {
        init {
            require(start >= 0 && seconds >= 0) { "Invalid source time" }
        }

        fun agrees(call: CallRecord?): Boolean =
            start == call!!.start &&
                seconds == call.seconds &&
                type == call.type &&
                (!presentationSupplied ||
                    !call.presentationSupplied ||
                    presentation == call.numberPresentation)

        companion object {
            @JvmStatic
            fun from(call: CallRecord?): SourceFacts =
                SourceFacts(
                    call!!.start,
                    call.seconds,
                    call.type,
                    call.numberPresentation,
                    call.presentationSupplied,
                )
        }
    }

    class Intent(
        state: State?,
        key: String?,
        @JvmField val calendar: Long,
        @JvmField val sourceSignature: String?,
        @JvmField val marker: String?,
        fingerprint: String?,
        @JvmField val eventId: Long,
        @JvmField val payload: CallEvent?,
        @JvmField val operation: Operation?,
        @JvmField val facts: SourceFacts?,
    ) {
        @JvmField val state: State = Objects.requireNonNull(state)!!
        @JvmField val key: String = Objects.requireNonNull(key)!!
        @JvmField val fingerprint: String = Objects.requireNonNull(fingerprint)!!

        constructor(
            state: State?,
            key: String?,
            calendar: Long,
            signature: String?,
            marker: String?,
            fingerprint: String?,
            eventId: Long,
            payload: CallEvent?,
        ) : this(state, key, calendar, signature, marker, fingerprint, eventId, payload, null, null)

        constructor(
            state: State?,
            key: String?,
            calendar: Long,
            signature: String?,
            marker: String?,
            fingerprint: String?,
            eventId: Long,
            payload: CallEvent?,
            operation: Operation?,
        ) : this(
            state,
            key,
            calendar,
            signature,
            marker,
            fingerprint,
            eventId,
            payload,
            operation,
            null,
        )

        fun state(next: State?, id: Long): Intent {
            val retained =
                if (
                    next == State.CONFIRMED ||
                        next == State.REVIEW ||
                        next == State.SUPPRESSED ||
                        next == State.EXPIRED_READY
                )
                    null
                else payload
            return Intent(
                next,
                key,
                calendar,
                sourceSignature,
                marker,
                fingerprint,
                id,
                retained,
                operation,
                facts,
            )
        }

        fun bind(signature: String?): Intent =
            Intent(
                state,
                key,
                calendar,
                signature,
                marker,
                fingerprint,
                eventId,
                payload,
                operation,
                facts,
            )
    }

    fun previouslyConfirmed(key: String?, calendar: Long): Boolean =
        synchronized(DELIVERY_LOCK) {
            val prior = store!!.get(key, calendar)
            prior != null && prior.state == State.CONFIRMED
        }

    fun deliver(
        call: CallRecord?,
        calendar: Long,
        timezone: String?,
        allowed: BooleanSupplier?,
    ): Result = deliver(call, calendar, timezone, null, allowed)

    fun deliver(
        call: CallRecord?,
        calendar: Long,
        timezone: String?,
        operation: Operation?,
        allowed: BooleanSupplier?,
    ): Result =
        synchronized(DELIVERY_LOCK) {
            try {
                if (!allowed!!.asBoolean) return Result.PAUSED
                if (calendar < 0 || !sink!!.writable(calendar))
                    return Result.DESTINATION_UNAVAILABLE
                val signature = CallIdentity.signature(call)
                val source = call!!
                var intent = store!!.get(source.key(), calendar)
                if (intent == null) {
                    val event = CallEvent.from(source, timezone)
                    intent =
                        Intent(
                            State.READY,
                            source.key(),
                            calendar,
                            signature,
                            event.marker,
                            event.fingerprint(),
                            -1,
                            event,
                            operation,
                            SourceFacts.from(source),
                        )
                    store.put(intent)
                } else if (pending(intent) && !sameOperation(intent.operation, operation)) {
                    return Result.PAUSED
                } else if (intent.sourceSignature == null) {
                    if (intent.fingerprint != CallEvent.from(source, timezone).fingerprint())
                        return review(intent, Result.REVIEW_REQUIRED)
                    intent = intent.bind(signature)
                    store.put(intent)
                } else if (
                    intent.sourceSignature != signature ||
                        intent.facts != null && !intent.facts.agrees(source)
                ) {
                    return review(intent, Result.SOURCE_CHANGED)
                }
                attempt(intent, allowed)
            } catch (denied: SecurityException) {
                Result.PERMISSION_REQUIRED
            } catch (invalid: ArithmeticException) {
                Result.INVALID_CALL
            } catch (invalid: IllegalArgumentException) {
                Result.INVALID_CALL
            } catch (failed: RuntimeException) {
                Result.FAILED
            }
        }

    fun resume(key: String?, calendar: Long, allowed: BooleanSupplier?): Result =
        resume(key, calendar, null, allowed)

    fun resume(
        key: String?,
        calendar: Long,
        operation: Operation?,
        allowed: BooleanSupplier?,
    ): Result =
        synchronized(DELIVERY_LOCK) {
            try {
                if (!allowed!!.asBoolean) return Result.PAUSED
                if (calendar < 0 || !sink!!.writable(calendar))
                    return Result.DESTINATION_UNAVAILABLE
                val intent = store!!.get(key, calendar) ?: return Result.NOTHING_PENDING
                if (!sameOperation(intent.operation, operation)) return Result.PAUSED
                attempt(intent, allowed)
            } catch (denied: SecurityException) {
                Result.PERMISSION_REQUIRED
            } catch (failed: RuntimeException) {
                Result.FAILED
            }
        }

    fun reapproveExpired(
        call: CallRecord?,
        calendar: Long,
        timezone: String?,
        operation: Operation?,
        confirmed: BooleanSupplier?,
    ): Result =
        synchronized(DELIVERY_LOCK) {
            try {
                if (!confirmed!!.asBoolean) return Result.PAUSED
                if (operation == null || !operation.owner.startsWith("import/"))
                    return Result.REVIEW_REQUIRED
                val source = call!!
                val prior = store!!.get(source.key(), calendar)
                if (prior == null || prior.state != State.EXPIRED_READY || prior.eventId >= 0)
                    return Result.REVIEW_REQUIRED
                val signature = CallIdentity.signature(source)
                if (
                    prior.sourceSignature == null ||
                        prior.sourceSignature != signature ||
                        prior.facts != null && !prior.facts.agrees(source)
                )
                    return Result.SOURCE_CHANGED
                if (!sink!!.writable(calendar)) return Result.DESTINATION_UNAVAILABLE
                val event = CallEvent.from(source, timezone)
                if (!confirmed.asBoolean) return Result.PAUSED
                val refreshed =
                    Intent(
                        State.READY,
                        source.key(),
                        calendar,
                        signature,
                        event.marker,
                        event.fingerprint(),
                        -1,
                        event,
                        operation,
                        SourceFacts.from(source),
                    )
                store.put(refreshed)
                attempt(refreshed, confirmed)
            } catch (denied: SecurityException) {
                Result.PERMISSION_REQUIRED
            } catch (invalid: ArithmeticException) {
                Result.INVALID_CALL
            } catch (invalid: IllegalArgumentException) {
                Result.INVALID_CALL
            } catch (failed: RuntimeException) {
                Result.FAILED
            }
        }

    private fun attempt(original: Intent, allowed: BooleanSupplier): Result {
        var intent = original
        if (
            intent.state == State.EXPIRED_READY ||
                intent.state == State.REVIEW ||
                intent.state == State.SUPPRESSED
        )
            return Result.REVIEW_REQUIRED
        if (intent.eventId >= 0) return verify(intent, false)
        if (intent.state == State.IN_FLIGHT) return recover(intent)
        if (intent.state != State.READY || intent.payload == null)
            return review(intent, Result.REVIEW_REQUIRED)
        if (!allowed.asBoolean) return Result.PAUSED
        if (!sink!!.writable(intent.calendar)) return Result.DESTINATION_UNAVAILABLE
        // This committed transition is never undone on exceptions/cancellation.
        intent = intent.state(State.IN_FLIGHT, -1)
        store!!.put(intent)
        if (!allowed.asBoolean) return Result.PAUSED
        val id = sink.insert(intent.calendar, intent.payload)
        if (id < 0) return review(intent, Result.REVIEW_REQUIRED)
        intent = intent.state(State.ATTEMPTED, id)
        store.put(intent)
        return verify(intent, true)
    }

    private fun recover(intent: Intent): Result {
        val found = sink!!.find(intent.calendar, intent.marker)!!
        if (found.size != 1 || !matches(found[0], intent))
            return review(intent, Result.REVIEW_REQUIRED)
        val known = intent.state(State.ATTEMPTED, found[0]!!.id)
        store!!.put(known)
        val result = verify(known, false)
        return if (result == Result.ALREADY_LOGGED) Result.RECOVERED else result
    }

    private fun verify(intent: Intent, inserted: Boolean): Result {
        val existing = sink!!.read(intent.eventId)
        if (!matches(existing, intent) || existing!!.id != intent.eventId)
            return review(intent, Result.REVIEW_REQUIRED)
        store!!.put(intent.state(State.CONFIRMED, intent.eventId))
        return if (inserted) Result.SAVED else Result.ALREADY_LOGGED
    }

    private fun matches(event: SliceEngine.StoredEvent?, intent: Intent): Boolean =
        event != null &&
            event.id >= 0 &&
            event.calendar == intent.calendar &&
            event.event != null &&
            intent.fingerprint == event.event.fingerprint()

    private fun review(intent: Intent, result: Result): Result {
        store!!.put(intent.state(State.REVIEW, intent.eventId))
        return result
    }

    companion object {
        @JvmField val DELIVERY_LOCK = Any()

        private fun sameOperation(left: Operation?, right: Operation?): Boolean =
            if (left == null) right == null
            else right != null && left.owner == right.owner && left.generation == right.generation

        private fun pending(intent: Intent): Boolean =
            intent.state == State.READY ||
                intent.state == State.IN_FLIGHT ||
                intent.state == State.ATTEMPTED
    }
}
