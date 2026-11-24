package com.markobl.calllogsync.slice

import android.content.Context
import androidx.room3.withWriteTransaction
import java.io.Closeable
import java.util.Collections
import java.util.function.LongSupplier
import kotlinx.coroutines.runBlocking

/** Durable source/destination receipts; Room transactions serialize concurrent instances. */
class SqliteLedger(context: Context?, name: String?, private val clock: LongSupplier?) :
    SliceEngine.Ledger, AutomaticDelivery.Progress, Closeable {
    constructor(context: Context?) : this(context, "calltrack-ledger.db")

    constructor(
        context: Context?,
        name: String?,
    ) : this(context, name, LongSupplier { System.currentTimeMillis() })

    internal val room =
        RoomPersistence.ledger(requireNotNull(context), requireNotNull(name), requireNotNull(clock))
    private val dao = room.ledgerDao()

    override fun close() = room.close()

    private fun now(): Long {
        val time = clock!!.asLong
        check(time >= 0) { "Invalid storage clock" }
        return time
    }

    class RecoveryCursor(@JvmField val started: Long, key: String?, @JvmField val calendar: Long) {
        @JvmField val key: String

        init {
            require(started >= -1 && !key.isNullOrEmpty() && calendar >= 0) {
                "Invalid receipt cursor"
            }
            this.key = key
        }
    }

    class RecoveryItem(
        @JvmField val cursor: RecoveryCursor?,
        @JvmField val intent: DurableDelivery.Intent?,
    )

    fun recoveryPage(after: RecoveryCursor?, limit: Int): List<RecoveryItem> {
        require(limit in 1..50) { "Invalid receipt page" }
        val rows = runBlocking {
            dao.recoveryPage(after?.started, after?.key, after?.calendar, limit)
        }
        val items = rows.mapNotNull { row ->
            val point = RecoveryCursor(row.started, row.source, row.calendar)
            readIntent(point.key, point.calendar)?.let { RecoveryItem(point, it) }
        }
        return Collections.unmodifiableList(items)
    }

    fun counts(): DeliveryCounts {
        var pending = 0L
        var verified = 0L
        var review = 0L
        var suppressed = 0L
        for (row in runBlocking { dao.counts() }) {
            when (DurableDelivery.State.valueOf(row.state)) {
                DurableDelivery.State.READY,
                DurableDelivery.State.IN_FLIGHT,
                DurableDelivery.State.ATTEMPTED -> pending = Math.addExact(pending, row.total)
                DurableDelivery.State.CONFIRMED -> verified = Math.addExact(verified, row.total)
                DurableDelivery.State.REVIEW,
                DurableDelivery.State.LEGACY,
                DurableDelivery.State.EXPIRED_READY -> review = Math.addExact(review, row.total)
                DurableDelivery.State.SUPPRESSED ->
                    suppressed = Math.addExact(suppressed, row.total)
            }
        }
        return DeliveryCounts(pending, verified, review, suppressed)
    }

    override fun pending(
        operation: DurableDelivery.Operation?,
        calendar: Long,
        limit: Int,
    ): List<String> {
        require(operation != null && calendar >= 0 && limit in 1..500) { "Invalid pending query" }
        return Collections.unmodifiableList(
            runBlocking { dao.pending(operation.owner, operation.generation, calendar, limit) },
        )
    }

    override fun get(operation: DurableDelivery.Operation?): PagedCalls.Checkpoint? =
        checkpoint(operation)

    override fun put(operation: DurableDelivery.Operation?, checkpoint: PagedCalls.Checkpoint?) {
        checkpoint(operation, checkpoint)
    }

    fun checkpoint(operation: DurableDelivery.Operation?): PagedCalls.Checkpoint? {
        require(operation != null) { "Operation required" }
        return runBlocking { readCheckpoint(operation) }
    }

    private suspend fun readCheckpoint(
        operation: DurableDelivery.Operation,
    ): PagedCalls.Checkpoint? =
        dao.checkpoint(operation.owner, operation.generation)?.let {
            PagedCalls.Checkpoint(
                it.namespace,
                it.upperId,
                it.upperBinding,
                it.lastId,
                it.lastBinding,
            )
        }

    fun checkpoint(operation: DurableDelivery.Operation?, next: PagedCalls.Checkpoint?) {
        require(operation != null && next != null) { "Checkpoint required" }
        runBlocking {
            room.commitIndependently("Checkpoint must commit independently") {
                val prior = readCheckpoint(operation)
                check(
                    prior == null ||
                        prior.namespace == next.namespace &&
                            prior.upperId == next.upperId &&
                            prior.upperBinding == next.upperBinding &&
                            next.lastId >= prior.lastId &&
                            (next.lastId != prior.lastId || prior.lastBinding == next.lastBinding),
                ) {
                    "Frozen checkpoint cannot be reset or rebound"
                }
                val row =
                    ScanCursorRow(
                        operation.owner,
                        operation.generation,
                        next.namespace,
                        next.upperId,
                        next.upperBinding,
                        next.lastId,
                        next.lastBinding,
                    )
                check(dao.storeCheckpoint(row) >= 0) { "Checkpoint not stored" }
            }
        }
    }

    override fun rollover(
        operation: DurableDelivery.Operation?,
        expected: PagedCalls.Checkpoint?,
        next: PagedCalls.Checkpoint?,
    ) {
        require(
            operation != null &&
                expected != null &&
                next != null &&
                expected.lastId == expected.upperId &&
                expected.namespace == next.namespace &&
                next.upperId >= expected.upperId &&
                next.lastId == expected.lastId &&
                next.lastBinding == expected.lastBinding &&
                (next.upperId != expected.upperId || next.upperBinding == expected.upperBinding),
        ) {
            "Invalid scan rollover"
        }
        runBlocking {
            room.commitIndependently("Rollover must commit independently") {
                check(PagedCalls.equal(readCheckpoint(operation), expected)) {
                    "Scan changed before rollover"
                }
                check(
                    dao.rollover(
                        operation.owner,
                        operation.generation,
                        next.upperId,
                        next.upperBinding,
                    ) == 1,
                ) {
                    "Rollover not stored"
                }
            }
        }
    }

    override suspend fun transaction(work: SliceEngine.Work?): SliceEngine.Result? =
        room.withWriteTransaction {
            work!!.run()
        }

    fun get(sourceKey: String?, calendar: Long): SliceEngine.Entry? = runBlocking {
        getReceipt(sourceKey, calendar)
    }

    override suspend fun getReceipt(sourceKey: String?, calendar: Long): SliceEngine.Entry? =
        dao.receipt(sourceKey, calendar)?.let { SliceEngine.Entry(it.event, it.fingerprint) }

    fun put(sourceKey: String?, calendar: Long, entry: SliceEngine.Entry?) = runBlocking {
        putReceipt(sourceKey, calendar, entry)
    }

    override suspend fun putReceipt(sourceKey: String?, calendar: Long, entry: SliceEngine.Entry?) {
        dao.insertReceipt(ReceiptRow(sourceKey!!, calendar, entry!!.id, entry.fingerprint!!))
    }

    fun outbox(): DurableDelivery.Store =
        object : DurableDelivery.Store {
            override fun get(key: String?, calendar: Long): DurableDelivery.Intent? =
                readIntent(key, calendar)

            override fun put(intent: DurableDelivery.Intent?) {
                runBlocking { writeIntent(intent) }
            }
        }

    fun expirePayloads(limit: Int): Int {
        require(limit in 1..500) { "Invalid retention batch" }
        return synchronized(DurableDelivery.DELIVERY_LOCK) {
            val time = now()
            if (time < PayloadRetention.WINDOW_MILLIS) return 0
            runBlocking { dao.expirePayloads(time - PayloadRetention.WINDOW_MILLIS, limit) }
        }
    }

    private fun readIntent(key: String?, calendar: Long): DurableDelivery.Intent? =
        synchronized(DurableDelivery.DELIVERY_LOCK) {
            runBlocking {
                val row = dao.intent(key, calendar)
                if (row == null) {
                    val prior = dao.receipt(key, calendar) ?: return@runBlocking null
                    return@runBlocking DurableDelivery.Intent(
                        DurableDelivery.State.LEGACY,
                        prior.source,
                        prior.calendar,
                        null,
                        null,
                        prior.fingerprint,
                        prior.event,
                        null,
                    )
                }
                val current = row.toIntent()
                if (
                    current.payload != null && PayloadRetention.expired(row.payloadSavedAt, now())
                ) {
                    val cleared = PayloadRetention.clear(current)
                    writeIntent(cleared)
                    cleared
                } else current
            }
        }

    private fun OutboxRow.toIntent(): DurableDelivery.Intent =
        DurableDelivery.Intent(
            DurableDelivery.State.valueOf(state),
            source,
            calendar,
            signature,
            marker,
            fingerprint,
            event,
            payloadTitle?.let {
                CallEvent(
                    it,
                    payloadDescription,
                    payloadStart ?: 0L,
                    payloadEnd ?: 0L,
                    payloadTimezone,
                    marker,
                )
            },
            operationOwner?.let { DurableDelivery.Operation(it, operationGeneration) },
            sourceStart?.let {
                DurableDelivery.SourceFacts(
                    it,
                    sourceSeconds ?: 0L,
                    sourceType ?: 0,
                    sourcePresentation,
                    (sourcePresentationSupplied ?: 0) != 0,
                )
            },
        )

    internal suspend fun writeIntent(intent: DurableDelivery.Intent?) {
        room.commitIndependently("Outbox intent must commit before provider access") {
            val current = intent!!
            val payload = current.payload
            val facts = current.facts
            val savedAt = payload?.let {
                dao.payloadSavedAt(current.key, current.calendar) ?: now()
            }
            val row =
                OutboxRow(
                    source = current.key,
                    calendar = current.calendar,
                    state = current.state.name,
                    event = current.eventId,
                    signature = current.sourceSignature,
                    marker = current.marker,
                    fingerprint = current.fingerprint,
                    payloadTitle = payload?.title,
                    payloadDescription = payload?.description,
                    payloadStart = payload?.start,
                    payloadEnd = payload?.end,
                    payloadTimezone = payload?.timezone,
                    operationOwner = current.operation?.owner,
                    operationGeneration = current.operation?.generation ?: 0L,
                    sourceStart = facts?.start,
                    sourceSeconds = facts?.seconds,
                    sourceType = facts?.type,
                    sourcePresentation = facts?.presentation,
                    sourcePresentationSupplied =
                        facts?.let { if (it.presentationSupplied) 1 else 0 },
                    payloadSavedAt = savedAt,
                )
            check(dao.storeIntent(row) >= 0) { "Outbox write was not committed" }
        }
    }
}
