// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import com.google.protobuf.InvalidProtocolBufferException
import com.markobl.calllogsync.settings.CallTrackSettings
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

internal object SettingsSerializer : Serializer<CallTrackSettings> {
    override val defaultValue: CallTrackSettings = CallTrackSettings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): CallTrackSettings =
        try {
            CallTrackSettings.parseFrom(input)
        } catch (error: InvalidProtocolBufferException) {
            throw CorruptionException("CallTrack settings are invalid", error)
        }

    override suspend fun writeTo(t: CallTrackSettings, output: OutputStream) = t.writeTo(output)
}

internal val Context.callTrackSettings by
    dataStore(
        fileName = "calltrack_settings.pb",
        serializer = SettingsSerializer,
        produceMigrations = { context -> listOf(migrateSliceSettings(context, "slice")) },
    )

internal fun migrateSliceSettings(context: Context, name: String) =
    SharedPreferencesMigration<CallTrackSettings>(
        context = context,
        sharedPreferencesName = name,
        keysToMigrate =
            setOf(
                "calendar",
                "sourceNamespace",
                "automaticActivation",
                "automaticStatusGeneration",
                "automaticStatusAt",
                "automaticStatusToken",
                "automaticStatusCode",
                "automaticStatusDelivered",
                "automaticStatusReview",
                "automaticStatusFailures",
            ),
    ) { previous: SharedPreferencesView, current: CallTrackSettings ->
        val next = current.toBuilder()
        if (!current.hasSelectedCalendar() && "calendar" in previous) {
            next.selectedCalendar = previous.getLong("calendar", -1)
        }
        if (current.sourceNamespace.isEmpty()) {
            previous.getString("sourceNamespace", null)?.let { next.sourceNamespace = it }
        }
        if (!current.hasAutomatic()) {
            previous.getString("automaticActivation", null)?.let {
                next.automatic = AndroidActivationStore.fromLegacy(it)
            }
        }
        val generation = previous.getLong("automaticStatusGeneration", -1)
        if (!current.hasExecution() && generation > 0) {
            next.execution =
                WorkStatus(
                        generation,
                        previous.getLong("automaticStatusAt", 0),
                        previous.getString("automaticStatusToken", "legacy"),
                        WorkStatus.Code.parse(previous.getString("automaticStatusCode", null)),
                        previous.getInt("automaticStatusDelivered", 0),
                        previous.getInt("automaticStatusReview", 0),
                        previous.getLong("automaticStatusFailures", 0),
                    )
                    .toProto()
        }
        next.build()
    }

internal class AndroidSettingsStore(private val store: DataStore<CallTrackSettings>) {
    constructor(context: Context) : this(context.applicationContext.callTrackSettings)

    val data = store.data

    // The delivery and generation guards still execute synchronous provider operations.
    // Only their background callers use these bridges.
    fun read(): CallTrackSettings = backgroundRead { data.first() }

    fun update(transform: (CallTrackSettings) -> CallTrackSettings): CallTrackSettings =
        backgroundRead {
            store.updateData(transform)
        }

    fun selectedCalendar(): Long =
        read().let { if (it.hasSelectedCalendar()) it.selectedCalendar else -1 }

    suspend fun selectCalendar(calendar: Long) {
        store.updateData { it.toBuilder().setSelectedCalendar(calendar).build() }
    }

    suspend fun forgetCalendar(expected: Long) {
        store.updateData { current ->
            if (current.hasSelectedCalendar() && current.selectedCalendar == expected) {
                current.toBuilder().clearSelectedCalendar().build()
            } else {
                current
            }
        }
    }

    fun namespace(): String {
        val existing = read().sourceNamespace
        if (existing.isNotEmpty()) return existing
        return update { current ->
            if (current.sourceNamespace.isNotEmpty()) {
                current
            } else {
                current
                    .toBuilder()
                    .setSourceNamespace("android-call-log/" + UUID.randomUUID())
                    .build()
            }
        }
            .sourceNamespace
    }

    private fun <T> backgroundRead(operation: suspend () -> T): T =
        try {
            runBlocking { operation() }
        } catch (error: IOException) {
            throw IllegalStateException("CallTrack settings unavailable", error)
        }
}
