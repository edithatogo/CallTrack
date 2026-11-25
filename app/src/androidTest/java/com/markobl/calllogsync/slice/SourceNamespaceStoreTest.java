package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

/** Synthetic source identity storage only. */
public class SourceNamespaceStoreTest {
    private SettingsTestFixture fixture() {
        return new SettingsTestFixture(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @Test
    public void failedWriteCannotPublishSourceIdentity() throws Exception {
        verifyFailedWrite(SettingsTestFixture.Failure.BEFORE_WRITE);
    }

    @Test
    public void interruptedSerializationCannotPublishSourceIdentity() throws Exception {
        verifyFailedWrite(SettingsTestFixture.Failure.AFTER_SERIALIZATION);
    }

    private void verifyFailedWrite(SettingsTestFixture.Failure failure) throws Exception {
        try (SettingsTestFixture fixture = fixture()) {
            fixture.failure(failure);
            assertThrows(IllegalStateException.class, fixture::namespace);
            assertEquals("", fixture.disk().getSourceNamespace());
            fixture.failure(null);
            assertEquals(fixture.namespace(), fixture.disk().getSourceNamespace());
        }
    }

    @Test
    public void committedNamespaceIsReusedAcrossAdapters() throws Exception {
        try (SettingsTestFixture fixture = fixture()) {
            String first = fixture.namespace();
            assertTrue(first.startsWith("android-call-log/"));
            assertEquals(first, fixture.disk().getSourceNamespace());
            assertEquals(first, fixture.namespace());
        }
    }
}
