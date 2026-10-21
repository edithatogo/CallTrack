package com.markobl.calllogsync.slice

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONException
import org.json.JSONObject

/** One atomic settings document; failed cache/disk commits latch process-wide. */
class AndroidActivationStore(private val preferences: SharedPreferences?) :
    ActivationControl.Store {
    constructor(
        context: Context?
    ) : this(context!!.applicationContext.getSharedPreferences("slice", Context.MODE_PRIVATE))

    override fun get(): ActivationControl.Settings? {
        commits.checkReadable()
        val encoded = preferences!!.getString("automaticActivation", null) ?: return null
        try {
            val value = JSONObject(encoded)
            if (value.getInt("version") != 1)
                throw IllegalStateException("Activation migration required")
            return ActivationControl.Settings(
                value.getLong("generation"),
                value.getBoolean("enabled"),
                value.getLong("calendar"),
                value.getString("timezone"),
                value.getLong("start"),
                value.getBoolean("incoming"),
                value.getBoolean("outgoing"),
                value.getBoolean("missed"),
            )
        } catch (invalid: JSONException) {
            throw IllegalStateException("Activation settings unavailable", invalid)
        } catch (invalid: IllegalArgumentException) {
            throw IllegalStateException("Activation settings unavailable", invalid)
        }
    }

    override fun put(settings: ActivationControl.Settings?) {
        try {
            val value = JSONObject()
            value.put("version", 1)
            value.put("generation", settings!!.generation)
            value.put("enabled", settings.enabled)
            value.put("calendar", settings.calendar)
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
