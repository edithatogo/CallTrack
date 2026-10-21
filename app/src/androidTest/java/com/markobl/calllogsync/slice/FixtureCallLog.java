package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.Manifest;
import android.app.UiAutomation;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.CallLog.Calls;

import androidx.test.platform.app.InstrumentationRegistry;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Test-only provider access. Old guests use shell CLI, never app write permission. */
final class FixtureCallLog {
    private static UiAutomation automation() {
        return InstrumentationRegistry.getInstrumentation().getUiAutomation();
    }

    static void asShell(Runnable action) {
        if (Build.VERSION.SDK_INT < 29) {
            assertEquals(
                    "Legacy call fixture requires an emulator",
                    "1",
                    shell("getprop ro.kernel.qemu"));
            action.run();
            return;
        }
        UiAutomation ui = automation();
        ui.adoptShellPermissionIdentity(
                Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG);
        try {
            action.run();
        } finally {
            ui.dropShellPermissionIdentity();
        }
    }

    private static String quote(String text) {
        return "'" + text.replace("'", "'\\''") + "'";
    }

    private static String shell(String command) {
        File script = null;
        try {
            // executeShellCommand tokenizes arguments; use a script so quoted values stay intact.
            File cache =
                    InstrumentationRegistry.getInstrumentation()
                            .getTargetContext()
                            .getExternalCacheDir();
            assertNotNull("Emulator fixture cache unavailable", cache);
            script = File.createTempFile("calltrack-shell-", ".sh", cache);
            assertTrue(script.getAbsolutePath().matches("[A-Za-z0-9_./-]+"));
            try (FileOutputStream file = new FileOutputStream(script)) {
                file.write(
                        (command
                                        + " 2>&1\n"
                                        + "calltrack_fixture_status=$?\n"
                                        + "printf '\\n"
                                        + "__CALLTRACK_EXIT__=%s\\n"
                                        + "' \"$calltrack_fixture_status\"\n")
                                .getBytes(StandardCharsets.UTF_8));
            }
            try (InputStream input =
                            new ParcelFileDescriptor.AutoCloseInputStream(
                                    automation()
                                            .executeShellCommand(
                                                    "sh " + script.getAbsolutePath()));
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                for (int read; (read = input.read(buffer)) != -1; ) {
                    bytes.write(buffer, 0, read);
                    assertTrue("Call fixture output exceeded limit", bytes.size() <= 1024 * 1024);
                }
                String output = new String(bytes.toByteArray(), StandardCharsets.UTF_8).trim();
                String marker = "__CALLTRACK_EXIT__=0";
                assertTrue("Call fixture shell command failed", output.endsWith(marker));
                return output.substring(0, output.length() - marker.length()).trim();
            }
        } catch (java.io.IOException error) {
            throw new AssertionError("Call fixture shell failed", error);
        } finally {
            if (script != null) assertTrue("Fixture script removal failed", script.delete());
        }
    }

    private static String where(String selection, String[] args) {
        if (selection == null) {
            assertTrue(args == null || args.length == 0);
            return "";
        }
        String result = selection;
        if (args != null)
            for (String value : args) {
                int index = result.indexOf('?');
                assertTrue("Unexpected fixture selection argument", index >= 0);
                result =
                        result.substring(0, index)
                                + "'"
                                + value.replace("'", "''")
                                + "'"
                                + result.substring(index + 1);
            }
        assertFalse("Unbound fixture selection", result.contains("?"));
        return " --where " + quote(result);
    }

    static Cursor query(
            Context context, String[] columns, String selection, String[] args, String sort) {
        if (Build.VERSION.SDK_INT >= 29)
            return context.getContentResolver()
                    .query(Calls.CONTENT_URI, columns, selection, args, sort);
        assertNotNull(columns);
        for (String column : columns)
            assertTrue("Unexpected fixture column", column.matches("[a-z_]+"));
        String output =
                shell(
                        "content query --uri content://call_log/calls --projection "
                                + quote(String.join(":", columns))
                                + where(selection, args)
                                + (sort == null ? "" : " --sort " + quote(sort)));
        MatrixCursor cursor = new MatrixCursor(columns);
        if (output.equals("No result found.")) return cursor;
        for (String row : output.split("\\r?\\n")) {
            assertTrue("Call fixture query did not return rows", row.matches("Row: \\d+ .*"));
            String[] cells = row.replaceFirst("Row: \\d+ ", "").split(", ", -1);
            assertEquals("Unexpected fixture projection", columns.length, cells.length);
            Object[] values = new Object[columns.length];
            for (int i = 0; i < columns.length; i++) {
                assertTrue(
                        "Unexpected fixture column order", cells[i].startsWith(columns[i] + "="));
                String value = cells[i].substring(columns[i].length() + 1);
                values[i] = value.equals("NULL") ? null : value;
            }
            cursor.addRow(values);
        }
        return cursor;
    }

    static Uri insert(Context context, ContentValues values) {
        if (Build.VERSION.SDK_INT >= 29)
            return context.getContentResolver().insert(Calls.CONTENT_URI, values);
        assertNotNull(
                "Only tagged call fixtures may be inserted",
                values.getAsString(Calls.PHONE_ACCOUNT_ID));
        StringBuilder command = new StringBuilder("content insert --uri content://call_log/calls");
        for (Map.Entry<String, Object> entry : values.valueSet()) {
            assertTrue(entry.getKey().matches("[a-z_]+"));
            Object value = entry.getValue();
            assertNotNull(value);
            String type = value instanceof Long ? "l" : value instanceof Number ? "i" : "s";
            command.append(" --bind ").append(quote(entry.getKey() + ":" + type + ":" + value));
        }
        String output = shell(command.toString());
        assertFalse(
                "Call fixture insert failed",
                output.contains("Error") || output.contains("Exception"));
        // Old content CLI does not consistently print the inserted URI. Read the actual tagged ID.
        try (Cursor cursor =
                query(
                        context,
                        new String[] {Calls._ID},
                        Calls.PHONE_ACCOUNT_ID + "=?",
                        new String[] {values.getAsString(Calls.PHONE_ACCOUNT_ID)},
                        Calls._ID + " DESC")) {
            assertTrue("Inserted fixture was not read back", cursor.moveToFirst());
            return android.content.ContentUris.withAppendedId(Calls.CONTENT_URI, cursor.getLong(0));
        }
    }

    static int delete(Context context, String selection, String[] args) {
        assertNotNull("Refusing unscoped fixture deletion", selection);
        assertNotNull(args);
        assertTrue(args.length > 0);
        if (Build.VERSION.SDK_INT >= 29)
            return context.getContentResolver().delete(Calls.CONTENT_URI, selection, args);
        String output =
                shell("content delete --uri content://call_log/calls" + where(selection, args));
        Matcher match = Pattern.compile("Deleted (\\d+) rows\\.?").matcher(output);
        assertTrue("Call fixture deletion unconfirmed", match.matches());
        return Integer.parseInt(match.group(1));
    }
}
