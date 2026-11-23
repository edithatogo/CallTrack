package com.markobl.calllogsync.slice

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteStatement

/**
 * Temporary Android-specific SQL adapter for Room's official SupportSQLite bridge.
 * It does not open a second database; all operations use the Room-owned connection.
 * Remove alongside legacy imperative queries after suspending DAO parity is proven.
 */
class RoomSqliteCompat(private val database: SupportSQLiteDatabase) {
    fun inTransaction(): Boolean = database.inTransaction()
    fun beginTransaction() = database.beginTransaction()
    fun setTransactionSuccessful() = database.setTransactionSuccessful()
    fun endTransaction() = database.endTransaction()
    fun getVersion(): Int = database.version
    fun setVersion(version: Int) = database.setVersion(version)
    fun execSQL(sql: String) = database.execSQL(sql)
    fun execSQL(sql: String, bindArgs: Array<out Any>) = database.execSQL(sql, bindArgs)
    fun compileStatement(sql: String): SupportSQLiteStatement = database.compileStatement(sql)

    fun rawQuery(sql: String, selectionArgs: Array<String?>?): Cursor =
        if (selectionArgs == null) database.query(sql)
        else database.query(sql, selectionArgs.map { it as Any? }.toTypedArray())

    fun query(
        table: String,
        columns: Array<String>,
        selection: String?,
        selectionArgs: Array<String?>?,
        groupBy: String?,
        having: String?,
        orderBy: String?,
        limit: String? = null,
    ): Cursor {
        val sql =
            SQLiteQueryBuilder.buildQueryString(
                false,
                table,
                columns,
                selection,
                groupBy,
                having,
                orderBy,
                limit,
            )
        return if (selectionArgs == null) database.query(sql)
        else database.query(sql, selectionArgs.map { it as Any? }.toTypedArray())
    }

    fun insertOrThrow(table: String, nullColumnHack: String?, values: ContentValues): Long {
        require(nullColumnHack == null) { "Unexpected null column convention" }
        return database.insert(table, SQLiteDatabase.CONFLICT_NONE, values)
    }

    fun insertWithOnConflict(
        table: String,
        nullColumnHack: String?,
        values: ContentValues,
        conflictAlgorithm: Int,
    ): Long {
        require(nullColumnHack == null) { "Unexpected null column convention" }
        return database.insert(table, conflictAlgorithm, values)
    }

    fun update(
        table: String,
        values: ContentValues,
        whereClause: String?,
        whereArgs: Array<String?>?,
    ): Int =
        database.update(
            table,
            SQLiteDatabase.CONFLICT_NONE,
            values,
            whereClause,
            whereArgs?.map { it as Any? }?.toTypedArray(),
        )

    fun delete(table: String, whereClause: String?, whereArgs: Array<String?>?): Int =
        database.delete(table, whereClause, whereArgs?.map { it as Any? }?.toTypedArray())
}
