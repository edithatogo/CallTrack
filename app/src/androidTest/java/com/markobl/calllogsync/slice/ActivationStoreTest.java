package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

/** Isolated settings fixture only; no source/provider access or device permission changes. */
public class ActivationStoreTest {
    private Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void activationAndPauseCommitToDiskAndRejectOldGeneration() throws Exception {
        try (SettingsTestFixture fixture = new SettingsTestFixture(context())) {
            ActivationControl control = new ActivationControl(fixture.activation());
            assertNull(control.current());
            ActivationControl.Settings first = control.activate(7, "UTC", 1000, true, false, true);
            assertEquals(1, fixture.disk().getAutomatic().getGeneration());
            assertTrue(fixture.disk().getAutomatic().getEnabled());
            ActivationControl reopened = new ActivationControl(fixture.activation());
            assertTrue(reopened.allowed(first));
            assertEquals(1000, reopened.current().start);
            assertFalse(reopened.current().outgoing);
            reopened.pause();
            assertFalse(control.allowed(first));
            assertFalse(control.current().enabled);
            assertFalse(fixture.disk().getAutomatic().getEnabled());
            ActivationControl.Settings second = control.activate(8, "UTC", 2000, false, true, true);
            assertEquals(2, second.generation);
            assertFalse(reopened.allowed(first));
            assertTrue(reopened.allowed(second));
        }
    }

    @Test
    public void malformedOrFutureSettingsCannotBecomeEnabled() {
        String name = "activation-fixture-" + java.util.UUID.randomUUID();
        SharedPreferences old = context().getSharedPreferences(name, Context.MODE_PRIVATE);
        try {
            for (String encoded : new String[] {"{", "{\"version\":99}", "{\"version\":1}"}) {
                assertTrue(old.edit().putString("automaticActivation", encoded).commit());
                try (SettingsTestFixture fixture = new SettingsTestFixture(context(), name)) {
                    assertThrows(
                            IllegalStateException.class,
                            () -> new ActivationControl(fixture.activation()).current());
                }
                assertEquals(encoded, old.getString("automaticActivation", null));
            }
        } finally {
            assertTrue(context().deleteSharedPreferences(name));
        }
    }

    @Test
    public void failedWriteCannotPublishActivation() throws Exception {
        verifyFailedWrite(SettingsTestFixture.Failure.BEFORE_WRITE);
    }

    @Test
    public void interruptedSerializationCannotPublishActivation() throws Exception {
        verifyFailedWrite(SettingsTestFixture.Failure.AFTER_SERIALIZATION);
    }

    private void verifyFailedWrite(SettingsTestFixture.Failure failure) throws Exception {
        try (SettingsTestFixture fixture = new SettingsTestFixture(context())) {
            ActivationControl control = new ActivationControl(fixture.activation());
            ActivationControl.Settings current = control.activate(7, "UTC", 1000, true, true, true);
            fixture.failure(failure);
            assertThrows(
                    IllegalStateException.class,
                    () -> control.activate(8, "UTC", 2000, true, true, true));
            assertEquals(current.generation, fixture.disk().getAutomatic().getGeneration());
            assertTrue(new ActivationControl(fixture.activation()).allowed(current));
            assertEquals(7, fixture.activation().get().calendar);
        }
    }
}
