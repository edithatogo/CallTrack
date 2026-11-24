package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.test.platform.app.InstrumentationRegistry;

import com.markobl.calllogsync.R;

import org.junit.Test;

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
            RenderedUi.assertComposeRoot(instrumentation, activity);
            RenderedUi rendered = new RenderedUi(instrumentation);
            rendered.await(
                    () -> rendered.shown("auto_state", true) != null,
                    "Automatic settings status did not render");
            assertNotNull(rendered.shown("auto_intro_text", true));
            assertNotNull(rendered.shown(context.getString(R.string.auto_intro), false));
            assertNotNull(rendered.shown("auto_access_action", true));
            rendered.await(
                    () ->
                            rendered.shown("open_imports", true) != null
                                    && rendered.shown("open_recovery", true) != null,
                    "Automatic settings navigation actions did not render");
            rendered.await(
                    () ->
                            rendered.shown("auto_enable", true) != null
                                    && rendered.shown("auto_pause", true) != null
                                    && rendered.shown("auto_refresh", true) != null,
                    "Automatic settings actions did not render");
            assertFalse(rendered.shown("auto_enable", true).isEnabled());
            assertTrue(rendered.shown("auto_pause", true).isEnabled());
            assertTrue(rendered.shown("auto_refresh", true).isEnabled());
            AccessibilityNodeInfo picker = rendered.shown("auto_calendar_picker", true);
            assertNotNull(picker);
            assertEquals(
                    context.getString(R.string.slice_choose), String.valueOf(picker.getText()));
            assertFalse(picker.isEnabled());
            assertTrue(rendered.shown("auto_incoming", true).isChecked());
            assertTrue(rendered.shown("auto_outgoing", true).isChecked());
            assertTrue(rendered.shown("auto_missed", true).isChecked());
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
            RenderedUi rendered = new RenderedUi(instrumentation);
            RenderedUi.assertComposeRoot(instrumentation, activity);
            assertNotNull(rendered.shown("import_summary_text", true));
            assertFalse(rendered.shown("import_create", true).isEnabled());
            assertNotNull(rendered.shown("import_from", true));
            assertNotNull(rendered.shown("import_to", true));
            rendered.revealId("import_history_empty");
            assertNotNull(rendered.find(context.getString(R.string.import_empty_history), false));
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
            RenderedUi ui = new RenderedUi(instrumentation);
            ui.await(
                    () -> ui.shown("recovery_empty_message", true) != null,
                    "Recovery empty state did not render after the query");
            RenderedUi.assertComposeRoot(instrumentation, activity);
            ui.await(
                    () -> ui.shown("recovery_access_action", true) != null,
                    "Recovery access action did not render");
            assertEquals(
                    context.getString(R.string.recovery_empty),
                    ui.shown("recovery_empty_message", true).getText().toString());
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
