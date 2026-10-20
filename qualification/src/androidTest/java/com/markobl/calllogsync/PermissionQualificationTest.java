package com.markobl.calllogsync;

import static org.junit.Assert.*;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

/** Zero-row checks exercise permission enforcement without reading call history. */
public final class PermissionQualificationTest {
    @Test
    public void testDenied() {
        checkPermissionContract(false);
    }

    @Test
    public void testGranted() {
        checkPermissionContract(true);
    }

    private void checkPermissionContract(boolean expectedGrant) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        boolean granted =
                context.checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                        == PackageManager.PERMISSION_GRANTED;
        assertEquals("Permission state must match requested mode", expectedGrant, granted);
        try (Cursor cursor =
                context.getContentResolver()
                        .query(
                                CallLog.Calls.CONTENT_URI,
                                new String[] {CallLog.Calls._ID},
                                "1=0",
                                null,
                                null)) {
            if (!granted) {
                fail("Provider query unexpectedly allowed while permission denied");
            }
            assertNotNull("Provider returned null cursor", cursor);
            assertEquals("Selection must never return personal records", 0, cursor.getCount());
        } catch (SecurityException expected) {
            if (granted) {
                throw expected;
            }
        }
        assertFalse("Qualification must keep sync disabled", Sync.isEnabled(context));
        assertNull("Qualification must not configure an endpoint", Config.load(context).endpoint);
    }
}
