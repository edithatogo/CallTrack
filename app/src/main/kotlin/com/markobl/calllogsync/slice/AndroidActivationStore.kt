package com.markobl.calllogsync.slice

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** One atomic settings document; failed cache/disk commits latch process-wide. */
class AndroidActivationStore(private val preferences: SharedPreferences?) :
    ActivationControl.Store {
    constructor(
        context: Context?,
    ) : this(context!!.applicationContext.getSharedPreferences("slice", Context.MODE_PRIVATE))

    override fun get(): ActivationControl.Settings? {
        commits.checkReadable()
        val encoded = preferences!!.getString("automaticActivation", null) ?: return null
        try {
            val value = JSONObject(encoded)
            val version = value.getInt("version")
            val routes =
                when (version) {
                    1 -> emptyMap()
                    2 -> readDestinations(value.getJSONArray("destinations"))
                    else -> throw IllegalStateException("Activation migration required")
                }
            return ActivationControl.Settings(
                value.getLong("generation"),
                value.getBoolean("enabled"),
                if (version == 1) mapOf(value.getLong("calendar") to setOf(CallRecord.PHONE))
                else routes.mapValues { it.value.first },
                value.getString("timezone"),
                value.getLong("start"),
                value.getBoolean("incoming"),
                value.getBoolean("outgoing"),
                value.getBoolean("missed"),
                routes.mapValues { it.value.second },
            )
        } catch (invalid: JSONException) {
            throw IllegalStateException("Activation settings unavailable", invalid)
        } catch (invalid: IllegalArgumentException) {
            throw IllegalStateException("Activation settings unavailable", invalid)
        }
    }

    private fun readDestinations(entries: JSONArray): Map<Long, Pair<Set<String>, String>> =
        buildMap {
            for (i in 0 until entries.length()) {
                val entry = entries.getJSONObject(i)
                val sources = entry.getJSONArray("sources")
                val previous =
                    put(
                        entry.getLong("calendar"),
                        buildSet {
                            for (j in 0 until sources.length()) {
                                val source =
                                    sources.get(j) as? String
                                        ?: throw IllegalArgumentException("Invalid call source")
                                add(source)
                            }
                        } to entry.getString("binding"),
                    )
                require(previous == null) { "Duplicate activation destination" }
            }
        }

    override fun put(settings: ActivationControl.Settings?) {
        try {
            val value = JSONObject()
            val legacy = settings!!.calendarBindings.isEmpty()
            value.put("version", if (legacy) 1 else 2)
            value.put("generation", settings.generation)
            value.put("enabled", settings.enabled)
            if (legacy) {
                check(settings.destinations.values.singleOrNull() == setOf(CallRecord.PHONE)) {
                    "Calendar bindings required"
                }
                value.put("calendar", settings.calendar)
            } else {
                val destinations = JSONArray()
                for ((calendar, sources) in settings.destinations) {
                    destinations.put(
                        JSONObject()
                            .put("calendar", calendar)
                            .put("sources", JSONArray(sources.sorted()))
                            .put("binding", settings.calendarBindings.getValue(calendar)),
                    )
                }
                value.put("destinations", destinations)
            }
            value.put("timezone", settings.timezone)
            value.put("start", settings.start)
            value.put("incoming", settings.incoming)
            value.put("outgoing", settings.outgoing)
            value.put("missed", settings.missed)
            commits.commit {
                preferences!!.edit().putString("automaticActivation", value.toString()).commit()
            }
        } catch (invalid: JSONException) {
            throw IllegalStateException("Activation encoding failed", invalid)
        }
    }

    companion object {
        private val commits = ActivationCommitGuard()
    }
}
