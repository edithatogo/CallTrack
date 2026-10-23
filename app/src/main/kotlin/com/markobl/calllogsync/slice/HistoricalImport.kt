package com.markobl.calllogsync.slice

import java.util.Collections
import java.util.UUID
import java.util.function.BooleanSupplier

/** Frozen historical candidates; every store mutation commits atomically before returning. */
class HistoricalImport(
    private val store: Store?,
    private val source: PagedCalls.Source?,
    private val receipts: DurableDelivery.Store?,
    private val delivery: DurableDelivery?,
) {
    enum class Phase {
        BUILDING,
        PREVIEW,
        CONFIRMED,
        COMPLETE,
        CANCELLED,
        REVIEW,
    }

    interface Store {
        fun create(session: Session?)

        fun get(id: String?): Session?

        fun save(expected: Session?, next: Session?, added: List<Candidate?>?)

        fun candidates(id: String?, afterExclusive: Long, limit: Int): List<Candidate?>?

        fun finish(
            expected: Session?,
            next: Session?,
            candidate: Candidate?,
            outcome: DurableDelivery.Result?,
        )
    }

    class Candidate(
        @JvmField val sourceId: Long,
        key: String?,
        binding: String?,
        @JvmField val outcome: DurableDelivery.Result?,
    ) {
        @JvmField val key: String
        @JvmField val binding: String

        init {
            require(sourceId >= 0 && !key.isNullOrEmpty() && !binding.isNullOrEmpty()) {
                "Invalid import candidate"
            }
            this.key = key
            this.binding = binding
        }

        constructor(
            sourceId: Long,
            key: String?,
            binding: String?,
        ) : this(sourceId, key, binding, null)

        companion object {
            @JvmStatic
            fun from(call: CallRecord?): Candidate =
                Candidate(PagedCalls.id(call), call!!.key(), PagedCalls.binding(call))
        }
    }

    class Session(
        id: String?,
        @JvmField val calendar: Long,
        timezone: String?,
        @JvmField val from: Long,
        @JvmField val to: Long,
        @JvmField val incoming: Boolean,
        @JvmField val outgoing: Boolean,
        @JvmField val missed: Boolean,
        phase: Phase?,
        checkpoint: PagedCalls.Checkpoint?,
        @JvmField val revision: Long,
        @JvmField val count: Long,
        @JvmField val processedId: Long,
        @JvmField val saved: Long,
        @JvmField val review: Long,
        @JvmField val stopped: DurableDelivery.Result?,
    ) {
        @JvmField val id: String
        @JvmField val timezone: String
        @JvmField val phase: Phase
        @JvmField val checkpoint: PagedCalls.Checkpoint

        init {
            ActivationControl.Settings(
                1,
                false,
                calendar,
                timezone,
                from,
                incoming,
                outgoing,
                missed,
            )
            require(
                id != null &&
                    id.matches(Regex("[a-f0-9-]{36}")) &&
                    to > from &&
                    revision >= 0 &&
                    count >= 0 &&
                    processedId >= -1 &&
                    saved >= 0 &&
                    review >= 0 &&
                    saved <= count &&
                    review <= count - saved &&
                    phase != null &&
                    checkpoint != null
            ) {
                "Invalid import session"
            }
            this.id = id
            this.timezone = timezone!!
            this.phase = phase
            this.checkpoint = checkpoint
        }

        fun operation(): DurableDelivery.Operation = DurableDelivery.Operation("import/" + id, 1)

        fun next(
            phase: Phase?,
            checkpoint: PagedCalls.Checkpoint?,
            count: Long,
            processedId: Long,
            saved: Long,
            review: Long,
            stopped: DurableDelivery.Result?,
        ): Session =
            Session(
                id,
                calendar,
                timezone,
                from,
                to,
                incoming,
                outgoing,
                missed,
                phase,
                checkpoint,
                Math.addExact(revision, 1),
                count,
                processedId,
                saved,
                review,
                stopped,
            )

        fun includes(call: CallRecord?): Boolean =
            call!!.start < to &&
                ScanDelivery.Policy(from, incoming, outgoing, missed).includes(call)
    }

    fun start(
        calendar: Long,
        timezone: String?,
        from: Long,
        to: Long,
        incoming: Boolean,
        outgoing: Boolean,
        missed: Boolean,
    ): Session =
        synchronized(DurableDelivery.DELIVERY_LOCK) {
            val session =
                Session(
                    UUID.randomUUID().toString(),
                    calendar,
                    timezone,
                    from,
                    to,
                    incoming,
                    outgoing,
                    missed,
                    Phase.BUILDING,
                    PagedCalls.capture(source),
                    0,
                    0,
                    -1,
                    0,
                    0,
                    null,
                )
            store!!.create(session)
            session
        }

    fun preview(id: String?, limit: Int): Session {
        bounds(limit)
        return synchronized(DurableDelivery.DELIVERY_LOCK) {
            val session = required(id)
            if (session.phase != Phase.BUILDING) return session
            try {
                val page = PagedCalls.read(source, session.checkpoint, limit)
                val candidates = ArrayList<Candidate>()
                var checkpoint = session.checkpoint
                for (call in page) {
                    if (session.includes(call)) candidates.add(Candidate.from(call))
                    checkpoint = checkpoint.advance(call)
                }
                val next =
                    session.next(
                        if (checkpoint.lastId == checkpoint.upperId) Phase.PREVIEW
                        else Phase.BUILDING,
                        checkpoint,
                        Math.addExact(session.count, candidates.size.toLong()),
                        -1,
                        0,
                        0,
                        null,
                    )
                store!!.save(session, next, candidates)
                next
            } catch (changed: PagedCalls.SourceChanged) {
                save(
                    session,
                    session.next(
                        Phase.REVIEW,
                        session.checkpoint,
                        session.count,
                        -1,
                        0,
                        0,
                        DurableDelivery.Result.SOURCE_CHANGED,
                    ),
                )
            }
        }
    }

    fun confirm(id: String?): Session =
        synchronized(DurableDelivery.DELIVERY_LOCK) {
            val session = required(id)
            check(session.phase == Phase.PREVIEW) { "Preview must finish before confirmation" }
            save(
                session,
                session.next(
                    if (session.count == 0L) Phase.COMPLETE else Phase.CONFIRMED,
                    session.checkpoint,
                    session.count,
                    -1,
                    0,
                    0,
                    null,
                ),
            )
        }

    fun cancel(id: String?): Session =
        synchronized(DurableDelivery.DELIVERY_LOCK) {
            val session = required(id)
            if (session.phase == Phase.COMPLETE || session.phase == Phase.CANCELLED) return session
            save(
                session,
                session.next(
                    Phase.CANCELLED,
                    session.checkpoint,
                    session.count,
                    session.processedId,
                    session.saved,
                    session.review,
                    null,
                ),
            )
        }

    fun deliver(id: String?, limit: Int): Session = deliver(id, limit, BooleanSupplier { true })

    fun deliver(id: String?, limit: Int, running: BooleanSupplier?): Session {
        bounds(limit)
        return synchronized(DurableDelivery.DELIVERY_LOCK) {
            var session = required(id)
            if (session.phase != Phase.CONFIRMED) return session
            val page = store!!.candidates(id, session.processedId, limit)
            check(!page.isNullOrEmpty() && page.size <= limit) { "Import candidates unavailable" }
            for (entry in page) {
                if (!running!!.asBoolean) return session
                val candidate = entry!!
                check(
                    candidate.sourceId > session.processedId &&
                        candidate.sourceId <= session.checkpoint.upperId
                ) {
                    "Invalid candidate order"
                }
                val result = attempt(session, candidate, running)
                var saved = session.saved
                var review = session.review
                when (result) {
                    DurableDelivery.Result.SAVED,
                    DurableDelivery.Result.RECOVERED,
                    DurableDelivery.Result.ALREADY_LOGGED -> saved = Math.addExact(saved, 1)
                    DurableDelivery.Result.REVIEW_REQUIRED,
                    DurableDelivery.Result.SOURCE_CHANGED -> review = Math.addExact(review, 1)
                    else ->
                        return finish(
                            session,
                            session.next(
                                session.phase,
                                session.checkpoint,
                                session.count,
                                session.processedId,
                                saved,
                                review,
                                result,
                            ),
                            candidate,
                            result,
                        )
                }
                val next =
                    session.next(
                        if (saved + review == session.count) Phase.COMPLETE else Phase.CONFIRMED,
                        session.checkpoint,
                        session.count,
                        candidate.sourceId,
                        saved,
                        review,
                        null,
                    )
                session = finish(session, next, candidate, result)
                if (session.phase == Phase.COMPLETE) break
            }
            session
        }
    }

    private fun attempt(
        session: Session,
        candidate: Candidate,
        running: BooleanSupplier,
    ): DurableDelivery.Result =
        try {
            val intent = receipts!!.get(candidate.key, session.calendar)
            if (
                intent != null &&
                    intent.state != DurableDelivery.State.EXPIRED_READY &&
                    owned(intent, session) &&
                    bindingMatches(candidate, intent)
            ) {
                delivery!!.resume(candidate.key, session.calendar, session.operation()) {
                    running.asBoolean && allowed(session)
                }
            } else {
                val call = source!!.at(candidate.sourceId)
                if (
                    call == null ||
                        candidate.key != call.key() ||
                        candidate.binding != PagedCalls.binding(call)
                )
                    DurableDelivery.Result.SOURCE_CHANGED
                else if (intent != null && intent.state == DurableDelivery.State.EXPIRED_READY)
                    delivery!!.reapproveExpired(
                        call,
                        session.calendar,
                        session.timezone,
                        session.operation(),
                    ) {
                        running.asBoolean && allowed(session)
                    }
                else
                    delivery!!.deliver(
                        call,
                        session.calendar,
                        session.timezone,
                        session.operation(),
                    ) {
                        running.asBoolean && allowed(session)
                    }
            }
        } catch (denied: SecurityException) {
            DurableDelivery.Result.PERMISSION_REQUIRED
        } catch (failed: RuntimeException) {
            DurableDelivery.Result.FAILED
        }

    private fun finish(
        expected: Session,
        next: Session,
        candidate: Candidate,
        outcome: DurableDelivery.Result,
    ): Session {
        store!!.finish(expected, next, candidate, outcome)
        return next
    }

    private fun allowed(worker: Session): Boolean {
        val current = store!!.get(worker.id)
        return current != null &&
            current.phase == Phase.CONFIRMED &&
            current.revision == worker.revision
    }

    private fun owned(intent: DurableDelivery.Intent?, session: Session): Boolean =
        intent != null &&
            intent.operation != null &&
            intent.operation.owner == session.operation().owner &&
            intent.operation.generation == 1L

    private fun bindingMatches(candidate: Candidate, intent: DurableDelivery.Intent): Boolean =
        intent.facts != null &&
            candidate.binding ==
                intent.sourceSignature +
                    "|presentation-v1|" +
                    intent.facts.presentationSupplied +
                    "|" +
                    intent.facts.presentation

    private fun save(expected: Session, next: Session): Session {
        store!!.save(expected, next, Collections.emptyList())
        return next
    }

    private fun required(id: String?): Session =
        store!!.get(id) ?: throw IllegalArgumentException("Unknown import")

    private fun bounds(limit: Int) {
        require(limit in 1..500) { "Invalid import batch limit" }
    }
}
