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

    private android.view.View firstVisibleFocusable(android.view.View view) {
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                android.view.View candidate = firstVisibleFocusable(group.getChildAt(i));
                if (candidate != null) return candidate;
            }
        }
        android.graphics.Rect visible = new android.graphics.Rect();
        return view.isShown()
                        && view.isEnabled()
                        && view.isFocusable()
                        && view.getGlobalVisibleRect(visible)
                        && !visible.isEmpty()
                ? view
                : null;
    }

    private android.view.View composeHost(android.view.View view) {
        if (view.getClass().getName().equals("androidx.compose.ui.platform.ComposeView")) {
            return view;
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                android.view.View candidate = composeHost(group.getChildAt(i));
                if (candidate != null) return candidate;
            }
        }
        return null;
    }

    private boolean focusComposeHost(android.view.View view) {
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        return view.requestFocus();
    }

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
                int[] rootPosition = new int[2], contentTop = new int[1], requiredTop = new int[1];
                instrumentation.runOnMainSync(
                        () -> {
                            android.view.ViewGroup content =
                                    activity.findViewById(android.R.id.content);
                            android.widget.ScrollView scroll =
                                    (android.widget.ScrollView) content.getChildAt(0);
                            // Measure the viewport's visible content edge. Its child can be
                            // scrolled above the viewport when Android auto-focuses a field.
                            scroll.getLocationOnScreen(rootPosition);
                            contentTop[0] = rootPosition[1] + scroll.getPaddingTop();
                            android.view.WindowInsets insets =
                                    activity.getWindow().getDecorView().getRootWindowInsets();
                            requiredTop[0] =
                                    android.os.Build.VERSION.SDK_INT >= 30
                                            ? insets.getInsets(
                                                            android.view.WindowInsets.Type
                                                                            .systemBars()
                                                                    | android.view.WindowInsets.Type
                                                                            .displayCutout())
                                                    .top
                                            : insets.getSystemWindowInsetTop();
                        });
                assertTrue(
                        names[i]
                                + " visible content begins at "
                                + contentTop[0]
                                + " beneath system area "
                                + requiredTop[0],
                        contentTop[0] >= requiredTop[0]);
            } finally {
                finish(instrumentation, activity);
            }
        }
    }

    @Test
    public void renderedAccessibilityLabelsTargetsAndKeyboardFocus() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        RenderedUi ui = new RenderedUi(instrumentation);
        Class<?>[] screens = {
            AutomaticSettingsActivity.class,
            CalendarSliceActivity.class,
            HistoricalImportActivity.class,
            ReceiptRecoveryActivity.class
        };
        String[] anchors = {
            "auto_access_action",
            "slice_calendar_picker",
            "import_calendar_picker",
            "recovery_refresh_action"
        };
        for (int i = 0; i < screens.length; i++) {
            Activity activity =
                    instrumentation.startActivitySync(
                            new Intent(context(), screens[i])
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            try {
                final String anchor = anchors[i];
                ui.await(() -> ui.find(anchor, true) != null, "Missing screen " + anchor);
                ui.capture("accessibility-before-" + i);
                final int[] controls = {0};
                assertRenderedAccessibilityControls(
                        ui.automation.getRootInActiveWindow(),
                        controls,
                        context().getResources().getDisplayMetrics().density);
                assertTrue("Screen has no reachable controls", controls[0] > 0);
                android.view.accessibility.AccessibilityNodeInfo node = ui.shown(anchor, true);
                assertNotNull("Anchor must be reachable by scrolling", node);
                assertTrue(
                        "Enabled anchor must expose keyboard focus "
                                + anchor
                                + " enabled="
                                + node.isEnabled(),
                        !node.isEnabled() || node.isFocusable());
                android.view.View windowRoot = activity.getWindow().getDecorView();
                final android.view.View[] initialFocus = {null};
                final boolean[] inputFocusRequested = {false};
                instrumentation.runOnMainSync(
                        () -> {
                            initialFocus[0] = composeHost(windowRoot);
                            if (initialFocus[0] != null && initialFocus[0].isShown()) {
                                inputFocusRequested[0] = focusComposeHost(initialFocus[0]);
                            }
                            if (!inputFocusRequested[0]) {
                                initialFocus[0] = firstVisibleFocusable(windowRoot);
                                inputFocusRequested[0] =
                                        initialFocus[0] != null && initialFocus[0].requestFocus();
                            }
                        });
                assertTrue(
                        "Screen has no visible focusable view to accept keyboard input; host="
                                + (initialFocus[0] == null
                                        ? "missing"
                                        : initialFocus[0].getClass().getName()
                                                + ", shown="
                                                + initialFocus[0].isShown()
                                                + ", focusable="
                                                + initialFocus[0].isFocusable()),
                        inputFocusRequested[0]);
                if (node.isEnabled())
                    node.performAction(
                            android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS);
                try (android.os.ParcelFileDescriptor ignored =
                        instrumentation
                                .getUiAutomation()
                                .executeShellCommand("input keyevent KEYCODE_TAB")) {
                    // Inject through Android's input dispatcher; Instrumentation key events do
                    // not traverse the Compose host consistently on API26 x86 emulators.
                } catch (java.io.IOException error) {
                    throw new AssertionError("Could not inject device Tab key", error);
                }
                ui.capture("accessibility-after-tab-" + i);
                ui.await(
                        () -> {
                            android.view.accessibility.AccessibilityNodeInfo root =
                                    ui.automation.getRootInActiveWindow();
                            if (root == null) return false;
                            android.view.accessibility.AccessibilityNodeInfo focused =
                                    root.findFocus(
                                            android.view.accessibility.AccessibilityNodeInfo
                                                    .FOCUS_INPUT);
                            return focused != null
                                    && focused.isFocusable()
                                    && focused.isVisibleToUser()
                                    && context()
                                            .getPackageName()
                                            .contentEquals(focused.getPackageName());
                        },
                        "Keyboard Tab did not expose visible application focus " + anchor);
                ui.capture("accessibility-" + i);
            } finally {
                finish(instrumentation, activity);
            }
        }
        assertNull("Reading screens must not enable logging", active());
    }

    private void assertRenderedAccessibilityControls(
            android.view.accessibility.AccessibilityNodeInfo node, int[] controls, float density) {
        if (node == null || !node.refresh()) return;
        if (node.isVisibleToUser() && node.isEnabled() && node.isClickable()) {
            controls[0]++;
            android.graphics.Rect bounds = new android.graphics.Rect();
            node.getBoundsInScreen(bounds);
            assertTrue(
                    "Touch target below 48dp: " + node.getViewIdResourceName(),
                    bounds.height() >= 48 * density - 1);
            assertTrue("Touch target too narrow", bounds.width() >= 48 * density - 1);
            CharSequence label = node.getContentDescription();
            if (label == null || label.length() == 0) label = node.getText();
            assertTrue(
                    "Unlabelled interactive control id="
                            + node.getViewIdResourceName()
                            + " class="
                            + node.getClassName()
                            + " text="
                            + node.getText()
                            + " description="
                            + node.getContentDescription(),
                    label != null && label.length() > 0);
            assertTrue("Enabled control must accept keyboard focus", node.isFocusable());
        }
        for (int i = 0; i < node.getChildCount(); i++)
            assertRenderedAccessibilityControls(node.getChild(i), controls, density);
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
            boolean requiresSettingsGrant = ui.permissionDialogs(true, this::allGranted);
            assertFalse(allGranted());
            assertNull(active());
            ui.capture("settings-denied");
            if (requiresSettingsGrant) {
                // Some platform dialogs expose only the permanent-denial action. Preserve
                // the rendered denial assertion, then grant through the isolated test shell
                // so the remaining settings journey can still run without faking app state.
                for (String permission :
                        new String[] {
                            Manifest.permission.READ_CALENDAR,
                            Manifest.permission.WRITE_CALENDAR,
                            Manifest.permission.READ_CALL_LOG,
                            Manifest.permission.READ_PHONE_STATE
                        }) {
                    ui.automation.grantRuntimePermission(context().getPackageName(), permission);
                }
                instrumentation.runOnMainSync(activity::recreate);
            } else {
                ui.tapId("auto_permissions");
                ui.permissionDialogs(false, this::allGranted);
            }
            ui.await(this::allGranted, "Permission grants did not persist after restoration");
            assertNull("Permission grant must not enable logging", active());
            ui.tapId("auto_calendar_picker");
            ui.tapText(fixture.calendarDisplayName());
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
                    context().getString(R.string.auto_enabled, fixture.calendarDisplayName());
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
