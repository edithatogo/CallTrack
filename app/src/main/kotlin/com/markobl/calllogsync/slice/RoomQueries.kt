package com.markobl.calllogsync.slice

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Transactor
import androidx.room3.useWriterConnection

internal data class RecoveryRow(val source: String, val calendar: Long, val started: Long)

internal data class DeliveryCountRow(val state: String, val total: Long)

internal data class SessionPageRow(val cursor: Long, val body: String)

@Dao
internal interface RoomLedgerDao {
    @Query(
        """
        SELECT source, calendar, started FROM (
            SELECT source, calendar, COALESCE(source_start, -1) AS started FROM outbox
            UNION ALL
            SELECT r.source, r.calendar, -1 AS started FROM receipts r
            WHERE NOT EXISTS (
                SELECT 1 FROM outbox o WHERE o.source = r.source AND o.calendar = r.calendar
            )
        )
        WHERE :started IS NULL OR started < :started OR
            (started = :started AND (source > :key OR (source = :key AND calendar > :calendar)))
        ORDER BY started DESC, source ASC, calendar ASC LIMIT :limit
        """,
    )
    suspend fun recoveryPage(
        started: Long?,
        key: String?,
        calendar: Long?,
        limit: Int,
    ): List<RecoveryRow>

    @Query(
        """
        SELECT state, COUNT(*) AS total FROM (
            SELECT state FROM outbox
            UNION ALL
            SELECT 'LEGACY' AS state FROM receipts r
            WHERE NOT EXISTS (
                SELECT 1 FROM outbox o WHERE o.source = r.source AND o.calendar = r.calendar
            )
        ) GROUP BY state
        """,
    )
    suspend fun counts(): List<DeliveryCountRow>

    @Query(
        """
        SELECT source FROM outbox
        WHERE calendar = :calendar AND operation_owner = :owner AND operation_generation = :generation
            AND state IN ('READY', 'IN_FLIGHT', 'ATTEMPTED')
        ORDER BY source ASC LIMIT :limit
        """,
    )
    suspend fun pending(owner: String, generation: Long, calendar: Long, limit: Int): List<String>

    @Query("SELECT * FROM scan_cursors WHERE owner = :owner AND generation = :generation")
    suspend fun checkpoint(owner: String, generation: Long): ScanCursorRow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun storeCheckpoint(row: ScanCursorRow): Long

    @Query(
        """
        UPDATE scan_cursors SET upper_id = :upperId, upper_binding = :upperBinding
        WHERE owner = :owner AND generation = :generation
        """,
    )
    suspend fun rollover(owner: String, generation: Long, upperId: Long, upperBinding: String?): Int

    @Query("SELECT * FROM receipts WHERE source = :key AND calendar = :calendar")
    suspend fun receipt(key: String?, calendar: Long): ReceiptRow?

    @Insert suspend fun insertReceipt(row: ReceiptRow)

    @Query("SELECT * FROM outbox WHERE source = :key AND calendar = :calendar")
    suspend fun intent(key: String?, calendar: Long): OutboxRow?

    @Query(
        """
        SELECT payload_saved_at FROM outbox
        WHERE source = :key AND calendar = :calendar AND payload_title IS NOT NULL
        """,
    )
    suspend fun payloadSavedAt(key: String, calendar: Long): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun storeIntent(row: OutboxRow): Long

    @Query(
        """
        UPDATE outbox
        SET state = CASE WHEN state = 'READY' AND event < 0 THEN 'EXPIRED_READY' ELSE state END,
            payload_title = NULL, payload_description = NULL, payload_start = NULL,
            payload_end = NULL, payload_timezone = NULL, payload_saved_at = NULL
        WHERE rowid IN (
            SELECT rowid FROM outbox WHERE payload_title IS NOT NULL AND payload_saved_at <= :cutoff
            ORDER BY payload_saved_at ASC, source ASC, calendar ASC LIMIT :limit
        )
        """,
    )
    suspend fun expirePayloads(cutoff: Long, limit: Int): Int
}

@Dao
internal interface RoomImportDao {
    @Query("SELECT rowid AS cursor, body FROM sessions ORDER BY rowid DESC LIMIT :limit")
    suspend fun sessions(limit: Int): List<SessionPageRow>

    @Query(
        """
        SELECT rowid AS cursor, body FROM sessions WHERE rowid < :beforeExclusive
        ORDER BY rowid DESC LIMIT :limit
        """,
    )
    suspend fun sessionsBefore(beforeExclusive: Long, limit: Int): List<SessionPageRow>

    @Insert suspend fun insertSession(row: ImportSessionRow)

    @Query("SELECT body FROM sessions WHERE id = :id") suspend fun sessionBody(id: String?): String?

    @Insert suspend fun insertCandidates(rows: List<ImportCandidateRow>)

    @Query(
        """
        SELECT * FROM candidates WHERE session = :id AND source_id > :afterExclusive
        ORDER BY source_id ASC LIMIT :limit
        """,
    )
    suspend fun candidates(id: String?, afterExclusive: Long, limit: Int): List<ImportCandidateRow>

    @Query("UPDATE candidates SET outcome = :outcome WHERE session = :id AND source_id = :sourceId")
    suspend fun storeOutcome(id: String, sourceId: Long, outcome: String): Int

    @Query(
        "UPDATE sessions SET revision = :revision, body = :body WHERE id = :id AND revision = :expected",
    )
    suspend fun storeProgress(id: String, expected: Long, revision: Long, body: String): Int
}

internal suspend fun <T> RoomDatabase.commitIndependently(
    message: String,
    block: suspend () -> T,
): T = useWriterConnection { connection ->
    check(!connection.inTransaction()) { message }
    connection.withTransaction(Transactor.SQLiteTransactionType.IMMEDIATE) { block() }
}
