package com.markobl.calllogsync.slice;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import com.markobl.calllogsync.R;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Explicit manual action; previously enabled background policy is controlled independently. */
public final class CalendarSliceActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Spinner picker;
    private TextView status;
    private Button log;
    private List<AndroidCalendarSink.Choice> choices = new ArrayList<>();
    private long selected = -1;
    private boolean busy;
    private boolean refreshing;
    private int refreshGeneration;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        TextView intro = new TextView(this);
        intro.setText(R.string.slice_intro);
        layout.addView(intro);
        Button permission = new Button(this);
        permission.setText(R.string.slice_calendar_access);
        layout.addView(permission);
        permission.setOnClickListener(
                v -> {
                    if (calendarAccess()) refresh();
                    else
                        requestPermissions(
                                new String[] {
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR
                                },
                                1);
                });
        picker = new Spinner(this);
        picker.setId(R.id.slice_calendar_picker);
        picker.setContentDescription(getString(R.string.slice_choose));
        layout.addView(picker);
        picker.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        selected =
                                position > 0 && position <= choices.size()
                                        ? choices.get(position - 1).id
                                        : -1;
                        getSharedPreferences("slice", MODE_PRIVATE)
                                .edit()
                                .putLong("calendar", selected)
                                .apply();
                        updateButton();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        selected = -1;
                        updateButton();
                    }
                });
        log = new Button(this);
        log.setText(R.string.slice_log_latest);
        layout.addView(log);
        log.setEnabled(false);
        status = new TextView(this);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        layout.addView(status);
        log.setOnClickListener(
                v -> {
                    if (selected < 0 || busy) return;
                    if (!calendarAccess()) {
                        status.setText(R.string.slice_permission_needed);
                        updateButton();
                        return;
                    }
                    if (checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                            != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[] {Manifest.permission.READ_CALL_LOG}, 2);
                        return;
                    }
                    final long destination = selected;
                    busy = true;
                    updateButton();
                    status.setText(R.string.slice_working);
                    executor.execute(
                            () -> {
                                SliceEngine.Result result;
                                try (SqliteLedger ledger =
                                        new SqliteLedger(getApplicationContext())) {
                                    result =
                                            new ManualDelivery(
                                                            new AndroidCallLogSource(
                                                                    getApplicationContext()),
                                                            new DurableDelivery(
                                                                    ledger.outbox(),
                                                                    new AndroidCalendarSink(
                                                                            getApplicationContext())),
                                                            TimeZone.getDefault().getID())
                                                    .run(destination);
                                } catch (RuntimeException e) {
                                    result = SliceEngine.Result.FAILED;
                                }
                                final SliceEngine.Result outcome = result;
                                runOnUiThread(
                                        () -> {
                                            if (isDestroyed()) return;
                                            busy = false;
                                            status.setText(message(outcome));
                                            updateButton();
                                        });
                            });
                });
        Button automatic = new Button(this);
        automatic.setId(R.id.open_automatic);
        automatic.setText(R.string.auto_open);
        automatic.setOnClickListener(
                v -> startActivity(new Intent(this, AutomaticSettingsActivity.class)));
        layout.addView(automatic);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        ActivityInsets.apply(scroll);
        setContentView(scroll);
    }

    @Override
    public void onResume() {
        super.onResume();
        executor.execute(
                () -> {
                    try {
                        AutomaticScheduling.reconcile(getApplicationContext());
                    } catch (RuntimeException unavailable) {
                        /* Settings/status exposes unavailable persisted policy; opening cannot enable it. */
                    }
                    try (SqliteLedger ledger = new SqliteLedger(getApplicationContext())) {
                        ledger.expirePayloads(50);
                    } catch (RuntimeException unavailable) {
                        /* Status/recovery exposes any storage problem. */
                    }
                });
        if (calendarAccess()) refresh();
        else {
            selected = -1;
            if (status != null) status.setText(R.string.slice_permission_needed);
            updateButton();
        }
    }

    private boolean calendarAccess() {
        return checkSelfPermission(Manifest.permission.READ_CALENDAR)
                        == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.WRITE_CALENDAR)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private void updateButton() {
        if (log != null) log.setEnabled(!busy && !refreshing && selected >= 0 && calendarAccess());
        if (picker != null) picker.setEnabled(!busy && !refreshing && calendarAccess());
    }

    private void refresh() {
        // Save before asynchronous adapter replacement; spinner callbacks must not select a
        // fallback.
        final long saved = getSharedPreferences("slice", MODE_PRIVATE).getLong("calendar", -1);
        final int generation = ++refreshGeneration;
        refreshing = true;
        updateButton();
        executor.execute(
                () -> {
                    try {
                        List<AndroidCalendarSink.Choice> loaded =
                                new AndroidCalendarSink(getApplicationContext()).choices();
                        runOnUiThread(
                                () -> {
                                    if (isDestroyed() || generation != refreshGeneration) return;
                                    refreshing = false;
                                    choices = loaded;
                                    List<String> labels = new ArrayList<>();
                                    labels.add(getString(R.string.slice_choose));
                                    int position = 0;
                                    for (int i = 0; i < loaded.size(); i++) {
                                        labels.add(loaded.get(i).name);
                                        if (loaded.get(i).id == saved) position = i + 1;
                                    }
                                    selected = position == 0 ? -1 : loaded.get(position - 1).id;
                                    picker.setAdapter(
                                            new ArrayAdapter<>(
                                                    this,
                                                    android.R.layout.simple_spinner_dropdown_item,
                                                    labels));
                                    picker.setSelection(position);
                                    status.setText(
                                            loaded.isEmpty()
                                                    ? R.string.slice_no_calendars
                                                    : R.string.slice_ready);
                                    updateButton();
                                });
                    } catch (RuntimeException e) {
                        runOnUiThread(
                                () -> {
                                    if (!isDestroyed() && generation == refreshGeneration) {
                                        refreshing = false;
                                        selected = -1;
                                        status.setText(R.string.slice_calendar_unavailable);
                                        updateButton();
                                    }
                                });
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(code, permissions, grants);
        if (code == 1 && calendarAccess()) refresh();
        else if (code == 2) status.setText(R.string.slice_tap_again);
        else status.setText(R.string.slice_permission_needed);
    }

    private int message(SliceEngine.Result result) {
        switch (result) {
            case SAVED:
                return R.string.slice_saved;
            case ALREADY_LOGGED:
                return R.string.slice_already;
            case RECOVERED:
                return R.string.slice_recovered;
            case NO_CALL:
                return R.string.slice_no_call;
            case PERMISSION_REQUIRED:
                return R.string.slice_permission_needed;
            case DESTINATION_UNAVAILABLE:
                return R.string.slice_calendar_unavailable;
            case REVIEW_REQUIRED:
                return R.string.slice_review;
            case INVALID_CALL:
                return R.string.slice_invalid;
            default:
                return R.string.slice_failed;
        }
    }

    @Override
    public void onDestroy() {
        ++refreshGeneration;
        executor.shutdown();
        super.onDestroy();
    }
}
