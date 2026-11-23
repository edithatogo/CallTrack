package com.markobl.calllogsync.slice

import android.content.Context
import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.room3.support.getSupportWrapper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import java.util.function.LongSupplier

// These entities describe the existing on-disk schema, not a new interchange format.
// Retaining the original DB filenames is essential for preserving installed receipts.
@Entity(tableName = "receipts", primaryKeys = ["source", "calendar"])
internal data class ReceiptRow(
    val source: String,
    val calendar: Long,
    val event: Long,
    val fingerprint: String,
)

@Entity(
    tableName = "outbox",
    primaryKeys = ["source", "calendar"],
    indices = [Index(value = ["calendar", "state"], name = "outbox_state")],
)
internal data class OutboxRow(
    val source: String,
    val calendar: Long,
    val state: String,
    val event: Long,
    val signature: String?,
    val marker: String?,
    val fingerprint: String,
    @ColumnInfo(name = "payload_title") val payloadTitle: String?,
    @ColumnInfo(name = "payload_description") val payloadDescription: String?,
    @ColumnInfo(name = "payload_start") val payloadStart: Long?,
    @ColumnInfo(name = "payload_end") val payloadEnd: Long?,
    @ColumnInfo(name = "payload_timezone") val payloadTimezone: String?,
    @ColumnInfo(name = "operation_owner") val operationOwner: String?,
    @ColumnInfo(name = "operation_generation", defaultValue = "0") val operationGeneration: Long,
    @ColumnInfo(name = "source_start") val sourceStart: Long?,
    @ColumnInfo(name = "source_seconds") val sourceSeconds: Long?,
    @ColumnInfo(name = "source_type") val sourceType: Int?,
    @ColumnInfo(name = "source_presentation") val sourcePresentation: Int?,
    @ColumnInfo(name = "source_presentation_supplied") val sourcePresentationSupplied: Int?,
    @ColumnInfo(name = "payload_saved_at") val payloadSavedAt: Long?,
)

@Entity(tableName = "scan_cursors", primaryKeys = ["owner", "generation"])
internal data class ScanCursorRow(
    val owner: String,
    val generation: Long,
    val namespace: String,
    @ColumnInfo(name = "upper_id") val upperId: Long,
    @ColumnInfo(name = "upper_binding") val upperBinding: String?,
    @ColumnInfo(name = "last_id") val lastId: Long,
    @ColumnInfo(name = "last_binding") val lastBinding: String?,
)

@Entity(tableName = "sessions")
internal data class ImportSessionRow(
    @PrimaryKey val id: String,
    val revision: Long,
    val body: String,
)

@Entity(
    tableName = "candidates",
    primaryKeys = ["session", "source_id"],
    foreignKeys =
        [
            ForeignKey(
                entity = ImportSessionRow::class,
                parentColumns = ["id"],
                childColumns = ["session"],
            ),
        ],
)
internal data class ImportCandidateRow(
    val session: String,
    @ColumnInfo(name = "source_id") val sourceId: Long,
    @ColumnInfo(name = "source_key") val sourceKey: String,
    val binding: String,
    val outcome: String?,
)

@Dao
internal interface RoomLedgerDao {
    @Query("SELECT COUNT(*) FROM outbox") suspend fun outboxCount(): Long
}

@Dao
internal interface RoomImportDao {
    @Query("SELECT COUNT(*) FROM sessions") suspend fun sessionCount(): Long
}

@Database(
    entities = [ReceiptRow::class, OutboxRow::class, ScanCursorRow::class],
    version = 7,
    exportSchema = true,
)
internal abstract class RoomLedgerDatabase : RoomDatabase() {
    abstract fun ledgerDao(): RoomLedgerDao
}

@Database(
    entities = [ImportSessionRow::class, ImportCandidateRow::class],
    version = 3,
    exportSchema = true,
)
internal abstract class RoomImportDatabase : RoomDatabase() {
    abstract fun importDao(): RoomImportDao
}

/**
 * One Room owner per physical database. Room's official SupportSQLite bridge is temporary: it lets
 * the pre-existing, qualified receipt state machine keep its exact SQL and transaction boundaries
 * until its queries become suspend DAOs.
 */
internal object RoomPersistence {
    private const val RECEIPT_TABLE =
        "CREATE TABLE receipts (source TEXT NOT NULL, calendar INTEGER NOT NULL, event INTEGER NOT NULL, fingerprint TEXT NOT NULL, PRIMARY KEY(source,calendar))"
    private const val SCANS_TABLE =
        "CREATE TABLE scan_cursors (owner TEXT NOT NULL, generation INTEGER NOT NULL, namespace TEXT NOT NULL, upper_id INTEGER NOT NULL, upper_binding TEXT, last_id INTEGER NOT NULL, last_binding TEXT, PRIMARY KEY(owner,generation))"
    private const val OUTBOX_TABLE =
        "CREATE TABLE outbox (source TEXT NOT NULL, calendar INTEGER NOT NULL, state TEXT NOT NULL, event INTEGER NOT NULL, signature TEXT, marker TEXT, fingerprint TEXT NOT NULL, payload_title TEXT, payload_description TEXT, payload_start INTEGER, payload_end INTEGER, payload_timezone TEXT, operation_owner TEXT, operation_generation INTEGER NOT NULL DEFAULT 0, source_start INTEGER, source_seconds INTEGER, source_type INTEGER, source_presentation INTEGER, source_presentation_supplied INTEGER, payload_saved_at INTEGER, PRIMARY KEY(source,calendar))"

    private val migration6To7 =
        object : Migration(6, 7) {
            override suspend fun migrate(connection: SQLiteConnection) {
                // Room installs its schema identity; the legacy schema-6 tables stay
                // byte-compatible.
            }
        }

    private fun ledgerMigration(from: Int, clock: LongSupplier): Migration =
        object : Migration(from, 7) {
            override suspend fun migrate(connection: SQLiteConnection) {
                if (from == 1) {
                    connection.execSQL(OUTBOX_TABLE)
                    connection.execSQL("CREATE INDEX outbox_state ON outbox(calendar,state)")
                    connection.execSQL(
                        "INSERT INTO outbox (source,calendar,state,event,fingerprint) " +
                            "SELECT source,calendar,'LEGACY',event,fingerprint FROM receipts",
                    )
                } else {
                    if (from == 2) {
                        connection.execSQL("ALTER TABLE outbox ADD COLUMN operation_owner TEXT")
                        connection.execSQL(
                            "ALTER TABLE outbox ADD COLUMN operation_generation INTEGER NOT NULL DEFAULT 0",
                        )
                    }
                    if (from < 4) {
                        connection.execSQL("ALTER TABLE outbox ADD COLUMN source_start INTEGER")
                        connection.execSQL("ALTER TABLE outbox ADD COLUMN source_seconds INTEGER")
                        connection.execSQL("ALTER TABLE outbox ADD COLUMN source_type INTEGER")
                        connection.execSQL(
                            "ALTER TABLE outbox ADD COLUMN source_presentation INTEGER",
                        )
                        connection.execSQL(
                            "ALTER TABLE outbox ADD COLUMN source_presentation_supplied INTEGER",
                        )
                    }
                }
                if (from < 5) connection.execSQL(SCANS_TABLE)
                if (from != 1) {
                    connection.execSQL("ALTER TABLE outbox ADD COLUMN payload_saved_at INTEGER")
                }
                val now = clock.asLong
                check(now >= 0) { "Invalid migration clock" }
                connection.execSQL(
                    "UPDATE outbox SET payload_saved_at=" +
                        now +
                        " WHERE payload_title IS NOT NULL",
                )
            }
        }

    private val migration2To3 =
        object : Migration(2, 3) {
            override suspend fun migrate(connection: SQLiteConnection) {
                // Existing import schema-2 already has the final tables/columns.
            }
        }

    private val migration1To3 =
        object : Migration(1, 3) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE candidates ADD COLUMN outcome TEXT")
            }
        }

    fun ledger(context: Context, name: String, clock: LongSupplier): RoomLedgerDatabase =
        Room.databaseBuilder<RoomLedgerDatabase>(context.applicationContext, name)
            .setDriver(AndroidSQLiteDriver())
            .addMigrations(
                migration6To7,
                ledgerMigration(1, clock),
                ledgerMigration(2, clock),
                ledgerMigration(3, clock),
                ledgerMigration(4, clock),
                ledgerMigration(5, clock),
            )
            .build()

    fun imports(context: Context, name: String): RoomImportDatabase =
        Room.databaseBuilder<RoomImportDatabase>(context.applicationContext, name)
            .setDriver(AndroidSQLiteDriver())
            .addMigrations(migration2To3, migration1To3)
            .build()

    fun adapter(database: RoomDatabase): RoomSqliteCompat =
        RoomSqliteCompat(database.getSupportWrapper())
}
