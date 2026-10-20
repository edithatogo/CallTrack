package com.markobl.calllogsync.slice;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

/** One atomically committed settings document; no personal caller or account labels. */
public final class AndroidActivationStore implements ActivationControl.Store {
    private final SharedPreferences preferences;
    private static volatile boolean commitUncertain;

    public AndroidActivationStore(Context context) {
        this(context.getApplicationContext().getSharedPreferences("slice", Context.MODE_PRIVATE));
    }

    AndroidActivationStore(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public ActivationControl.Settings get() {
        if (commitUncertain)
            throw new IllegalStateException("Activation commit uncertain; restart required");
        String encoded = preferences.getString("automaticActivation", null);
        if (encoded == null) return null;
        try {
            JSONObject value = new JSONObject(encoded);
            if (value.getInt("version") != 1)
                throw new IllegalStateException("Activation migration required");
            return new ActivationControl.Settings(
                    value.getLong("generation"),
                    value.getBoolean("enabled"),
                    value.getLong("calendar"),
                    value.getString("timezone"),
                    value.getLong("start"),
                    value.getBoolean("incoming"),
                    value.getBoolean("outgoing"),
                    value.getBoolean("missed"));
        } catch (JSONException | IllegalArgumentException invalid) {
            throw new IllegalStateException("Activation settings unavailable", invalid);
        }
    }

    @Override
    public void put(ActivationControl.Settings settings) {
        try {
            JSONObject value = new JSONObject();
            value.put("version", 1);
            value.put("generation", settings.generation);
            value.put("enabled", settings.enabled);
            value.put("calendar", settings.calendar);
            value.put("timezone", settings.timezone);
            value.put("start", settings.start);
            value.put("incoming", settings.incoming);
            value.put("outgoing", settings.outgoing);
            value.put("missed", settings.missed);
            if (!preferences.edit().putString("automaticActivation", value.toString()).commit()) {
                commitUncertain = true;
                throw new IllegalStateException("Activation not committed");
            }
        } catch (JSONException invalid) {
            throw new IllegalStateException("Activation encoding failed", invalid);
        }
    }
}
