package com.markobl.calllogsync.slice;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import com.markobl.calllogsync.R;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Explicit activation controls. Reopening can resume an already-enabled policy, never enable it.
 */
public final class AutomaticSettingsActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Spinner destination;
    private CheckBox incoming, outgoing, missed;
    private Button enable, pause;
    private TextView state, counts, last, feedback;
    private List<AndroidCalendarSink.Choice> choices = Collections.emptyList();
    private long selected = -1;
    private boolean busy, active, initialised, reloadRequested, reloadReconcile;

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        TextView intro = new TextView(this);
        intro.setText(R.string.auto_intro);
        layout.addView(intro);
        Button access = new Button(this);
        access.setId(R.id.auto_permissions);
        access.setText(R.string.auto_access);
        layout.addView(access);
        access.setOnClickListener(
                v ->
                        requestPermissions(
                                new String[] {
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR,
                                    Manifest.permission.READ_CALL_LOG,
                                    Manifest.permission.READ_PHONE_STATE
                                },
                                7));
        destination = new Spinner(this);
        destination.setId(R.id.auto_calendar_picker);
        destination.setContentDescription(getString(R.string.slice_choose));
        layout.addView(destination);
        destination.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        Collections.singletonList(getString(R.string.slice_choose))));
        destination.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        selected =
                                position > 0 && position <= choices.size()
                                        ? choices.get(position - 1).id
                                        : -1;
                        buttons();
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        selected = -1;
                        buttons();
                    }
                });
        incoming = check(layout, R.string.auto_incoming, R.id.auto_incoming);
        outgoing = check(layout, R.string.auto_outgoing, R.id.auto_outgoing);
        missed = check(layout, R.string.auto_missed, R.id.auto_missed);
        enable = new Button(this);
        enable.setId(R.id.auto_enable);
        enable.setText(R.string.auto_enable);
        layout.addView(enable);
        enable.setOnClickListener(v -> confirmEnable());
        pause = new Button(this);
        pause.setId(R.id.auto_pause);
        pause.setText(R.string.auto_pause);
        layout.addView(pause);
        pause.setOnClickListener(
                v -> change(() -> AutomaticScheduling.pause(getApplicationContext())));
        state = new TextView(this);
        state.setId(R.id.auto_state);
        state.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        layout.addView(state);
        counts = new TextView(this);
        counts.setId(R.id.auto_counts);
        layout.addView(counts);
        feedback = new TextView(this);
        feedback.setId(R.id.auto_feedback);
        layout.addView(feedback);
        last = new TextView(this);
        last.setId(R.id.auto_last);
        layout.addView(last);
        Button refresh = new Button(this);
        refresh.setText(R.string.auto_refresh);
        layout.addView(refresh);
        refresh.setOnClickListener(v -> load(false));
        Button imports = new Button(this);
        imports.setId(R.id.open_imports);
        imports.setText(R.string.import_open);
        imports.setOnClickListener(
                v -> startActivity(new Intent(this, HistoricalImportActivity.class)));
        layout.addView(imports);
        Button recovery = new Button(this);
        recovery.setId(R.id.open_recovery);
        recovery.setText(R.string.recovery_open);
        recovery.setOnClickListener(
                v -> startActivity(new Intent(this, ReceiptRecoveryActivity.class)));
        layout.addView(recovery);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);
        buttons();
    }

    private CheckBox check(LinearLayout layout, int text, int id) {
        CheckBox box = new CheckBox(this);
        box.setId(id);
        box.setText(text);
        box.setChecked(true);
        layout.addView(box);
        box.setOnCheckedChangeListener((button, checked) -> buttons());
        return box;
    }

    @Override
    public void onResume() {
        super.onResume();
        load(true);
    }

    private boolean calendarAccess() {
        return checkSelfPermission(Manifest.permission.READ_CALENDAR)
                        == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.WRITE_CALENDAR)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private boolean callAccess() {
        return checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void buttons() {
        if (enable != null)
            enable.setEnabled(
                    !busy
                            && selected >= 0
                            && calendarAccess()
                            && callAccess()
                            && (incoming.isChecked()
                                    || outgoing.isChecked()
                                    || missed.isChecked()));
        if (pause != null) pause.setEnabled(!busy);
        if (destination != null) destination.setEnabled(!busy && calendarAccess());
        if (incoming != null) incoming.setEnabled(!busy);
        if (outgoing != null) outgoing.setEnabled(!busy);
        if (missed != null) missed.setEnabled(!busy);
    }

    private void confirmEnable() {
        if (selected < 0 || busy) return;
        final long calendar = selected;
        final boolean in = incoming.isChecked(),
                out = outgoing.isChecked(),
                miss = missed.isChecked();
        new AlertDialog.Builder(this)
                .setTitle(R.string.auto_enable)
                .setMessage(R.string.auto_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                        R.string.auto_enable,
                        (dialog, which) ->
                                change(
                                        () -> {
                                            if (!new AndroidCalendarSink(getApplicationContext())
                                                    .writable(calendar))
                                                throw new IllegalStateException(
                                                        "Destination unavailable");
                                            AutomaticScheduling.activate(
                                                    getApplicationContext(),
                                                    calendar,
                                                    TimeZone.getDefault().getID(),
                                                    in,
                                                    out,
                                                    miss);
                                        }))
                .show();
    }

    private void change(Runnable action) {
        if (busy) return;
        busy = true;
        buttons();
        executor.execute(
                () -> {
                    boolean success = true;
                    try {
                        action.run();
                    } catch (RuntimeException failed) {
                        success = false;
                    }
                    final boolean done = success;
                    runOnUiThread(
                            () -> {
                                if (isDestroyed()) return;
                                busy = false;
                                feedback.setText(
                                        done
                                                ? R.string.auto_action_saved
                                                : R.string.auto_action_failed);
                                buttons();
                                load(false);
                            });
                });
    }

    private void load(boolean reconcile) {
        if (busy) {
            reloadRequested = true;
            reloadReconcile |= reconcile;
            return;
        }
        busy = true;
        buttons();
        executor.execute(
                () -> {
                    try {
                        ActivationControl.Settings settings =
                                new ActivationControl(
                                                new AndroidActivationStore(getApplicationContext()))
                                        .current();
                        if (reconcile && settings != null && settings.enabled)
                            AutomaticScheduling.reconcile(getApplicationContext());
                        List<AndroidCalendarSink.Choice> loaded =
                                calendarAccess()
                                        ? new AndroidCalendarSink(getApplicationContext()).choices()
                                        : Collections.emptyList();
                        DeliveryCounts summary;
                        try (SqliteLedger ledger = new SqliteLedger(getApplicationContext())) {
                            ledger.expirePayloads(50);
                            summary = ledger.counts();
                        }
                        WorkStatus worker =
                                settings == null
                                        ? null
                                        : new AndroidWorkStatusStore(getApplicationContext())
                                                .read(settings.generation);
                        String code = worker == null ? null : worker.code.name();
                        runOnUiThread(
                                () -> {
                                    if (isDestroyed()) return;
                                    busy = false;
                                    active = settings != null && settings.enabled;
                                    String activeName =
                                            getString(R.string.auto_destination_unavailable);
                                    if (settings != null)
                                        for (AndroidCalendarSink.Choice choice : loaded)
                                            if (choice.id == settings.calendar)
                                                activeName = choice.name;
                                    state.setText(
                                            active
                                                    ? getString(R.string.auto_enabled, activeName)
                                                    : getString(R.string.auto_disabled));
                                    counts.setText(
                                            getString(
                                                    R.string.auto_count_text,
                                                    summary.pending,
                                                    summary.verified,
                                                    summary.review,
                                                    summary.suppressed));
                                    last.setText(
                                            getString(
                                                    R.string.auto_status_summary,
                                                    getString(workerMessage(code)),
                                                    getString(
                                                            R.string.auto_failure_count,
                                                            worker == null ? 0 : worker.failures)));
                                    {
                                        choices = loaded;
                                        List<String> labels = new ArrayList<>();
                                        labels.add(getString(R.string.slice_choose));
                                        int position = 0;
                                        long preferred =
                                                initialised
                                                        ? selected
                                                        : settings == null
                                                                ? getSharedPreferences(
                                                                                "slice",
                                                                                MODE_PRIVATE)
                                                                        .getLong("calendar", -1)
                                                                : settings.calendar;
                                        for (int i = 0; i < loaded.size(); i++) {
                                            labels.add(loaded.get(i).name);
                                            if (loaded.get(i).id == preferred) position = i + 1;
                                        }
                                        selected = position == 0 ? -1 : loaded.get(position - 1).id;
                                        destination.setAdapter(
                                                new ArrayAdapter<>(
                                                        this,
                                                        android.R.layout
                                                                .simple_spinner_dropdown_item,
                                                        labels));
                                        destination.setSelection(position);
                                        if (!initialised && settings != null) {
                                            incoming.setChecked(settings.incoming);
                                            outgoing.setChecked(settings.outgoing);
                                            missed.setChecked(settings.missed);
                                        }
                                        // Keep edits while visible; refresh calendar choices after
                                        // a later permission grant.
                                        initialised = calendarAccess();
                                    }
                                    buttons();
                                    reloadIfRequested();
                                });
                    } catch (RuntimeException unavailable) {
                        runOnUiThread(
                                () -> {
                                    if (isDestroyed()) return;
                                    busy = false;
                                    state.setText(R.string.auto_action_failed);
                                    buttons();
                                    reloadIfRequested();
                                });
                    }
                });
    }

    private void reloadIfRequested() {
        if (reloadRequested) {
            boolean reconcile = reloadReconcile;
            reloadRequested = false;
            reloadReconcile = false;
            load(reconcile);
        }
    }

    private int workerMessage(String code) {
        if (code == null) return R.string.auto_not_run;
        switch (code) {
            case "COMPLETE":
                return R.string.auto_complete;
            case "PAUSED":
                return R.string.auto_paused;
            case "BACKLOG":
                return R.string.auto_backlog;
            case "PERMISSION_REQUIRED":
                return R.string.slice_permission_needed;
            case "DESTINATION_UNAVAILABLE":
                return R.string.slice_calendar_unavailable;
            case "SOURCE_CHANGED":
            case "REVIEW_REQUIRED":
                return R.string.slice_review;
            case "INVALID_CALL":
                return R.string.slice_invalid;
            case "FAILED":
                return R.string.auto_run_failed;
            default:
                return R.string.auto_unknown;
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(code, permissions, grants);
        initialised = false;
        load(false);
    }

    @Override
    public void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}
