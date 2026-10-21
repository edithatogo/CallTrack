package com.markobl.calllogsync.slice

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.Closeable
import java.util.Collections
import java.util.function.LongSupplier

/** Durable source/destination receipts; SQL transactions serialize concurrent instances. */
// SQLiteOpenHelper did not implement AutoCloseable on API26. Keep Kotlin use safe there.
class SqliteLedger(context: Context?, name: String?, private val clock: LongSupplier?) :
    SQLiteOpenHelper(context, name, null, 6),
    SliceEngine.Ledger,
    AutomaticDelivery.Progress,
    Closeable {
    constructor(context: Context?) : this(context, "calltrack-ledger.db")

    constructor(
        context: Context?,
        name: String?,
    ) : this(context, name, LongSupplier { System.currentTimeMillis() })

    private fun now(): Long {
        val time = clock!!.asLong
        check(time >= 0) { "Invalid storage clock" }
        return time
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE receipts (source TEXT NOT NULL, calendar INTEGER NOT NULL, event INTEGER NOT NULL, fingerprint TEXT NOT NULL, PRIMARY KEY(source,calendar))"
        )
        createOutbox(db)
        createScans(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        check(newVersion == 6 && oldVersion in 1..5) { "A preserving ledger migration is required" }
        if (oldVersion == 1) {
            createOutbox(db)
            db.execSQL(
                "INSERT INTO outbox (source,calendar,state,event,fingerprint) SELECT source,calendar,'LEGACY',event,fingerprint FROM receipts"
            )
        } else {
            if (oldVersion == 2) {
                db.execSQL("ALTER TABLE outbox ADD COLUMN operation_owner TEXT")
                db.execSQL(
                    "ALTER TABLE outbox ADD COLUMN operation_generation INTEGER NOT NULL DEFAULT 0"
                )
            }
            if (oldVersion < 4) {
                db.execSQL("ALTER TABLE outbox ADD COLUMN source_start INTEGER")
                db.execSQL("ALTER TABLE outbox ADD COLUMN source_seconds INTEGER")
                db.execSQL("ALTER TABLE outbox ADD COLUMN source_type INTEGER")
                db.execSQL("ALTER TABLE outbox ADD COLUMN source_presentation INTEGER")
                db.execSQL("ALTER TABLE outbox ADD COLUMN source_presentation_supplied INTEGER")
            }
        }
        if (oldVersion < 5) createScans(db)
        if (oldVersion != 1) db.execSQL("ALTER TABLE outbox ADD COLUMN payload_saved_at INTEGER")
        val migrated = ContentValues()
        migrated.put("payload_saved_at", now())
        db.update("outbox", migrated, "payload_title IS NOT NULL", null)
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
        val rows =
            "SELECT source,calendar,COALESCE(source_start,-1) AS started FROM outbox UNION ALL SELECT r.source,r.calendar,-1 AS started FROM receipts r WHERE NOT EXISTS (SELECT 1 FROM outbox o WHERE o.source=r.source AND o.calendar=r.calendar)"
        val predicate =
            if (after == null) ""
            else
                " WHERE started<CAST(? AS INTEGER) OR (started=CAST(? AS INTEGER) AND (source>? OR (source=? AND calendar>CAST(? AS INTEGER))))"
        val args =
            if (after == null) arrayOf(limit.toString())
            else
                arrayOf(
                    after.started.toString(),
                    after.started.toString(),
                    after.key,
                    after.key,
                    after.calendar.toString(),
                    limit.toString(),
                )
        val items = ArrayList<RecoveryItem>()
        readableDatabase
            .rawQuery(
                "SELECT source,calendar,started FROM (" +
                    rows +
                    ")" +
                    predicate +
                    " ORDER BY started DESC,source ASC,calendar ASC LIMIT ?",
                args,
            )
            .use { cursor ->
                while (cursor.moveToNext()) {
                    val point =
                        RecoveryCursor(cursor.getLong(2), cursor.getString(0), cursor.getLong(1))
                    val intent = readIntent(point.key, point.calendar)
                    if (intent != null) items.add(RecoveryItem(point, intent))
                }
            }
        return Collections.unmodifiableList(items)
    }

    fun counts(): DeliveryCounts {
        var pending = 0L
        var verified = 0L
        var review = 0L
        var suppressed = 0L
        readableDatabase
            .rawQuery(
                "SELECT state,COUNT(*) FROM (SELECT state FROM outbox UNION ALL SELECT 'LEGACY' AS state FROM receipts r WHERE NOT EXISTS (SELECT 1 FROM outbox o WHERE o.source=r.source AND o.calendar=r.calendar)) GROUP BY state",
                null,
            )
            .use { cursor ->
                while (cursor.moveToNext()) {
                    val count = cursor.getLong(1)
                    when (DurableDelivery.State.valueOf(cursor.getString(0))) {
                        DurableDelivery.State.READY,
                        DurableDelivery.State.IN_FLIGHT,
                        DurableDelivery.State.ATTEMPTED -> pending = Math.addExact(pending, count)
                        DurableDelivery.State.CONFIRMED -> verified = Math.addExact(verified, count)
                        DurableDelivery.State.REVIEW,
                        DurableDelivery.State.LEGACY,
                        DurableDelivery.State.EXPIRED_READY -> review = Math.addExact(review, count)
                        DurableDelivery.State.SUPPRESSED ->
                            suppressed = Math.addExact(suppressed, count)
                    }
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
        val keys = ArrayList<String>()
        readableDatabase
            .query(
                "outbox",
                arrayOf("source"),
                "calendar=? AND operation_owner=? AND operation_generation=? AND state IN (?,?,?)",
                arrayOf(
                    calendar.toString(),
                    operation.owner,
                    operation.generation.toString(),
                    "READY",
                    "IN_FLIGHT",
                    "ATTEMPTED",
                ),
                null,
                null,
                "source ASC",
                limit.toString(),
            )
            .use { cursor -> while (cursor.moveToNext()) keys.add(cursor.getString(0)) }
        return Collections.unmodifiableList(keys)
    }

    override fun get(operation: DurableDelivery.Operation?): PagedCalls.Checkpoint? =
        checkpoint(operation)

    override fun put(operation: DurableDelivery.Operation?, checkpoint: PagedCalls.Checkpoint?) {
        checkpoint(operation, checkpoint)
    }

    fun checkpoint(operation: DurableDelivery.Operation?): PagedCalls.Checkpoint? {
        require(operation != null) { "Operation required" }
        readableDatabase
            .query(
                "scan_cursors",
                arrayOf("namespace", "upper_id", "upper_binding", "last_id", "last_binding"),
                "owner=? AND generation=?",
                arrayOf(operation.owner, operation.generation.toString()),
                null,
                null,
                null,
            )
            .use { cursor ->
                return if (!cursor.moveToFirst()) null
                else
                    PagedCalls.Checkpoint(
                        cursor.getString(0),
                        cursor.getLong(1),
                        cursor.getString(2),
                        cursor.getLong(3),
                        cursor.getString(4),
                    )
            }
    }

    fun checkpoint(operation: DurableDelivery.Operation?, next: PagedCalls.Checkpoint?) {
        require(operation != null && next != null) { "Checkpoint required" }
        val db = writableDatabase
        check(!db.inTransaction()) { "Checkpoint must commit independently" }
        db.beginTransaction()
        try {
            val prior = checkpoint(operation)
            check(
                prior == null ||
                    prior.namespace == next.namespace &&
                        prior.upperId == next.upperId &&
                        prior.upperBinding == next.upperBinding &&
                        next.lastId >= prior.lastId &&
                        (next.lastId != prior.lastId || prior.lastBinding == next.lastBinding)
            ) {
                "Frozen checkpoint cannot be reset or rebound"
            }
            val values = ContentValues()
            values.put("owner", operation.owner)
            values.put("generation", operation.generation)
            values.put("namespace", next.namespace)
            values.put("upper_id", next.upperId)
            values.put("upper_binding", next.upperBinding)
            values.put("last_id", next.lastId)
            values.put("last_binding", next.lastBinding)
            check(
                db.insertWithOnConflict(
                    "scan_cursors",
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE,
                ) >= 0
            ) {
                "Checkpoint not stored"
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
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
                (next.upperId != expected.upperId || next.upperBinding == expected.upperBinding)
        ) {
            "Invalid scan rollover"
        }
        val db = writableDatabase
        check(!db.inTransaction()) { "Rollover must commit independently" }
        db.beginTransaction()
        try {
            check(PagedCalls.equal(checkpoint(operation), expected)) {
                "Scan changed before rollover"
            }
            val values = ContentValues()
            values.put("upper_id", next.upperId)
            values.put("upper_binding", next.upperBinding)
            check(
                db.update(
                    "scan_cursors",
                    values,
                    "owner=? AND generation=?",
                    arrayOf(operation.owner, operation.generation.toString()),
                ) == 1
            ) {
                "Rollover not stored"
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun transaction(work: SliceEngine.Work?): SliceEngine.Result? {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val result = work!!.run()
            db.setTransactionSuccessful()
            return result
        } finally {
            db.endTransaction()
        }
    }

    override fun get(sourceKey: String?, calendar: Long): SliceEngine.Entry? {
        readableDatabase
            .query(
                "receipts",
                arrayOf("event", "fingerprint"),
                "source=? AND calendar=?",
                arrayOf(sourceKey, calendar.toString()),
                null,
                null,
                null,
            )
            .use { cursor ->
                return if (cursor.moveToFirst())
                    SliceEngine.Entry(cursor.getLong(0), cursor.getString(1))
                else null
            }
    }

    override fun put(sourceKey: String?, calendar: Long, entry: SliceEngine.Entry?) {
        val values = ContentValues()
        values.put("source", sourceKey)
        values.put("calendar", calendar)
        values.put("event", entry!!.id)
        values.put("fingerprint", entry.fingerprint)
        writableDatabase.insertOrThrow("receipts", null, values)
    }

    fun outbox(): DurableDelivery.Store =
        object : DurableDelivery.Store {
            override fun get(key: String?, calendar: Long): DurableDelivery.Intent? =
                readIntent(key, calendar)

            override fun put(intent: DurableDelivery.Intent?) {
                writeIntent(intent)
            }
        }

    fun expirePayloads(limit: Int): Int {
        require(limit in 1..500) { "Invalid retention batch" }
        return synchronized(DurableDelivery.DELIVERY_LOCK) {
            val time = now()
            if (time < PayloadRetention.WINDOW_MILLIS) return 0
            writableDatabase
                .compileStatement(
                    "UPDATE outbox SET state=CASE WHEN state='READY' AND event<0 THEN 'EXPIRED_READY' ELSE state END, payload_title=NULL,payload_description=NULL,payload_start=NULL,payload_end=NULL,payload_timezone=NULL,payload_saved_at=NULL WHERE rowid IN (SELECT rowid FROM outbox WHERE payload_title IS NOT NULL AND payload_saved_at<=? ORDER BY payload_saved_at ASC,source ASC,calendar ASC LIMIT ?)"
                )
                .use { statement ->
                    statement.bindLong(1, time - PayloadRetention.WINDOW_MILLIS)
                    statement.bindLong(2, limit.toLong())
                    statement.executeUpdateDelete()
                }
        }
    }

    private fun readIntent(key: String?, calendar: Long): DurableDelivery.Intent? =
        synchronized(DurableDelivery.DELIVERY_LOCK) { readIntentLocked(key, calendar) }

    private fun readIntentLocked(key: String?, calendar: Long): DurableDelivery.Intent? {
        var found: DurableDelivery.Intent? = null
        var savedAt: Long? = null
        readableDatabase
            .query(
                "outbox",
                arrayOf(
                    "state",
                    "event",
                    "signature",
                    "marker",
                    "fingerprint",
                    "payload_title",
                    "payload_description",
                    "payload_start",
                    "payload_end",
                    "payload_timezone",
                    "operation_owner",
                    "operation_generation",
                    "source_start",
                    "source_seconds",
                    "source_type",
                    "source_presentation",
                    "source_presentation_supplied",
                    "payload_saved_at",
                ),
                "source=? AND calendar=?",
                arrayOf(key, calendar.toString()),
                null,
                null,
                null,
            )
            .use { cursor ->
                if (cursor.moveToFirst()) {
                    val payload =
                        if (cursor.isNull(5)) null
                        else
                            CallEvent(
                                cursor.getString(5),
                                cursor.getString(6),
                                cursor.getLong(7),
                                cursor.getLong(8),
                                cursor.getString(9),
                                cursor.getString(3),
                            )
                    found =
                        DurableDelivery.Intent(
                            DurableDelivery.State.valueOf(cursor.getString(0)),
                            key,
                            calendar,
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getString(4),
                            cursor.getLong(1),
                            payload,
                            if (cursor.isNull(10)) null
                            else
                                DurableDelivery.Operation(cursor.getString(10), cursor.getLong(11)),
                            if (cursor.isNull(12)) null
                            else
                                DurableDelivery.SourceFacts(
                                    cursor.getLong(12),
                                    cursor.getLong(13),
                                    cursor.getInt(14),
                                    if (cursor.isNull(15)) null else cursor.getInt(15),
                                    cursor.getInt(16) != 0,
                                ),
                        )
                    savedAt = if (cursor.isNull(17)) null else cursor.getLong(17)
                }
            }
        val current = found
        if (current != null) {
            if (current.payload != null && PayloadRetention.expired(savedAt, now())) {
                return synchronized(DurableDelivery.DELIVERY_LOCK) {
                    val cleared = PayloadRetention.clear(current)
                    writeIntent(cleared)
                    cleared
                }
            }
            return current
        }
        val prior = get(key, calendar) ?: return null
        return DurableDelivery.Intent(
            DurableDelivery.State.LEGACY,
            key,
            calendar,
            null,
            null,
            prior.fingerprint,
            prior.id,
            null,
        )
    }

    private fun writeIntent(intent: DurableDelivery.Intent?) {
        val db = writableDatabase
        check(!db.inTransaction()) { "Outbox intent must commit before provider access" }
        db.beginTransaction()
        try {
            val current = intent!!
            var savedAt: Long? = null
            if (current.payload != null) {
                db.query(
                        "outbox",
                        arrayOf("payload_title", "payload_saved_at"),
                        "source=? AND calendar=?",
                        arrayOf(current.key, current.calendar.toString()),
                        null,
                        null,
                        null,
                    )
                    .use { prior ->
                        if (prior.moveToFirst() && !prior.isNull(0) && !prior.isNull(1))
                            savedAt = prior.getLong(1)
                    }
                if (savedAt == null) savedAt = now()
            }
            val values = ContentValues()
            values.put("payload_saved_at", savedAt)
            values.put("source", current.key)
            values.put("calendar", current.calendar)
            values.put("state", current.state.name)
            values.put("event", current.eventId)
            values.put("signature", current.sourceSignature)
            values.put("marker", current.marker)
            values.put("fingerprint", current.fingerprint)
            values.put("operation_owner", current.operation?.owner)
            values.put("operation_generation", current.operation?.generation ?: 0L)
            val facts = current.facts
            if (facts != null) {
                values.put("source_start", facts.start)
                values.put("source_seconds", facts.seconds)
                values.put("source_type", facts.type)
                values.put("source_presentation", facts.presentation)
                values.put("source_presentation_supplied", if (facts.presentationSupplied) 1 else 0)
            } else
                for (column in
                    arrayOf(
                        "source_start",
                        "source_seconds",
                        "source_type",
                        "source_presentation",
                        "source_presentation_supplied",
                    )) values.putNull(column)
            val payload = current.payload
            if (payload != null) {
                values.put("payload_title", payload.title)
                values.put("payload_description", payload.description)
                values.put("payload_start", payload.start)
                values.put("payload_end", payload.end)
                values.put("payload_timezone", payload.timezone)
            } else
                for (column in
                    arrayOf(
                        "payload_title",
                        "payload_description",
                        "payload_start",
                        "payload_end",
                        "payload_timezone",
                    )) values.putNull(column)
            check(
                db.insertWithOnConflict("outbox", null, values, SQLiteDatabase.CONFLICT_REPLACE) >=
                    0
            ) {
                "Outbox write was not committed"
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        private fun createScans(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE scan_cursors (owner TEXT NOT NULL, generation INTEGER NOT NULL, namespace TEXT NOT NULL, upper_id INTEGER NOT NULL, upper_binding TEXT, last_id INTEGER NOT NULL, last_binding TEXT, PRIMARY KEY(owner,generation))"
            )
        }

        private fun createOutbox(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE outbox (source TEXT NOT NULL, calendar INTEGER NOT NULL, state TEXT NOT NULL, event INTEGER NOT NULL, signature TEXT, marker TEXT, fingerprint TEXT NOT NULL, payload_title TEXT, payload_description TEXT, payload_start INTEGER, payload_end INTEGER, payload_timezone TEXT, operation_owner TEXT, operation_generation INTEGER NOT NULL DEFAULT 0, source_start INTEGER, source_seconds INTEGER, source_type INTEGER, source_presentation INTEGER, source_presentation_supplied INTEGER, payload_saved_at INTEGER, PRIMARY KEY(source,calendar))"
            )
            db.execSQL("CREATE INDEX outbox_state ON outbox(calendar,state)")
        }
    }
}
