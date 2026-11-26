package com.markobl.calllogsync.slice

import android.content.Context
import java.io.Closeable
import java.util.Collections
import kotlinx.coroutines.runBlocking
import org.json.JSONException
import org.json.JSONObject

private const val MAX_SESSION_PAGE_SIZE = 50
private const val MAX_CANDIDATE_BATCH_SIZE = 500

/** Versioned import sessions; delivery authority remains in the existing receipt ledger. */
class SqliteImportStore(context: Context?, name: String?) : HistoricalImport.Store, Closeable {
    constructor(context: Context?) : this(context, "calltrack-imports.db")

    internal val room = RoomPersistence.imports(requireNotNull(context), requireNotNull(name))
    private val dao = room.importDao()

    override fun close() = room.close()

    class ListedSession(
        @JvmField val cursor: Long,
        @JvmField val session: HistoricalImport.Session?,
    )

    fun sessions(beforeExclusive: Long, limit: Int): List<ListedSession> {
        require(
            (beforeExclusive == -1L || beforeExclusive >= 1) && limit in 1..MAX_SESSION_PAGE_SIZE,
        ) {
            "Invalid session page"
        }
        val rows = runBlocking {
            if (beforeExclusive == -1L) {
                dao.sessions(limit)
            } else {
                dao.sessionsBefore(beforeExclusive, limit)
            }
        }
        return Collections.unmodifiableList(rows.map { ListedSession(it.cursor, decode(it.body)) })
    }

    override fun create(session: HistoricalImport.Session?) {
        val current = session!!
        require(
            current.phase == HistoricalImport.Phase.BUILDING &&
                current.revision == 0L &&
                current.count == 0L &&
                current.processedId == -1L &&
                current.saved == 0L &&
                current.review == 0L &&
                current.checkpoint.lastId == -1L,
        ) {
            "Invalid initial import"
        }
        runBlocking {
            room.commitIndependently("Import creation must commit independently") {
                dao.insertSession(ImportSessionRow(current.id, 0, encode(current)))
            }
        }
    }

    override fun get(id: String?): HistoricalImport.Session? = runBlocking { readSession(id) }

    private suspend fun readSession(id: String?): HistoricalImport.Session? =
        dao.sessionBody(id)?.let(::decode)

    override fun save(
        expected: HistoricalImport.Session?,
        next: HistoricalImport.Session?,
        added: List<HistoricalImport.Candidate?>?,
    ) {
        update(expected, next, added, null, null)
    }

    override fun finish(
        expected: HistoricalImport.Session?,
        next: HistoricalImport.Session?,
        candidate: HistoricalImport.Candidate?,
        outcome: DurableDelivery.Result?,
    ) {
        require(
            candidate != null &&
                outcome != null &&
                expected!!.phase == HistoricalImport.Phase.CONFIRMED,
        ) {
            "Invalid candidate result"
        }
        update(expected, next, Collections.emptyList(), candidate, outcome)
    }

    private fun update(
        expected: HistoricalImport.Session?,
        next: HistoricalImport.Session?,
        added: List<HistoricalImport.Candidate?>?,
        handled: HistoricalImport.Candidate?,
        outcome: DurableDelivery.Result?,
    ) {
        validate(expected, next, added)
        val prior = expected!!
        val updated = next!!
        require(handled != null || updated.processedId == prior.processedId) {
            "Candidate outcome required for progress"
        }
        runBlocking {
            room.commitIndependently("Import progress must commit independently") {
                val stored = readSession(prior.id)
                check(stored != null && encode(stored) == encode(prior)) {
                    "Import changed before publication"
                }
                dao.insertCandidates(
                    added!!.map { candidate ->
                        ImportCandidateRow(
                            updated.id,
                            candidate!!.sourceId,
                            candidate.key,
                            candidate.binding,
                            null,
                        )
                    },
                )
                if (handled != null) storeCandidateOutcome(prior, updated, handled, outcome)
                check(
                    dao.storeProgress(
                        prior.id,
                        prior.revision,
                        updated.revision,
                        encode(updated),
                    ) == 1,
                ) {
                    "Import progress not stored"
                }
            }
        }
    }

    private suspend fun storeCandidateOutcome(
        prior: HistoricalImport.Session,
        updated: HistoricalImport.Session,
        handled: HistoricalImport.Candidate,
        outcome: DurableDelivery.Result?,
    ) {
        val pending = readCandidates(prior.id, prior.processedId, 1)
        check(
            pending.isNotEmpty() &&
                pending[0].sourceId == handled.sourceId &&
                pending[0].key == handled.key &&
                pending[0].binding == handled.binding,
        ) {
            "Candidate changed before result"
        }
        val verified =
            outcome == DurableDelivery.Result.SAVED ||
                outcome == DurableDelivery.Result.RECOVERED ||
                outcome == DurableDelivery.Result.ALREADY_LOGGED
        val terminal =
            verified ||
                outcome == DurableDelivery.Result.REVIEW_REQUIRED ||
                outcome == DurableDelivery.Result.SOURCE_CHANGED
        require(
            if (terminal) {
                updated.processedId == handled.sourceId
            } else {
                updated.processedId == prior.processedId
            },
        ) {
            "Outcome disagrees with progress"
        }
        require(
            updated.saved == prior.saved + (if (verified) 1 else 0) &&
                updated.review == prior.review + (if (terminal && !verified) 1 else 0),
        ) {
            "Outcome disagrees with counts"
        }
        check(dao.storeOutcome(prior.id, handled.sourceId, outcome!!.name) == 1) {
            "Candidate outcome not stored"
        }
    }

    override fun candidates(
        id: String?,
        afterExclusive: Long,
        limit: Int,
    ): List<HistoricalImport.Candidate> {
        require(afterExclusive >= -1 && limit in 1..MAX_CANDIDATE_BATCH_SIZE) {
            "Invalid candidate page"
        }
        return Collections.unmodifiableList(
            runBlocking { readCandidates(id, afterExclusive, limit) },
        )
    }

    private suspend fun readCandidates(
        id: String?,
        afterExclusive: Long,
        limit: Int,
    ): List<HistoricalImport.Candidate> =
        dao.candidates(id, afterExclusive, limit).map {
            HistoricalImport.Candidate(
                it.sourceId,
                it.sourceKey,
                it.binding,
                it.outcome?.let(DurableDelivery.Result::valueOf),
            )
        }

    companion object {
        private fun validate(
            old: HistoricalImport.Session?,
            next: HistoricalImport.Session?,
            added: List<HistoricalImport.Candidate?>?,
        ) {
            require(old != null && next != null && added != null) { "Invalid import transition" }
            validateRequest(old, next, added.size)
            validateCursor(old, next, added.size)
            validatePhase(old, next, added.isEmpty())
            validateProgress(old, next)
            validateCandidates(old, next, added)
        }

        private fun validateRequest(
            old: HistoricalImport.Session,
            next: HistoricalImport.Session,
            added: Int,
        ) {
            require(
                added <= MAX_CANDIDATE_BATCH_SIZE &&
                    old.id == next.id &&
                    next.revision == Math.addExact(old.revision, 1) &&
                    old.calendar == next.calendar &&
                    old.callSource == next.callSource &&
                    old.calendarBinding == next.calendarBinding &&
                    old.timezone == next.timezone &&
                    old.from == next.from &&
                    old.to == next.to &&
                    old.incoming == next.incoming &&
                    old.outgoing == next.outgoing &&
                    old.missed == next.missed,
            ) {
                "Invalid import transition"
            }
        }

        private fun validateCursor(
            old: HistoricalImport.Session,
            next: HistoricalImport.Session,
            added: Int,
        ) {
            require(
                old.checkpoint.namespace == next.checkpoint.namespace &&
                    old.checkpoint.upperId == next.checkpoint.upperId &&
                    old.checkpoint.upperBinding == next.checkpoint.upperBinding &&
                    next.checkpoint.lastId >= old.checkpoint.lastId &&
                    next.processedId >= old.processedId &&
                    next.saved >= old.saved &&
                    next.review >= old.review &&
                    next.count == Math.addExact(old.count, added.toLong()),
            ) {
                "Invalid import transition"
            }
        }

        private fun validatePhase(
            old: HistoricalImport.Session,
            next: HistoricalImport.Session,
            noAddedCandidates: Boolean,
        ) {
            require(
                canTransition(old, next) &&
                    (old.phase == HistoricalImport.Phase.BUILDING ||
                        (noAddedCandidates && PagedCalls.equal(old.checkpoint, next.checkpoint))),
            ) {
                "Import phase cannot change this way"
            }
            require(
                !((next.phase == HistoricalImport.Phase.PREVIEW &&
                    next.checkpoint.lastId != next.checkpoint.upperId) ||
                    (next.phase == HistoricalImport.Phase.COMPLETE &&
                        next.saved + next.review != next.count)),
            ) {
                "Import is incomplete"
            }
        }

        private fun canTransition(
            old: HistoricalImport.Session,
            next: HistoricalImport.Session,
        ): Boolean =
            when (old.phase) {
                HistoricalImport.Phase.BUILDING ->
                    next.phase == HistoricalImport.Phase.BUILDING ||
                        next.phase == HistoricalImport.Phase.PREVIEW ||
                        next.phase == HistoricalImport.Phase.REVIEW ||
                        next.phase == HistoricalImport.Phase.CANCELLED

                HistoricalImport.Phase.PREVIEW ->
                    next.phase == HistoricalImport.Phase.CONFIRMED ||
                        next.phase == HistoricalImport.Phase.COMPLETE ||
                        next.phase == HistoricalImport.Phase.CANCELLED

                HistoricalImport.Phase.CONFIRMED ->
                    next.phase == HistoricalImport.Phase.CONFIRMED ||
                        next.phase == HistoricalImport.Phase.COMPLETE ||
                        next.phase == HistoricalImport.Phase.CANCELLED

                HistoricalImport.Phase.REVIEW -> next.phase == HistoricalImport.Phase.CANCELLED

                else -> false
            }

        private fun validateProgress(
            old: HistoricalImport.Session,
            next: HistoricalImport.Session,
        ) {
            val oldDone = old.saved + old.review
            val nextDone = next.saved + next.review
            if (
                old.phase != HistoricalImport.Phase.CONFIRMED ||
                    next.phase == HistoricalImport.Phase.CANCELLED
            ) {
                require(next.processedId == old.processedId && nextDone == oldDone) {
                    "Import progress requires confirmation"
                }
            } else {
                require(
                    if (next.processedId == old.processedId) {
                        nextDone == oldDone
                    } else {
                        nextDone == Math.addExact(oldDone, 1)
                    },
                ) {
                    "Import progress must handle one candidate"
                }
            }
        }

        private fun validateCandidates(
            old: HistoricalImport.Session,
            next: HistoricalImport.Session,
            added: List<HistoricalImport.Candidate?>,
        ) {
            var previous = old.checkpoint.lastId
            for (entry in added) {
                val candidate = entry!!
                require(
                    candidate.outcome == null &&
                        candidate.sourceId > previous &&
                        candidate.sourceId <= next.checkpoint.lastId,
                ) {
                    "Invalid candidate order"
                }
                previous = candidate.sourceId
            }
            require(
                next.checkpoint.lastId != old.checkpoint.lastId ||
                    next.checkpoint.lastBinding == old.checkpoint.lastBinding,
            ) {
                "Import anchor cannot be rebound"
            }
        }

        private fun encode(s: HistoricalImport.Session): String {
            try {
                val j = JSONObject()
                if (s.calendarBinding == null) {
                    j.put("version", 1)
                } else {
                    j.put("version", 2)
                    j.put("callSource", s.callSource)
                    j.put("calendarBinding", s.calendarBinding)
                }
                j.put("id", s.id)
                j.put("calendar", s.calendar)
                j.put("timezone", s.timezone)
                j.put("from", s.from)
                j.put("to", s.to)
                j.put("incoming", s.incoming)
                j.put("outgoing", s.outgoing)
                j.put("missed", s.missed)
                j.put("phase", s.phase.name)
                j.put("revision", s.revision)
                j.put("count", s.count)
                j.put("processed", s.processedId)
                j.put("saved", s.saved)
                j.put("review", s.review)
                j.put("stopped", s.stopped?.name ?: JSONObject.NULL)
                j.put("namespace", s.checkpoint.namespace)
                j.put("upper", s.checkpoint.upperId)
                j.put("upperBinding", s.checkpoint.upperBinding ?: JSONObject.NULL)
                j.put("last", s.checkpoint.lastId)
                j.put("lastBinding", s.checkpoint.lastBinding ?: JSONObject.NULL)
                return j.toString()
            } catch (invalid: JSONException) {
                throw IllegalStateException("Import encoding failed", invalid)
            }
        }

        private fun decode(body: String?): HistoricalImport.Session {
            try {
                val j = JSONObject(body!!)
                val version = j.getInt("version")
                val callSource =
                    when (version) {
                        1 -> CallRecord.PHONE
                        2 -> j.getString("callSource")
                        else -> error("Import document migration required")
                    }
                return HistoricalImport.Session(
                    j.getString("id"),
                    j.getLong("calendar"),
                    j.getString("timezone"),
                    j.getLong("from"),
                    j.getLong("to"),
                    j.getBoolean("incoming"),
                    j.getBoolean("outgoing"),
                    j.getBoolean("missed"),
                    HistoricalImport.Phase.valueOf(j.getString("phase")),
                    PagedCalls.Checkpoint(
                        j.getString("namespace"),
                        j.getLong("upper"),
                        if (j.isNull("upperBinding")) null else j.getString("upperBinding"),
                        j.getLong("last"),
                        if (j.isNull("lastBinding")) null else j.getString("lastBinding"),
                    ),
                    j.getLong("revision"),
                    j.getLong("count"),
                    j.getLong("processed"),
                    j.getLong("saved"),
                    j.getLong("review"),
                    if (j.isNull("stopped")) {
                        null
                    } else {
                        DurableDelivery.Result.valueOf(j.getString("stopped"))
                    },
                    callSource,
                    if (version == 2) j.getString("calendarBinding") else null,
                )
            } catch (invalid: JSONException) {
                throw IllegalStateException("Import document unavailable", invalid)
            } catch (invalid: IllegalArgumentException) {
                throw IllegalStateException("Import document unavailable", invalid)
            }
        }
    }
}
