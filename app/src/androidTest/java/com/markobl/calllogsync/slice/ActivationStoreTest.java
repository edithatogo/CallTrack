package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Isolated settings fixture only; no source/provider access or device permission changes. */
public class ActivationStoreTest {
    private Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private String fixture() {
        String token = InstrumentationRegistry.getArguments().getString("fixtureToken", "");
        if (!token.matches("[a-f0-9]{32}"))
            throw new IllegalArgumentException("Fixture token required");
        return "activation-" + token;
    }

    @Test
    public void activationAndPauseCommitToDiskAndRejectOldGeneration() throws Exception {
        String name = fixture();
        SharedPreferences preferences = context().getSharedPreferences(name, Context.MODE_PRIVATE);
        try {
            assertTrue(preferences.edit().clear().commit());
            ActivationControl control =
                    new ActivationControl(new AndroidActivationStore(preferences));
            assertNull(control.current());
            ActivationControl.Settings first = control.activate(7, "UTC", 1000, true, false, true);
            String xml =
                    new String(
                            Files.readAllBytes(
                                    new File(
                                                    context().getApplicationInfo().dataDir,
                                                    "shared_prefs/" + name + ".xml")
                                            .toPath()),
                            StandardCharsets.UTF_8);
            assertTrue(xml.contains("automaticActivation"));
            assertTrue(xml.contains("generation"));
            ActivationControl reopened =
                    new ActivationControl(new AndroidActivationStore(preferences));
            assertTrue(reopened.allowed(first));
            assertEquals(1000, reopened.current().start);
            assertFalse(reopened.current().outgoing);
            reopened.pause();
            assertFalse(control.allowed(first));
            assertFalse(control.current().enabled);
            ActivationControl.Settings second = control.activate(8, "UTC", 2000, false, true, true);
            assertEquals(2, second.generation);
            assertFalse(reopened.allowed(first));
            assertTrue(reopened.allowed(second));
        } finally {
            assertTrue(context().deleteSharedPreferences(name));
        }
    }

    @Test
    public void malformedOrFutureSettingsCannotBecomeEnabled() {
        String name = fixture();
        SharedPreferences preferences = context().getSharedPreferences(name, Context.MODE_PRIVATE);
        try {
            for (String encoded : new String[] {"{", "{\"version\":99}", "{\"version\":1}"}) {
                assertTrue(preferences.edit().putString("automaticActivation", encoded).commit());
                try {
                    new ActivationControl(new AndroidActivationStore(preferences)).current();
                    fail("Settings must be refused");
                } catch (IllegalStateException expected) {
                }
            }
        } finally {
            assertTrue(context().deleteSharedPreferences(name));
        }
    }
}
