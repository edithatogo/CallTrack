package com.markobl.calllogsync.slice

import android.content.Context
import com.markobl.calllogsync.settings.AutomaticSettings
import com.markobl.calllogsync.settings.CalendarRoute
import org.json.JSONException
import org.json.JSONObject

/** The schedule and route bindings publish together after the DataStore write commits. */
class AndroidActivationStore internal constructor(private val storage: AndroidSettingsStore) :
    ActivationControl.Store {
    constructor(context: Context?) : this(AndroidSettingsStore(checkNotNull(context)))

    override fun get(): ActivationControl.Settings? =
        storage.read().let { if (it.hasAutomatic()) decode(it.automatic) else null }

    override fun put(settings: ActivationControl.Settings?) {
        val encoded = encode(checkNotNull(settings))
        storage.update { it.toBuilder().setAutomatic(encoded).build() }
    }

    internal companion object {
        private fun encode(settings: ActivationControl.Settings): AutomaticSettings =
            AutomaticSettings.newBuilder()
                .setGeneration(settings.generation)
                .setEnabled(settings.enabled)
                .setTimezone(settings.timezone)
                .setStart(settings.start)
                .setIncoming(settings.incoming)
                .setOutgoing(settings.outgoing)
                .setMissed(settings.missed)
                .addAllDestinations(
                    settings.destinations.map { (calendar, sources) ->
                        CalendarRoute.newBuilder()
                            .setCalendar(calendar)
                            .addAllSources(sources.sorted())
                            .setBinding(settings.calendarBindings[calendar].orEmpty())
                            .build()
                    },
                )
                .build()

        private fun decode(settings: AutomaticSettings): ActivationControl.Settings {
            val entries = settings.destinationsList
            val destinations = entries.associate { it.calendar to it.sourcesList.toSet() }
            require(destinations.size == entries.size) { "Duplicate activation destination" }
            val bindings =
                entries.filter { it.binding.isNotEmpty() }.associate { it.calendar to it.binding }
            return ActivationControl.Settings(
                settings.generation,
                settings.enabled,
                destinations,
                settings.timezone,
                settings.start,
                settings.incoming,
                settings.outgoing,
                settings.missed,
                bindings,
            )
        }

        // The only reader of the versioned preference document is the one-time platform migration.
        fun fromLegacy(document: String): AutomaticSettings =
            try {
                readLegacy(document)
            } catch (invalid: JSONException) {
                throw IllegalStateException("Stored activation settings are invalid", invalid)
            }

        private fun readLegacy(document: String): AutomaticSettings {
            val old = JSONObject(document)
            val version = old.getInt("version")
            val entries = buildMap {
                when (version) {
                    1 -> put(old.getLong("calendar"), setOf(CallRecord.PHONE) to "")

                    2 -> {
                        val routes = old.getJSONArray("destinations")
                        for (index in 0 until routes.length()) {
                            val route = routes.getJSONObject(index)
                            val sources = route.getJSONArray("sources")
                            val selected = buildSet {
                                for (source in 0 until sources.length()) {
                                    val id = sources.get(source) as? String
                                    require(!id.isNullOrBlank()) { "Invalid call source" }
                                    add(id)
                                }
                            }
                            val previous =
                                put(
                                    route.getLong("calendar"),
                                    selected to route.getString("binding"),
                                )
                            require(previous == null) { "Duplicate activation destination" }
                        }
                    }

                    else -> error("Unknown stored activation version")
                }
            }
            return encode(
                ActivationControl.Settings(
                    old.getLong("generation"),
                    old.getBoolean("enabled"),
                    entries.mapValues { it.value.first },
                    old.getString("timezone"),
                    old.getLong("start"),
                    old.getBoolean("incoming"),
                    old.getBoolean("outgoing"),
                    old.getBoolean("missed"),
                    entries.filterValues { it.second.isNotEmpty() }.mapValues { it.value.second },
                ),
            )
        }
    }
}
