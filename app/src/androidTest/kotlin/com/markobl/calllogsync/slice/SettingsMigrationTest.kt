// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsMigrationTest {
    @Test
    fun movesRoutesNamespaceAndWorkerStatusTogether() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "settings-migration-" + UUID.randomUUID()
        val old = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val binding = "a".repeat(64)
        val routes =
            JSONArray()
                .put(
                    JSONObject()
                        .put("calendar", 7)
                        .put("sources", JSONArray(listOf("phone")))
                        .put("binding", binding),
                )
                .put(
                    JSONObject()
                        .put("calendar", 9)
                        .put("sources", JSONArray(listOf("org.example.calls")))
                        .put("binding", binding),
                )
        val automatic =
            JSONObject()
                .put("version", 2)
                .put("generation", 3)
                .put("enabled", false)
                .put("destinations", routes)
                .put("timezone", "UTC")
                .put("start", 4000)
                .put("incoming", true)
                .put("outgoing", false)
                .put("missed", true)
        assertTrue(
            old.edit()
                .putLong("calendar", 7)
                .putString("sourceNamespace", "migration/source")
                .putString("automaticActivation", automatic.toString())
                .putLong("automaticStatusGeneration", 3)
                .putLong("automaticStatusAt", 6000)
                .putString("automaticStatusToken", "fixture")
                .putString("automaticStatusCode", "COMPLETE")
                .putInt("automaticStatusDelivered", 2)
                .putLong("automaticStatusFailures", 1)
                .commit(),
        )
        SettingsTestFixture(context, name).use { fixture ->
            val settings = checkNotNull(fixture.activation().get())
            assertEquals(3L, settings.generation)
            assertFalse(settings.enabled)
            assertEquals(4000L, settings.start)
            assertEquals(
                mapOf(7L to setOf("phone"), 9L to setOf("org.example.calls")),
                settings.destinations,
            )
            assertEquals(mapOf(7L to binding, 9L to binding), settings.calendarBindings)
            assertEquals("migration/source", fixture.namespace())
            assertEquals(7L, fixture.disk().selectedCalendar)
            assertEquals(2, checkNotNull(fixture.status().read(3)).delivered)
            assertEquals(1L, checkNotNull(fixture.status().read(3)).failures)
            assertTrue(old.all.isEmpty())
        }
    }
}
