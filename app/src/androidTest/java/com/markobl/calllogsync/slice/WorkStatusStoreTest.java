package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

/** Private nonpersonal settings fixture only; no work scheduling or provider permissions. */
public class WorkStatusStoreTest {
    private Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void publicationPersistsFailureCountAndRejectsOldActivationAfterReopen() {
        try (SettingsTestFixture fixture = new SettingsTestFixture(context())) {
            ActivationControl control = new ActivationControl(fixture.activation());
            control.activate(7, "UTC", 1000, true, true, true);
            AndroidWorkStatusStore store = fixture.status();
            assertTrue(store.publish(1, 1001, "a", WorkStatus.Code.FAILED, 0, 0));
            assertTrue(store.publish(1, 1002, "a", WorkStatus.Code.FAILED, 0, 0));
            assertEquals(1, fixture.status().read(1).failures);
            assertTrue(store.publish(1, 1003, "b", WorkStatus.Code.COMPLETE, 2, 0));
            assertEquals(1, store.read(1).failures);
            control.activate(8, "UTC", 2000, true, false, true);
            assertTrue(store.publish(1, 2001, "old", WorkStatus.Code.FAILED, 0, 0));
            assertNull(store.read(2));
            assertEquals(1, store.read(1).failures);
            assertTrue(store.publish(2, 2002, "new", WorkStatus.Code.COMPLETE, 3, 0));
            assertTrue(store.publish(1, 3000, "late", WorkStatus.Code.FAILED, 0, 0));
            WorkStatus current = fixture.status().read(2);
            assertEquals(0, current.failures);
            assertEquals("new", current.token);
            assertEquals(3, current.delivered);
            assertEquals(WorkStatus.Code.COMPLETE, current.code);
        }
    }

    @Test
    public void legacyStatusWithoutCounterRemainsReadableAndUnknownCodesAreSanitised() {
        String name = "status-fixture-" + java.util.UUID.randomUUID();
        SharedPreferences old = context().getSharedPreferences(name, Context.MODE_PRIVATE);
        assertTrue(
                old.edit()
                        .putLong("automaticStatusGeneration", 1)
                        .putLong("automaticStatusAt", 1000)
                        .putString("automaticStatusCode", "unsupported-private-text")
                        .commit());
        try (SettingsTestFixture fixture = new SettingsTestFixture(context(), name)) {
            WorkStatus status = fixture.status().read(1);
            assertEquals(WorkStatus.Code.UNKNOWN, status.code);
            assertEquals(0, status.failures);
            assertEquals("legacy", status.token);
            assertFalse(old.contains("automaticStatusCode"));
        }
    }

    @Test
    public void failedWriteRetainsLastCommittedStatus() throws Exception {
        verifyFailedWrite(SettingsTestFixture.Failure.BEFORE_WRITE);
    }

    @Test
    public void interruptedSerializationRetainsLastCommittedStatus() throws Exception {
        verifyFailedWrite(SettingsTestFixture.Failure.AFTER_SERIALIZATION);
    }

    private void verifyFailedWrite(SettingsTestFixture.Failure failure) throws Exception {
        try (SettingsTestFixture fixture = new SettingsTestFixture(context())) {
            new ActivationControl(fixture.activation()).activate(7, "UTC", 1000, true, true, true);
            AndroidWorkStatusStore store = fixture.status();
            assertTrue(store.publish(1, 1001, "old", WorkStatus.Code.COMPLETE, 0, 0));
            fixture.failure(failure);
            for (long at : new long[] {1002, 1003}) {
                assertThrows(
                        IllegalStateException.class,
                        () -> store.publish(1, at, "attempt", WorkStatus.Code.FAILED, 0, 0));
                assertEquals("old", fixture.disk().getExecution().getToken());
                assertEquals("old", fixture.status().read(1).token);
            }
            fixture.failure(null);
            assertTrue(store.publish(1, 1004, "attempt", WorkStatus.Code.FAILED, 0, 0));
            WorkStatus saved = fixture.status().read(1);
            assertEquals(1, saved.failures);
            assertEquals(1004, saved.at);
            assertEquals("attempt", saved.token);
        }
    }
}
