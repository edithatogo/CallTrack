package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;

/**
 * Visible accessibility selectors plus actual touch injection; never invokes app click handlers.
 */
final class RenderedUi {
    final UiAutomation automation;
    private final Context context;

    RenderedUi(Instrumentation instrumentation) {
        context = instrumentation.getTargetContext();
        automation = instrumentation.getUiAutomation();
        AccessibilityServiceInfo info = automation.getServiceInfo();
        info.flags |=
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                        | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        automation.setServiceInfo(info);
    }

    void await(BooleanSupplier condition, String message) {
        await(condition, message, 20000);
    }

    void await(BooleanSupplier condition, String message, long timeoutMillis) {
        long end = SystemClock.elapsedRealtime() + timeoutMillis;
        do {
            if (condition.getAsBoolean()) return;
            SystemClock.sleep(100);
        } while (SystemClock.elapsedRealtime() < end);
        fail(message);
    }

    AccessibilityNodeInfo find(String value, boolean id) {
        return find(automation.getRootInActiveWindow(), value, id);
    }

    private AccessibilityNodeInfo find(AccessibilityNodeInfo node, String value, boolean id) {
        if (node == null || !node.refresh()) return null;
        String actual =
                id
                        ? node.getViewIdResourceName()
                        : node.getText() == null ? null : node.getText().toString();
        if (node.isVisibleToUser()
                && actual != null
                && (id
                        ? actual.equals(value) || actual.endsWith(":id/" + value)
                        : value.equalsIgnoreCase(actual))) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = find(node.getChild(i), value, id);
            if (found != null) return found;
        }
        return null;
    }

    private AccessibilityNodeInfo scrollable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isVisibleToUser() && node.isScrollable()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = scrollable(node.getChild(i));
            if (found != null) return found;
        }
        return null;
    }

    AccessibilityNodeInfo shown(String value, boolean id) {
        for (int direction :
                new int[] {
                    AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
                    AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                }) {
            for (int i = 0; i < 8; i++) {
                AccessibilityNodeInfo node = find(value, id);
                if (node != null) return node;
                AccessibilityNodeInfo scroll = scrollable(automation.getRootInActiveWindow());
                if (scroll == null || !scroll.performAction(direction)) break;
                SystemClock.sleep(150);
            }
        }
        return null;
    }

    void setTextId(String id, String value) {
        tapId(id);
        AccessibilityNodeInfo node = shown(id, true);
        assertNotNull(node);
        android.os.Bundle text = new android.os.Bundle();
        text.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value);
        assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, text));
        await(
                () -> find(id, true) != null && value.contentEquals(find(id, true).getText()),
                "Edited text did not persist: " + id);
    }

    boolean keyboardVisible() {
        for (android.view.accessibility.AccessibilityWindowInfo window : automation.getWindows())
            if (window.getType()
                    == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD)
                return true;
        return false;
    }

    void dismissKeyboard() {
        if (!keyboardVisible()) return;
        assertTrue(
                automation.performGlobalAction(
                        android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK));
        await(() -> !keyboardVisible(), "Keyboard did not dismiss");
    }

    void revealId(String id) {
        await(() -> shown(id, true) != null, "Visible field missing: " + id);
        AccessibilityNodeInfo node = shown(id, true);
        node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.getId());
        settle();
    }

    void tapId(String id) {
        tap(id, true);
    }

    void tapText(String text) {
        tap(text, false);
    }

    /** Tap the app-owned positive dialog action across interactive windows. */
    void tapDialogPositive() {
        final AccessibilityNodeInfo[] found = new AccessibilityNodeInfo[1];
        try {
            await(
                    () -> {
                        found[0] = findDialogPositive(automation.getWindows());
                        return found[0] != null;
                    },
                    "Visible enabled positive dialog action missing");
        } catch (AssertionError missing) {
            throw new AssertionError(missing.getMessage() + "; " + dialogActionSummary(), missing);
        }
        settle();
        found[0] = findDialogPositive(automation.getWindows());
        assertNotNull("Dialog action disappeared before touch", found[0]);
        touch(found[0], "dialog_positive");
    }

    /** Select a visible permission-controller action using only known resource IDs. */
    String tapAnyId(String... ids) {
        for (String id : ids) {
            AccessibilityNodeInfo node = shown(id, true);
            if (node != null && node.isEnabled()) {
                tapId(id);
                return id;
            }
        }
        tapId(ids[0]); // Preserve the usual bounded diagnostic when no known ID is present.
        return ids[0];
    }

    void tapIdWhileBusy(String id) {
        tap(id, true, false);
    }

    private void tap(String value, boolean id) {
        tap(value, id, true);
    }

    private void tap(String value, boolean id, boolean waitForIdle) {
        final AccessibilityNodeInfo[] found = new AccessibilityNodeInfo[1];
        try {
            await(
                    () -> {
                        found[0] = waitForIdle ? shown(value, id) : find(value, id);
                        return found[0] != null && found[0].isEnabled();
                    },
                    "Visible enabled control missing: " + value);
        } catch (AssertionError missing) {
            if (!value.startsWith("permission_")) throw missing;
            throw new AssertionError(missing.getMessage() + "; " + permissionControlSummary());
        }
        if (waitForIdle) settle();
        else SystemClock.sleep(50); // A user can stop work while its progress keeps changing.
        found[0] = waitForIdle ? shown(value, id) : find(value, id);
        assertNotNull("Control disappeared before touch", found[0]);
        touch(found[0], value);
    }

    private AccessibilityNodeInfo findDialogPositive(
            java.util.List<android.view.accessibility.AccessibilityWindowInfo> windows) {
        for (android.view.accessibility.AccessibilityWindowInfo window : windows) {
            AccessibilityNodeInfo found = findDialogPositive(window.getRoot());
            if (found != null) return found;
        }
        return null;
    }

    private AccessibilityNodeInfo findDialogPositive(AccessibilityNodeInfo node) {
        if (node == null || !node.refresh() || !node.isVisibleToUser()) return null;
        CharSequence owner = node.getPackageName();
        CharSequence resourceId = node.getViewIdResourceName();
        if (owner != null
                && context.getPackageName().contentEquals(owner)
                && resourceId != null
                && resourceId.toString().endsWith(":id/button1")
                && node.isClickable()
                && node.isEnabled()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findDialogPositive(node.getChild(i));
            if (found != null) return found;
        }
        return null;
    }

    private String dialogActionSummary() {
        AccessibilityNodeInfo root = automation.getRootInActiveWindow();
        String activeOwner = packageCategory(root == null ? null : root.getPackageName());
        java.util.List<String> windows = new java.util.ArrayList<>();
        for (android.view.accessibility.AccessibilityWindowInfo window : automation.getWindows()) {
            AccessibilityNodeInfo windowRoot = window.getRoot();
            String owner = packageCategory(windowRoot == null ? null : windowRoot.getPackageName());
            String type =
                    window.getType()
                                    == android.view.accessibility.AccessibilityWindowInfo
                                            .TYPE_APPLICATION
                            ? "application"
                            : window.getType()
                                            == android.view.accessibility.AccessibilityWindowInfo
                                                    .TYPE_INPUT_METHOD
                                    ? "input-method"
                                    : "other";
            java.util.List<String> controls = new java.util.ArrayList<>();
            collectDialogControls(windowRoot, controls);
            windows.add(owner + ":" + type + ":" + controls);
            if (windows.size() >= 8) break;
        }
        return "dialog-active="
                + activeOwner
                + " dialog-windows="
                + windows
                + " app-permissions="
                + appPermissionState()
                + permissionPurposeSummary();
    }

    private String permissionPurposeSummary() {
        AccessibilityNodeInfo root = automation.getRootInActiveWindow();
        if (root == null || root.getPackageName() == null) return "";
        if (!permissionPackage(root.getPackageName().toString())) return "";
        AccessibilityNodeInfo message = find("permission_message", true);
        CharSequence value = message == null ? null : message.getText();
        if (value == null) return " permission-purpose=unavailable";
        String text = value.toString().toLowerCase(java.util.Locale.ROOT);
        String purpose =
                text.contains("call log")
                        ? "call-log"
                        : text.contains("calendar")
                                ? "calendar"
                                : text.contains("phone")
                                        ? "phone-state"
                                        : text.contains("contacts")
                                                ? "contacts"
                                                : text.contains("location")
                                                        ? "location"
                                                        : text.contains("camera")
                                                                ? "camera"
                                                                : text.contains("microphone")
                                                                        ? "microphone"
                                                                        : text.contains(
                                                                                        "notification")
                                                                                ? "notifications"
                                                                                : "other";
        return " permission-purpose=" + purpose;
    }

    private String appPermissionState() {
        String[] names = {"calendar-read", "calendar-write", "call-log-read", "phone-state-read"};
        String[] permissions = {
            android.Manifest.permission.READ_CALENDAR,
            android.Manifest.permission.WRITE_CALENDAR,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.READ_PHONE_STATE
        };
        java.util.List<String> states = new java.util.ArrayList<>();
        for (int i = 0; i < permissions.length; i++)
            states.add(
                    names[i]
                            + "="
                            + (context.checkSelfPermission(permissions[i])
                                            == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    ? "granted"
                                    : "denied"));
        return states.toString();
    }

    private String packageCategory(CharSequence owner) {
        if (owner == null) return "unavailable";
        if (context.getPackageName().contentEquals(owner)) return "app";
        if (permissionPackage(owner.toString())) return "permission";
        if ("com.android.systemui".contentEquals(owner)) return "system-ui";
        if (owner.toString().contains("inputmethod")) return "input-method";
        return "other";
    }

    private void collectDialogControls(
            AccessibilityNodeInfo node, java.util.List<String> controls) {
        if (node == null || controls.size() >= 12) return;
        CharSequence packageName = node.getPackageName();
        CharSequence resourceId = node.getViewIdResourceName();
        String id = resourceId == null ? "" : resourceId.toString();
        String control =
                id.endsWith(":id/button1")
                        ? "button1"
                        : id.endsWith(":id/button2")
                                ? "button2"
                                : id.endsWith(":id/button3")
                                        ? "button3"
                                        : knownPermissionControl(id);
        boolean appOwner =
                packageName != null && context.getPackageName().contentEquals(packageName);
        boolean permissionOwner = packageName != null && permissionPackage(packageName.toString());
        if ((appOwner || permissionOwner)
                && node.isVisibleToUser()
                && node.isClickable()
                && !control.equals("other")) {
            String widget =
                    "android.widget.Button".contentEquals(node.getClassName())
                            ? "android.widget.Button"
                            : "other";
            controls.add(
                    control + ":" + widget + ":enabled=" + node.isEnabled() + ":clickable=true");
        }
        for (int i = 0; i < node.getChildCount() && controls.size() < 12; i++)
            collectDialogControls(node.getChild(i), controls);
    }

    private String knownPermissionControl(String id) {
        for (String value :
                new String[] {
                    "permission_allow_button",
                    "permission_allow_one_time_button",
                    "permission_allow_foreground_only_button",
                    "permission_deny_button",
                    "permission_deny_and_dont_ask_again_button"
                }) if (id.endsWith(":id/" + value)) return value;
        return "other";
    }

    private void touch(AccessibilityNodeInfo node, String selector) {
        String packageName = String.valueOf(node.getPackageName());
        assertTrue(
                "Refusing a control in an unexpected package: " + packageName,
                packageName.equals(context.getPackageName())
                        || selector.startsWith("permission_") && permissionPackage(packageName));
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        assertFalse(bounds.isEmpty());
        long at = SystemClock.uptimeMillis();
        MotionEvent down =
                MotionEvent.obtain(
                        at, at, MotionEvent.ACTION_DOWN, bounds.centerX(), bounds.centerY(), 0);
        MotionEvent up =
                MotionEvent.obtain(
                        at, at + 40, MotionEvent.ACTION_UP, bounds.centerX(), bounds.centerY(), 0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try {
            assertTrue(automation.injectInputEvent(down, true));
            assertTrue(automation.injectInputEvent(up, true));
        } finally {
            down.recycle();
            up.recycle();
        }
        SystemClock.sleep(250);
    }

    private String permissionControlSummary() {
        AccessibilityNodeInfo root = automation.getRootInActiveWindow();
        if (root == null || root.getPackageName() == null) return "permission-root=unavailable";
        java.util.List<String> controls = new java.util.ArrayList<>();
        collectPermissionControls(root, controls);
        return "permission-root=" + root.getPackageName() + " controls=" + controls;
    }

    private void collectPermissionControls(
            AccessibilityNodeInfo node, java.util.List<String> controls) {
        if (node == null || controls.size() >= 16) return;
        CharSequence owner = node.getPackageName();
        CharSequence resourceId = node.getViewIdResourceName();
        if (owner != null
                && permissionPackage(owner.toString())
                && node.isVisibleToUser()
                && node.isClickable()
                && resourceId != null) {
            controls.add(resourceId + ":" + node.getClassName() + ":enabled=" + node.isEnabled());
        }
        for (int i = 0; i < node.getChildCount() && controls.size() < 16; i++)
            collectPermissionControls(node.getChild(i), controls);
    }

    private boolean permissionPackage(String owner) {
        return owner.equals("com.google.android.permissioncontroller")
                || owner.equals("com.android.permissioncontroller")
                || android.os.Build.VERSION.SDK_INT < 29
                        && (owner.equals("com.google.android.packageinstaller")
                                || owner.equals("com.android.packageinstaller"));
    }

    private String permissionMessage() {
        AccessibilityNodeInfo root = automation.getRootInActiveWindow();
        if (root == null
                || root.getPackageName() == null
                || !permissionPackage(root.getPackageName().toString())) return null;
        AccessibilityNodeInfo message = find("permission_message", true);
        return message == null || message.getText() == null ? "" : message.getText().toString();
    }

    boolean permissionDialogs(boolean denyFirst, BooleanSupplier complete) {
        long deadline = SystemClock.elapsedRealtime() + 45000;
        int handled = 0;
        boolean requiresSettingsGrant = false;
        while (SystemClock.elapsedRealtime() < deadline) {
            String before = permissionMessage();
            if (before != null) {
                boolean changed = false;
                for (int attempt = 0; attempt < 3 && !changed; attempt++) {
                    capture("permission-" + (denyFirst ? "deny" : "allow") + "-" + handled);
                    if (!denyFirst && permissionMessage() == null && complete.getAsBoolean()) {
                        capture("permission-restored-without-dialog");
                        return false;
                    }
                    if (denyFirst && handled == 0) {
                        String selected =
                                tapAnyId(
                                        "permission_deny_button",
                                        "permission_deny_and_dont_ask_again_button");
                        requiresSettingsGrant = selected.contains("and_dont_ask_again");
                    } else {
                        tapId("permission_allow_button");
                    }
                    long transition = SystemClock.elapsedRealtime() + 3000;
                    do {
                        if (!java.util.Objects.equals(before, permissionMessage())) {
                            changed = true;
                            break;
                        }
                        SystemClock.sleep(100);
                    } while (SystemClock.elapsedRealtime() < transition);
                }
                assertTrue("Permission choice did not advance the actual dialog", changed);
                handled++;
            } else if (handled > 0 || !denyFirst && complete.getAsBoolean()) {
                settle();
                AccessibilityNodeInfo root = automation.getRootInActiveWindow();
                if (root != null
                        && root.getPackageName() != null
                        && context.getPackageName().contentEquals(root.getPackageName())
                        && permissionMessage() == null) return requiresSettingsGrant;
            } else SystemClock.sleep(100);
        }
        throw new AssertionError("Permission dialog sequence did not complete");
    }

    private void settle() {
        try {
            automation.waitForIdle(700, 5000);
        } catch (java.util.concurrent.TimeoutException busy) {
            throw new AssertionError("UI did not settle before touch/capture", busy);
        }
        SystemClock.sleep(200);
    }

    void capture(String name) {
        settle();
        try {
            File directory = new File(context.getFilesDir(), "ui-evidence");
            assertTrue(directory.isDirectory() || directory.mkdirs());
            String unique = name;
            for (int suffix = 1; new File(directory, unique + ".png").exists(); suffix++)
                unique = name + "-" + suffix;
            Bitmap image = automation.takeScreenshot();
            assertNotNull("Rendered screenshot unavailable", image);
            try (FileOutputStream stream =
                    new FileOutputStream(new File(directory, unique + ".png"))) {
                assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, stream));
            } finally {
                image.recycle();
            }
            StringBuilder tree = new StringBuilder();
            describe(automation.getRootInActiveWindow(), tree, 0);
            try (FileOutputStream stream =
                    new FileOutputStream(new File(directory, unique + ".txt"))) {
                stream.write(tree.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception error) {
            throw new AssertionError("UI evidence failed", error);
        }
    }

    private void describe(AccessibilityNodeInfo node, StringBuilder out, int depth) {
        if (node == null) return;
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        out.append(depth)
                .append(' ')
                .append(node.getPackageName())
                .append(' ')
                .append(node.getViewIdResourceName())
                .append(" visible=")
                .append(node.isVisibleToUser())
                .append(" enabled=")
                .append(node.isEnabled())
                .append(" clickable=")
                .append(node.isClickable())
                .append(" focusable=")
                .append(node.isFocusable())
                .append(" checked=")
                .append(node.isChecked())
                .append(' ')
                .append(bounds)
                .append(' ')
                .append(node.getText())
                .append(" description=")
                .append(node.getContentDescription())
                .append('\n');
        for (int i = 0; i < node.getChildCount(); i++) describe(node.getChild(i), out, depth + 1);
    }
}
