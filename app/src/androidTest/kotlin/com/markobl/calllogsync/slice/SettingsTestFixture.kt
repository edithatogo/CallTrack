// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import com.markobl.calllogsync.settings.CallTrackSettings
import java.io.File
import java.io.IOException
import java.io.OutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking

/** Real Android DataStore file with a faultable serializer, isolated from the app's store. */
internal class SettingsTestFixture
@JvmOverloads
constructor(
    context: Context,
    legacyName: String? = null,
) : AutoCloseable {
    private val file = File.createTempFile("calltrack-settings-fixture-", ".pb", context.cacheDir)
    private val job = SupervisorJob()

    @Volatile private var failure: Failure? = null

    enum class Failure {
        BEFORE_WRITE,
        AFTER_SERIALIZATION,
    }

    private val store =
        AndroidSettingsStore(
            DataStoreFactory.create(
                serializer =
                    object : Serializer<CallTrackSettings> by SettingsSerializer {
                        override suspend fun writeTo(t: CallTrackSettings, output: OutputStream) {
                            if (failure == Failure.BEFORE_WRITE) {
                                throw IOException("Fixture write failure")
                            }
                            SettingsSerializer.writeTo(t, output)
                            if (failure == Failure.AFTER_SERIALIZATION) {
                                throw IOException("Fixture interrupted serialization")
                            }
                        }
                    },
                migrations =
                    legacyName?.let { listOf(migrateSliceSettings(context, it)) }.orEmpty(),
                scope = CoroutineScope(Dispatchers.IO + job),
                produceFile = { file },
            ),
        )

    fun activation() = AndroidActivationStore(store)

    fun status() = AndroidWorkStatusStore(store)

    fun namespace() = store.namespace()

    fun failure(mode: Failure?) {
        failure = mode
    }

    fun disk(): CallTrackSettings = file.inputStream().use { CallTrackSettings.parseFrom(it) }

    override fun close() {
        runBlocking { job.cancelAndJoin() }
        check(file.delete()) { "Fixture settings file was not removed" }
    }

    companion object {
        @JvmStatic
        fun snapshot(context: Context): CallTrackSettings = AndroidSettingsStore(context).read()

        @JvmStatic
        fun calendar(context: Context): Long = AndroidSettingsStore(context).selectedCalendar()

        @JvmStatic
        fun select(context: Context, calendar: Long) {
            runBlocking { AndroidSettingsStore(context).selectCalendar(calendar) }
        }

        @JvmStatic
        fun namespace(context: Context, namespace: String) {
            AndroidSettingsStore(context).update {
                it.toBuilder().setSourceNamespace(namespace).build()
            }
        }

        @JvmStatic
        fun clearAutomatic(context: Context) {
            AndroidSettingsStore(context).update {
                it.toBuilder().clearAutomatic().clearExecution().build()
            }
        }
    }
}
