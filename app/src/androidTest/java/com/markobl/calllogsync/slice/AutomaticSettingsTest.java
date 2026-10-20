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
                                        ((TextView) activity.findViewById(R.id.auto_state))
                                                .getText()
                                                .toString()
                                                .equals(
                                                        activity.getString(
                                                                R.string.auto_disabled))));
                if (ready.get()) break;
                SystemClock.sleep(50);
            } while (SystemClock.elapsedRealtime() < deadline);
            assertTrue("Settings state did not finish loading", ready.get());
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
}
