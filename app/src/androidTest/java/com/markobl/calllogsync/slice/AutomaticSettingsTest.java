package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.*;

import androidx.test.platform.app.InstrumentationRegistry;

import com.markobl.calllogsync.R;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

/** Activity state contracts only; no permission dialog, provider data or automatic activation. */
public class AutomaticSettingsTest {
    @Test
    public void openingSettingsDoesNotEnableOrSelectDestinationWithoutAccess() {
        android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        assertFalse(
                context.getSharedPreferences("slice", Context.MODE_PRIVATE)
                        .contains("automaticActivation"));
        Activity activity =
                instrumentation.startActivitySync(
                        new Intent(context, AutomaticSettingsActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            AtomicBoolean ready = new AtomicBoolean();
            long deadline = SystemClock.elapsedRealtime() + 10000;
            do {
                instrumentation.runOnMainSync(
                        () ->
                                ready.set(
                                        activity.findViewById(R.id.auto_state)
                                                instanceof
                                                androidx.compose.ui.platform.ComposeView));
                if (ready.get()) break;
                SystemClock.sleep(50);
            } while (SystemClock.elapsedRealtime() < deadline);
            assertTrue("Compose settings status host did not initialize", ready.get());
            instrumentation.runOnMainSync(
                    () -> {
                        assertFalse(activity.findViewById(R.id.auto_enable).isEnabled());
                        assertEquals(
                                0,
                                ((Spinner) activity.findViewById(R.id.auto_calendar_picker))
                                        .getSelectedItemPosition());
                        assertTrue(
                                ((CheckBox) activity.findViewById(R.id.auto_incoming)).isChecked());
                        assertTrue(
                                ((CheckBox) activity.findViewById(R.id.auto_outgoing)).isChecked());
                        assertTrue(
                                ((CheckBox) activity.findViewById(R.id.auto_missed)).isChecked());
                    });
            assertFalse(
                    context.getSharedPreferences("slice", Context.MODE_PRIVATE)
                            .contains("automaticActivation"));
        } finally {
            instrumentation.runOnMainSync(activity::finish);
        }
    }

    @Test
    public void openingImportsDoesNotCreatePreviewOrConfirmWithoutAccess() {
        android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        try (SqliteImportStore store = new SqliteImportStore(context)) {
            assertTrue(store.sessions(-1, 10).isEmpty());
        }
        Activity activity =
                instrumentation.startActivitySync(
                        new Intent(context, HistoricalImportActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            instrumentation.runOnMainSync(
                    () -> {
                        assertFalse(activity.findViewById(R.id.import_create).isEnabled());
                        assertFalse(activity.findViewById(R.id.import_confirm).isEnabled());
                        assertFalse(activity.findViewById(R.id.import_continue).isEnabled());
                        assertEquals(
                                activity.getString(R.string.import_none),
                                ((TextView) activity.findViewById(R.id.import_summary))
                                        .getText()
                                        .toString());
                    });
            try (SqliteImportStore store = new SqliteImportStore(context)) {
                assertTrue(store.sessions(-1, 10).isEmpty());
            }
        } finally {
            instrumentation.runOnMainSync(activity::finish);
        }
    }

    @Test
    public void openingRecoveryDoesNotCreateReceiptsOrEnableLogging() {
        android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        try (SqliteLedger ledger = new SqliteLedger(context)) {
            assertTrue(ledger.recoveryPage(null, 10).isEmpty());
        }
        Activity activity =
                instrumentation.startActivitySync(
                        new Intent(context, ReceiptRecoveryActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            java.util.concurrent.atomic.AtomicBoolean ready =
                    new java.util.concurrent.atomic.AtomicBoolean();
            long deadline = SystemClock.elapsedRealtime() + 10000;
            do {
                instrumentation.runOnMainSync(
                        () -> ready.set(activity.findViewById(R.id.recovery_empty) != null));
                if (ready.get()) break;
                SystemClock.sleep(50);
            } while (SystemClock.elapsedRealtime() < deadline);
            assertTrue("Recovery list did not load", ready.get());
            try (SqliteLedger ledger = new SqliteLedger(context)) {
                assertTrue(ledger.recoveryPage(null, 10).isEmpty());
            }
            assertFalse(
                    context.getSharedPreferences("slice", Context.MODE_PRIVATE)
                            .contains("automaticActivation"));
        } finally {
            instrumentation.runOnMainSync(activity::finish);
        }
    }
}
