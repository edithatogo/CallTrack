package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.*;

import androidx.compose.ui.platform.ComposeView;
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
            assertTrue(activity.findViewById(R.id.auto_intro_panel) instanceof ComposeView);
            assertTrue(activity.findViewById(R.id.auto_permissions) instanceof ComposeView);
            assertTrue(
                    activity.findViewById(R.id.auto_calendar_picker_panel) instanceof ComposeView);
            assertTrue(activity.findViewById(R.id.auto_actions) instanceof ComposeView);
            assertTrue(activity.findViewById(R.id.auto_navigation) instanceof ComposeView);
            assertTrue(activity.findViewById(R.id.auto_call_types) instanceof ComposeView);
            RenderedUi rendered = new RenderedUi(instrumentation);
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
            assertEquals(
                    context.getString(R.string.slice_choose),
                    rendered.shown("auto_calendar_picker", true).getContentDescription());
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
            assertTrue(
                    activity.findViewById(R.id.import_summary)
                            instanceof androidx.compose.ui.platform.ComposeView);
            assertTrue(
                    activity.findViewById(R.id.import_actions)
                            instanceof androidx.compose.ui.platform.ComposeView);
            assertTrue(
                    activity.findViewById(R.id.import_filters)
                            instanceof androidx.compose.ui.platform.ComposeView);
            assertTrue(
                    activity.findViewById(R.id.import_history)
                            instanceof androidx.compose.ui.platform.ComposeView);
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
            assertTrue(activity.findViewById(R.id.recovery_empty) instanceof ComposeView);
            assertTrue(activity.findViewById(R.id.recovery_access_panel) instanceof ComposeView);
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
