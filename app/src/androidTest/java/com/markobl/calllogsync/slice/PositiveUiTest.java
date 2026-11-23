package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.SystemClock;
import android.provider.CallLog.Calls;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.WorkManager;

import com.markobl.calllogsync.R;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

/** Visible settings interactions on a token-owned local calendar, with no real calls. */
public class PositiveUiTest {
    private final ParityProviderTest fixture = new ParityProviderTest();

    private android.view.accessibility.AccessibilityNodeInfo firstEnabledFocusableControl(
            android.view.accessibility.AccessibilityNodeInfo node) {
        if (node == null || !node.refresh() || !node.isVisibleToUser()) return null;
        if (node.isEnabled()
                && node.isFocusable()
                && node.isClickable()
                && context().getPackageName().contentEquals(node.getPackageName())) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            android.view.accessibility.AccessibilityNodeInfo match =
                    firstEnabledFocusableControl(node.getChild(i));
            if (match != null) return match;
        }
        return null;
    }

    private void assertComposeScreen(
            Instrumentation instrumentation, Activity activity, int... ids) {
        instrumentation.runOnMainSync(() -> {
            for (int id : ids) {
                assertTrue(
                        "Missing Compose host " + context().getResources().getResourceEntryName(id),
                        activity.findViewById(id)
                                instanceof androidx.compose.ui.platform.ComposeView);
            }
            assertNoLegacyContent(activity.findViewById(android.R.id.content));
        });
    }

    private static void assertNoLegacyContent(android.view.View view) {
        if (view instanceof androidx.compose.ui.platform.ComposeView) return;
        assertFalse("Legacy adapter widget remains", view instanceof android.widget.AdapterView);
        assertTrue(
                "Legacy content remains: " + view.getClass().getSimpleName(),
                view instanceof android.view.ViewGroup);
        android.view.ViewGroup group = (android.view.ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) assertNoLegacyContent(group.getChildAt(i));
    }

    private boolean treeHasMovedFocus(AccessibilityNodeInfo node, AccessibilityNodeInfo initial) {
        // A focused descendant of the original control is not a focus transition.
        if (node == null || !node.refresh() || initial.equals(node)) return false;
        if (node.isFocused()
                && node.isFocusable()
                && node.isVisibleToUser()
                && node.getPackageName() != null
                && context().getPackageName().contentEquals(node.getPackageName())) return true;
        for (int i = 0; i < node.getChildCount(); i++) {
            if (treeHasMovedFocus(node.getChild(i), initial)) return true;
        }
        return false;
    }

    private String focusDiagnostic(RenderedUi ui, AccessibilityNodeInfo initial) {
        AccessibilityNodeInfo root = ui.automation.getRootInActiveWindow();
        AccessibilityNodeInfo node =
                root == null ? null : root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        String kind = "none", owner = "none";
        boolean focused = false, focusable = false, visible = false;
        if (node != null) {
            node.refresh();
            String name = String.valueOf(node.getClassName());
            if (name.equals("android.view.View")) kind = "view";
            else if (name.equals("android.view.ViewGroup")) kind = "group";
            else if (name.equals("android.widget.ScrollView")) kind = "scroll";
            else if (name.equals("android.widget.TextView")) kind = "text";
            else if (name.equals("android.widget.Button")) kind = "button";
            else kind = "other";
            owner =
                    node.getPackageName() != null
                                    && context()
                                            .getPackageName()
                                            .contentEquals(node.getPackageName())
                            ? "app"
                            : "foreign";
            focused = node.isFocused();
            focusable = node.isFocusable();
            visible = node.isVisibleToUser();
        }
        return " focus-node="
                + kind
                + ":"
                + owner
                + ":"
                + focused
                + ":"
                + focusable
                + ":"
                + visible
                + " focus-tree-moved="
                + treeHasMovedFocus(root, initial);
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
    public void renderedManualCalendarChoiceRequiresAnExplicitTap() {
        Context context = context();
        SharedPreferences preferences = context.getSharedPreferences("slice", Context.MODE_PRIVATE);
        long previous = preferences.getLong("calendar", -1);
        long expected = state().getLong("calendar", -1);
        assertTrue("Prepared synthetic calendar is required", expected > 0);
        assertTrue(preferences.edit().putLong("calendar", Long.MAX_VALUE).commit());
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        RenderedUi ui = new RenderedUi(instrumentation);
        Activity[] activity = {null};
        try {
            activity[0] =
                    instrumentation.startActivitySync(
                            new Intent(context, CalendarSliceActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            ui.await(
                    () -> {
                        android.view.accessibility.AccessibilityNodeInfo picker =
                                ui.find("slice_calendar_picker", true);
                        return picker != null && picker.isEnabled();
                    },
                    "Manual destination picker did not load local calendar choices");
            ui.await(
                    () -> ui.find("slice_intro", true) != null,
                    "Manual Compose intro identifier is missing");
            ui.await(
                    () -> ui.find("slice_calendar_controls_panel", true) != null,
                    "Manual Compose calendar-controls identifier is missing");
            ui.await(
                    () -> ui.find("slice_manual_panel", true) != null,
                    "Manual Compose logging-panel identifier is missing");
            ui.await(
                    () -> ui.find("open_automatic", true) != null,
                    "Manual Compose automatic-settings action is missing");
            android.view.ViewGroup content = activity[0].findViewById(android.R.id.content);
            assertTrue(content.getChildAt(0) instanceof android.widget.ScrollView);
            android.widget.ScrollView viewport = (android.widget.ScrollView) content.getChildAt(0);
            assertEquals("Manual screen has one Compose content root", 1, viewport.getChildCount());
            assertTrue(
                    "Manual screen presentation must use a Compose root",
                    viewport.getChildAt(0) instanceof androidx.compose.ui.platform.ComposeView);
            android.view.accessibility.AccessibilityNodeInfo before =
                    ui.find("slice_calendar_picker", true);
            assertNotNull(before);
            assertEquals(
                    "A provider read must not select or persist a destination",
                    -1,
                    preferences.getLong("calendar", -1));
            AccessibilityNodeInfo log = ui.shown("slice_log_latest", true);
            assertNotNull("Manual logging action is missing", log);
            assertFalse("A missing saved destination must leave logging disabled", log.isEnabled());
            ui.tapId("slice_calendar_access");
            ui.await(
                    () -> {
                        AccessibilityNodeInfo picker = ui.find("slice_calendar_picker", true);
                        return picker != null && picker.isEnabled();
                    },
                    "Manual destination picker did not finish refreshing");
            assertEquals(
                    "Permission and refresh alone must not deliver", 0, fixture.events(expected));
            ui.tapVisibleId("slice_calendar_picker");
            ui.tapVisibleText(fixture.calendarDisplayName());
            assertEquals(
                    "Only the explicit option tap persists the provider ID",
                    expected,
                    preferences.getLong("calendar", -1));
            ui.await(
                    () -> ui.find(fixture.calendarDisplayName(), false) != null,
                    "Selected local calendar is not shown by the manual picker");
            assertEquals(
                    "Destination selection alone must not deliver", 0, fixture.events(expected));
        } finally {
            if (activity[0] != null) instrumentation.runOnMainSync(activity[0]::finish);
            assertTrue(preferences.edit().putLong("calendar", previous).commit());
        }
    }

    @Test
    public void renderedManualDeliverySurvivesRecreationWithoutRepeating() {
        Context context = context();
        long calendar = state().getLong("calendar", -1);
        assertTrue("Prepared synthetic calendar is required", calendar > 0);
        assertNull("Automatic delivery must be inactive for the manual fixture", active());
        SharedPreferences preferences = context.getSharedPreferences("slice", Context.MODE_PRIVATE);
        long previous = preferences.getLong("calendar", -1);
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        RenderedUi ui = new RenderedUi(instrumentation);
        try {
            FixtureCallLog.asShell(() -> {
                try (Cursor rows =
                        FixtureCallLog.query(context, new String[] {Calls._ID}, null, null, null)) {
                    assertNotNull(rows);
                    assertEquals(
                            "Manual fixture requires an empty emulator call log",
                            0,
                            rows.getCount());
                }
                assertTrue(state().edit().putBoolean("manualCallPending", true).commit());
                ContentValues row = new ContentValues();
                row.put(Calls.NUMBER, "0000000000");
                row.put(Calls.DATE, System.currentTimeMillis());
                row.put(Calls.DURATION, 7);
                row.put(Calls.TYPE, Calls.OUTGOING_TYPE);
                row.put(Calls.NUMBER_PRESENTATION, Calls.PRESENTATION_ALLOWED);
                row.put(Calls.PHONE_ACCOUNT_ID, manualCallTag());
                assertNotNull(FixtureCallLog.insert(context, row));
            });
            assertTrue(preferences.edit().putLong("calendar", calendar).commit());
            try (ActivityScenario<CalendarSliceActivity> scenario =
                    ActivityScenario.launch(CalendarSliceActivity.class)) {
                awaitManualSelection(ui);
                assertEquals(
                        "Opening with a saved destination must not deliver",
                        0,
                        fixture.events(calendar));
                ui.tapId("slice_log_latest");
                ui.await(
                        () -> ui.find(context.getString(R.string.slice_saved), false) != null,
                        "Explicit manual delivery did not report success");
                assertEquals("One tap must produce one event", 1, fixture.events(calendar));

                scenario.recreate();
                awaitManualSelection(ui);
                assertEquals(calendar, preferences.getLong("calendar", -1));
                assertEquals("Recreation must not repeat delivery", 1, fixture.events(calendar));
                ui.capture("manual-recreated-single-event");
            }
        } finally {
            try {
                cleanManualCall();
            } finally {
                assertTrue(preferences.edit().putLong("calendar", previous).commit());
            }
        }
    }

    private void awaitManualSelection(RenderedUi ui) {
        ui.await(
                () -> {
                    AccessibilityNodeInfo action = ui.find("slice_log_latest", true);
                    return action != null
                            && action.isEnabled()
                            && ui.find(fixture.calendarDisplayName(), false) != null;
                },
                "Manual launcher did not restore its exact saved destination");
    }

    private String manualCallTag() {
        return fixture.account() + "-manual";
    }

    private void cleanManualCall() {
        if (!state().getBoolean("manualCallPending", false)) return;
        FixtureCallLog.asShell(() -> {
            String where = Calls.PHONE_ACCOUNT_ID + "=?";
            String[] args = {manualCallTag()};
            FixtureCallLog.delete(context(), where, args);
            try (Cursor remaining =
                    FixtureCallLog.query(context(), new String[] {Calls._ID}, where, args, null)) {
                assertNotNull(remaining);
                assertEquals("Owned manual call survived cleanup", 0, remaining.getCount());
            }
        });
        assertTrue(state().edit().remove("manualCallPending").commit());
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
                instrumentation.runOnMainSync(() -> {
                    android.view.ViewGroup content = activity.findViewById(android.R.id.content);
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
                                                    android.view.WindowInsets.Type.systemBars()
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
            HistoricalImportActivity.class,
            ReceiptRecoveryActivity.class,
            CalendarSliceActivity.class
        };
        String[] anchors = {
            "auto_access_action",
            "import_calendar_picker",
            "recovery_refresh_action",
            "slice_calendar_picker"
        };
        for (int i = 0; i < screens.length; i++) {
            Activity activity =
                    instrumentation.startActivitySync(
                            new Intent(context(), screens[i])
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            try {
                final String anchor = anchors[i];
                ui.await(() -> ui.find(anchor, true) != null, "Missing screen " + anchor);
                final boolean[] windowFocused = {false};
                ui.await(
                        () -> {
                            instrumentation.runOnMainSync(
                                    () -> windowFocused[0] = activity.hasWindowFocus());
                            return windowFocused[0];
                        },
                        "Screen window did not receive keyboard input focus " + anchor);
                if (activity instanceof HistoricalImportActivity) {
                    assertComposeScreen(
                            instrumentation,
                            activity,
                            R.id.import_intro_panel,
                            R.id.import_access_panel,
                            R.id.import_calendar_picker_panel,
                            R.id.import_filters,
                            R.id.import_actions,
                            R.id.import_summary,
                            R.id.import_feedback,
                            R.id.import_history);
                } else if (activity instanceof ReceiptRecoveryActivity) {
                    assertComposeScreen(
                            instrumentation,
                            activity,
                            R.id.recovery_intro_panel,
                            R.id.recovery_access_panel,
                            R.id.recovery_refresh,
                            R.id.recovery_retention_copy,
                            R.id.recovery_retention,
                            R.id.recovery_feedback,
                            R.id.recovery_empty,
                            R.id.recovery_cards,
                            R.id.recovery_navigation);
                    assertNotNull(activity.findViewById(R.id.recovery_rows));
                }
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
                android.view.accessibility.AccessibilityNodeInfo focusTarget = node;
                if (!node.isEnabled()) {
                    focusTarget =
                            firstEnabledFocusableControl(ui.automation.getRootInActiveWindow());
                }
                assertNotNull(
                        "Screen has no enabled focusable control for keyboard acceptance " + anchor,
                        focusTarget);
                assertTrue(
                        "The initial keyboard target must accept focus " + anchor,
                        focusTarget.isFocused()
                                || focusTarget.performAction(
                                        android.view.accessibility.AccessibilityNodeInfo
                                                .ACTION_FOCUS));
                final android.view.accessibility.AccessibilityNodeInfo selectedTarget = focusTarget;
                ui.await(
                        () -> {
                            instrumentation.runOnMainSync(
                                    () -> windowFocused[0] = activity.hasWindowFocus());
                            return windowFocused[0]
                                    && selectedTarget.refresh()
                                    && selectedTarget.isFocused();
                        },
                        "Initial keyboard focus was not applied " + anchor);
                long now = SystemClock.uptimeMillis();
                KeyEvent down =
                        new KeyEvent(
                                now,
                                now,
                                KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_TAB,
                                0,
                                0,
                                KeyCharacterMap.VIRTUAL_KEYBOARD,
                                0,
                                0,
                                InputDevice.SOURCE_KEYBOARD);
                KeyEvent up =
                        new KeyEvent(
                                now,
                                now,
                                KeyEvent.ACTION_UP,
                                KeyEvent.KEYCODE_TAB,
                                0,
                                0,
                                KeyCharacterMap.VIRTUAL_KEYBOARD,
                                0,
                                0,
                                InputDevice.SOURCE_KEYBOARD);
                boolean downAccepted = ui.automation.injectInputEvent(down, true);
                boolean upAccepted = ui.automation.injectInputEvent(up, true);
                assertTrue("Android did not accept the Tab key down", downAccepted);
                assertTrue("Android did not accept the Tab key up", upAccepted);
                ui.capture("accessibility-after-tab-" + i);
                final String[] focusState = {"no-root"};
                try {
                    ui.await(
                            () -> {
                                android.view.accessibility.AccessibilityNodeInfo root =
                                        ui.automation.getRootInActiveWindow();
                                if (root == null) {
                                    focusState[0] = "no-root";
                                    return false;
                                }
                                if (treeHasMovedFocus(root, selectedTarget)) {
                                    focusState[0] = "moved";
                                    return true;
                                }
                                android.view.accessibility.AccessibilityNodeInfo focused =
                                        root.findFocus(
                                                android.view.accessibility.AccessibilityNodeInfo
                                                        .FOCUS_INPUT);
                                if (focused == null) focusState[0] = "none";
                                else if (!focused.refresh()) focusState[0] = "stale";
                                else if (selectedTarget.equals(focused)) focusState[0] = "initial";
                                else if (!focused.isFocused()) focusState[0] = "not-focused";
                                else if (!focused.isFocusable()) focusState[0] = "not-focusable";
                                else if (!focused.isVisibleToUser()) focusState[0] = "not-visible";
                                else if (focused.getPackageName() == null
                                        || !context()
                                                .getPackageName()
                                                .contentEquals(focused.getPackageName()))
                                    focusState[0] = "foreign";
                                else focusState[0] = "not-in-tree";
                                return false;
                            },
                            "Keyboard Tab did not expose visible application focus " + anchor);
                } catch (AssertionError failure) {
                    throw new AssertionError(
                            "Keyboard Tab did not expose visible application focus "
                                    + anchor
                                    + " focus-state="
                                    + focusState[0]
                                    + focusDiagnostic(ui, selectedTarget),
                            failure);
                }
                ui.capture("accessibility-" + i);
            } finally {
                finish(instrumentation, activity);
            }
        }
        assertNull("Reading screens must not enable logging", active());
    }

    static void assertRenderedAccessibilityControls(
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
        SharedPreferences manual = context().getSharedPreferences("slice", Context.MODE_PRIVATE);
        long prior = manual.getLong("calendar", -1);
        assertTrue(manual.edit().putLong("calendar", state().getLong("calendar", -1)).commit());
        try (ActivityScenario<CalendarSliceActivity> scenario =
                ActivityScenario.launch(CalendarSliceActivity.class)) {
            ui.await(
                    () ->
                            ui.find(context().getString(R.string.slice_permission_needed), false)
                                    != null,
                    "Manual launcher did not report revoked calendar access");
            AccessibilityNodeInfo action = ui.shown("slice_log_latest", true);
            assertNotNull("Manual logging action is missing", action);
            assertFalse("Revoked calendar access must disable manual delivery", action.isEnabled());
        } finally {
            assertTrue(manual.edit().putLong("calendar", prior).commit());
        }
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
        cleanManualCall();
        new ActivationControl(new AndroidActivationStore(context())).pause();
        WorkManager.getInstance(context())
                .cancelAllWorkByTag(AutomaticScheduling.TAG)
                .getResult()
                .get(15, TimeUnit.SECONDS);
        fixture.cleanupFixture();
    }
}
