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
        if (node == null) return null;
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

    void tapIdWhileBusy(String id) {
        tap(id, true, false);
    }

    private void tap(String value, boolean id) {
        tap(value, id, true);
    }

    private void tap(String value, boolean id, boolean waitForIdle) {
        final AccessibilityNodeInfo[] found = new AccessibilityNodeInfo[1];
        await(
                () -> {
                    found[0] = shown(value, id);
                    return found[0] != null && found[0].isEnabled();
                },
                "Visible enabled control missing: " + value);
        if (waitForIdle) settle();
        else SystemClock.sleep(50); // A user can stop work while its progress keeps changing.
        found[0] = shown(value, id);
        assertNotNull("Control disappeared before touch", found[0]);
        String packageName = String.valueOf(found[0].getPackageName());
        assertTrue(
                "Refusing a control in an unexpected package: " + packageName,
                packageName.equals(context.getPackageName())
                        || value.startsWith("permission_") && permissionPackage(packageName));
        Rect bounds = new Rect();
        found[0].getBoundsInScreen(bounds);
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

    void permissionDialogs(boolean denyFirst, BooleanSupplier complete) {
        long deadline = SystemClock.elapsedRealtime() + 45000;
        int handled = 0;
        while (SystemClock.elapsedRealtime() < deadline) {
            String before = permissionMessage();
            if (before != null) {
                boolean changed = false;
                for (int attempt = 0; attempt < 3 && !changed; attempt++) {
                    capture("permission-" + (denyFirst ? "deny" : "allow") + "-" + handled);
                    if (!denyFirst && permissionMessage() == null && complete.getAsBoolean()) {
                        capture("permission-restored-without-dialog");
                        return;
                    }
                    tapId(
                            denyFirst && handled == 0
                                    ? "permission_deny_button"
                                    : "permission_allow_button");
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
                        && permissionMessage() == null) return;
            } else SystemClock.sleep(100);
        }
        fail("Permission dialog sequence did not complete");
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
                .append(" checked=")
                .append(node.isChecked())
                .append(' ')
                .append(bounds)
                .append(' ')
                .append(node.getText())
                .append('\n');
        for (int i = 0; i < node.getChildCount(); i++) describe(node.getChild(i), out, depth + 1);
    }
}
