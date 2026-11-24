package com.markobl.calllogsync.slice

import android.database.Cursor
import android.database.MatrixCursor
import androidx.room3.RoomDatabase
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLITE_DATA_TEXT
import kotlinx.coroutines.runBlocking

/** Inspects stored columns and injects synthetic failures through the Room-owned connection. */
object RoomFixtures {
    @JvmStatic fun version(ledger: SqliteLedger): Int = version(ledger.room)

    @JvmStatic fun version(imports: SqliteImportStore): Int = version(imports.room)

    @JvmStatic
    fun countRows(ledger: SqliteLedger, table: String): Long = countRows(ledger.room, table)

    @JvmStatic
    fun countRows(imports: SqliteImportStore, table: String): Long = countRows(imports.room, table)

    @JvmStatic
    fun query(ledger: SqliteLedger, sql: String, args: Array<String>?): Cursor =
        query(ledger.room, sql, args)

    @JvmStatic
    fun query(imports: SqliteImportStore, sql: String, args: Array<String>?): Cursor =
        query(imports.room, sql, args)

    @JvmStatic
    fun execute(ledger: SqliteLedger, sql: String, vararg args: Any) =
        execute(ledger.room, sql, args)

    @JvmStatic
    fun execute(imports: SqliteImportStore, sql: String, vararg args: Any) =
        execute(imports.room, sql, args)

    @JvmStatic
    fun writeIntentInReceiptTransaction(ledger: SqliteLedger, intent: DurableDelivery.Intent) {
        runBlocking {
            ledger.transaction {
                ledger.writeIntent(intent)
                SliceEngine.Result.NO_CALL
            }
        }
    }

    private fun version(room: RoomDatabase): Int =
        query(room, "PRAGMA user_version", null).use {
            check(it.moveToFirst())
            it.getInt(0)
        }

    private fun countRows(room: RoomDatabase, table: String): Long =
        query(room, "SELECT COUNT(*) FROM " + table, null).use {
            check(it.moveToFirst())
            it.getLong(0)
        }

    private fun query(room: RoomDatabase, sql: String, args: Array<String>?): Cursor = runBlocking {
        room.useReaderConnection { connection ->
            connection.usePrepared(sql) { statement ->
                args?.forEachIndexed { index, value -> statement.bindText(index + 1, value) }
                val rows = MatrixCursor(statement.getColumnNames().toTypedArray())
                while (statement.step()) {
                    rows.addRow(
                        Array<Any?>(statement.getColumnCount()) { column ->
                            when (statement.getColumnType(column)) {
                                SQLITE_DATA_INTEGER -> statement.getLong(column)
                                SQLITE_DATA_TEXT -> statement.getText(column)
                                SQLITE_DATA_NULL -> null
                                else -> error("Unexpected fixture column type")
                            }
                        },
                    )
                }
                rows
            }
        }
    }

    private fun execute(room: RoomDatabase, sql: String, args: Array<out Any>) {
        runBlocking {
            room.useWriterConnection { connection ->
                connection.usePrepared(sql) { statement ->
                    args.forEachIndexed { index, value ->
                        when (value) {
                            is String -> statement.bindText(index + 1, value)
                            is Long -> statement.bindLong(index + 1, value)
                            else -> error("Unexpected fixture bind argument")
                        }
                    }
                    statement.step()
                }
            }
        }
    }
}
