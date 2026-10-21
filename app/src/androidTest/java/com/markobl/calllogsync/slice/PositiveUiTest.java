package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.WorkManager;

import com.markobl.calllogsync.R;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

/** Visible settings interactions on a token-owned local calendar, with no real calls. */
public class PositiveUiTest {
    private final ParityProviderTest fixture = new ParityProviderTest();

    private Context context() {
        return fixture.context();
    }

    private SharedPreferences state() {
        return context().getSharedPreferences(fixture.account() + "-ui", Context.MODE_PRIVATE);
    }

    private ActivationControl.Settings active() {
        return new ActivationControl(new AndroidActivationStore(context())).current();
    }

    private boolean allGranted() {
        for (String p :
                new String[] {
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_PHONE_STATE
                })
            if (context().checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) return false;
        return true;
    }

    private Activity open(Instrumentation instrumentation) {
        return instrumentation.startActivitySync(
                new Intent(context(), AutomaticSettingsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private void finish(Instrumentation instrumentation, Activity activity) {
        if (activity != null) instrumentation.runOnMainSync(activity::finish);
    }

    @Test
    public void prepareCalendarWithoutCallAccess() {
        assertNull(active());
        assertTrue(state().edit().putLong("calendar", fixture.createCalendar()).commit());
    }

    @Test
    public void renderedActivitiesRespectSystemBars() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        RenderedUi ui = new RenderedUi(instrumentation);
        Class<?>[] activities = {
            AutomaticSettingsActivity.class,
            CalendarSliceActivity.class,
            HistoricalImportActivity.class,
            ReceiptRecoveryActivity.class
        };
        String[] ids = {
            "auto_permissions",
            "slice_calendar_picker",
            "import_calendar_picker",
            "recovery_refresh"
        };
        String[] names = {"settings", "manual", "imports", "recovery"};
        for (int i = 0; i < activities.length; i++) {
            Activity activity =
                    instrumentation.startActivitySync(
                            new Intent(context(), activities[i])
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            final String id = ids[i];
            try {
                ui.await(() -> ui.find(id, true) != null, "Screen missing: " + names[i]);
                ui.capture("system-bars-" + names[i]);
                int[] firstPosition = new int[2], requiredTop = new int[1];
                instrumentation.runOnMainSync(
                        () -> {
                            android.view.ViewGroup content =
                                    activity.findViewById(android.R.id.content);
                            android.widget.ScrollView scroll =
                                    (android.widget.ScrollView) content.getChildAt(0);
                            android.view.ViewGroup layout =
                                    (android.view.ViewGroup) scroll.getChildAt(0);
                            layout.getChildAt(0).getLocationOnScreen(firstPosition);
                            requiredTop[0] =
                                    activity.getWindow()
                                            .getDecorView()
                                            .getRootWindowInsets()
                                            .getInsets(
                                                    android.view.WindowInsets.Type.systemBars()
                                                            | android.view.WindowInsets.Type
                                                                    .displayCutout())
                                            .top;
                        });
                assertTrue(
                        names[i]
                                + " first content begins at "
                                + firstPosition[1]
                                + " beneath system area "
                                + requiredTop[0],
                        firstPosition[1] >= requiredTop[0]);
            } finally {
                finish(instrumentation, activity);
            }
        }
    }

    @Test
    public void renderedPermissionsFiltersConfirmPauseAndReopen() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        RenderedUi ui = new RenderedUi(instrumentation);
        Activity activity = null;
        try {
            activity = open(instrumentation);
            ui.await(() -> ui.find("auto_permissions", true) != null, "Settings not rendered");
            ui.capture("settings-before-access");
            assertFalse(allGranted());
            assertNull(active());
            ui.tapId("auto_permissions");
            ui.permissionDialogs(true, this::allGranted);
            assertFalse(allGranted());
            assertNull(active());
            ui.capture("settings-denied");
            ui.tapId("auto_permissions");
            ui.permissionDialogs(false, this::allGranted);
            ui.await(this::allGranted, "Visible permission grants did not persist");
            assertNull("Permission grant must not enable logging", active());
            ui.tapId("auto_calendar_picker");
            ui.tapText("CallTrack parity fixture");
            for (String filter : new String[] {"auto_incoming", "auto_outgoing", "auto_missed"}) {
                ui.tapId(filter);
                assertFalse(ui.find(filter, true).isChecked());
            }
            assertFalse(ui.find("auto_enable", true).isEnabled());
            ui.capture("settings-no-filters");
            ui.tapId("auto_incoming");
            ui.tapId("auto_missed");
            ui.tapId("auto_enable");
            ui.capture("settings-enable-preview");
            ui.tapId("button2");
            assertNull(active());
            assertEquals(0, fixture.events(state().getLong("calendar", -1)));
            ui.tapId("auto_enable");
            ui.tapId("button1");
            ui.await(() -> active() != null && active().enabled, "Enable not committed");
            ActivationControl.Settings enabled = active();
            assertEquals(state().getLong("calendar", -1), enabled.calendar);
            assertTrue(enabled.incoming);
            assertFalse(enabled.outgoing);
            assertTrue(enabled.missed);
            String enabledText =
                    context().getString(R.string.auto_enabled, "CallTrack parity fixture");
            ui.await(
                    () -> ui.find(enabledText, false) != null,
                    "Enabled state not visibly rendered");
            ui.capture("settings-enabled");
            ui.tapId("auto_pause");
            ui.await(
                    () ->
                            !active().enabled
                                    && ui.find(context().getString(R.string.auto_disabled), false)
                                            != null,
                    "Pause not rendered/committed");
            ui.capture("settings-paused");
            finish(instrumentation, activity);
            activity = open(instrumentation);
            ui.await(
                    () -> ui.find("auto_outgoing", true) != null, "Reopened settings not rendered");
            assertFalse(active().enabled);
            assertEquals(enabled.generation, active().generation);
            ui.await(
                    () ->
                            ui.find("auto_outgoing", true) != null
                                    && !ui.find("auto_outgoing", true).isChecked(),
                    "Reopened filter selection changed");
            ui.capture("settings-reopened-paused");
            assertEquals(0, fixture.events(enabled.calendar));
        } catch (Throwable failure) {
            try {
                ui.capture("settings-failure");
            } catch (Throwable capture) {
                failure.addSuppressed(capture);
            }
            throw failure;
        } finally {
            finish(instrumentation, activity);
        }
    }

    @Test
    public void revokedCalendarAndVisibleRestoreDoNotEnableLogging() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        RenderedUi ui = new RenderedUi(instrumentation);
        Activity activity = null;
        try {
            activity = open(instrumentation);
            ui.await(() -> ui.find("auto_enable", true) != null, "Settings missing");
            assertFalse(ui.find("auto_enable", true).isEnabled());
            assertFalse(active().enabled);
            ui.capture("settings-calendar-revoked");
            ui.tapId("auto_permissions");
            ui.permissionDialogs(false, this::allGranted);
            ui.await(this::allGranted, "Calendar access not restored through UI");
            assertFalse(active().enabled);
            ui.capture("settings-restored-still-paused");
            assertEquals(0, fixture.events(state().getLong("calendar", -1)));
        } catch (Throwable failure) {
            try {
                ui.capture("restore-failure");
            } catch (Throwable capture) {
                failure.addSuppressed(capture);
            }
            throw failure;
        } finally {
            finish(instrumentation, activity);
        }
    }

    @Test
    public void cleanupFixture() throws Exception {
        new ActivationControl(new AndroidActivationStore(context())).pause();
        WorkManager.getInstance(context())
                .cancelAllWorkByTag(AutomaticScheduling.TAG)
                .getResult()
                .get(15, TimeUnit.SECONDS);
        fixture.cleanupFixture();
    }
}
