package com.markobl.calllogsync.slice;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import com.markobl.calllogsync.R;

import java.time.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Foreground bounded import runs; opening/resuming a screen never confirms historical delivery. */
public final class HistoricalImportActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean running;
    private boolean busy;
    private String currentId, zone;
    private HistoricalImport.Session session;
    private EditText from, to;
    private Spinner destination;
    private CheckBox incoming, outgoing, missed;
    private Button create, continueRun, confirm, cancel, stop, older, newer;
    private TextView summary, feedback;
    private LinearLayout history;
    private List<AndroidCalendarSink.Choice> choices = Collections.emptyList();
    private long selected = -1, historyCursor = -1, renderVersion;
    private final List<Long> historyBack = new ArrayList<>();
    private long nextHistoryCursor = -1;

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        zone =
                saved == null
                        ? ZoneId.systemDefault().getId()
                        : saved.getString("zone", ZoneId.systemDefault().getId());
        currentId = saved == null ? null : saved.getString("session");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        TextView intro = new TextView(this);
        intro.setText(getString(R.string.import_intro, zone));
        layout.addView(intro);
        Button access = button(layout, R.string.auto_access, 0);
        access.setOnClickListener(
                v ->
                        requestPermissions(
                                new String[] {
                                    Manifest.permission.READ_CALL_LOG,
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR
                                },
                                9));
        destination = new Spinner(this);
        destination.setId(R.id.import_calendar_picker);
        destination.setContentDescription(getString(R.string.slice_choose));
        layout.addView(destination);
        destination.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        Collections.singletonList(getString(R.string.slice_choose))));
        destination.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> p, View view, int position, long id) {
                        selected =
                                position > 0 && position <= choices.size()
                                        ? choices.get(position - 1).id
                                        : -1;
                        buttons();
                    }

                    public void onNothingSelected(AdapterView<?> p) {
                        selected = -1;
                        buttons();
                    }
                });
        from =
                date(
                        layout,
                        R.string.import_from,
                        R.id.import_from,
                        saved == null
                                ? LocalDate.now(ZoneId.of(zone)).minusDays(7).toString()
                                : saved.getString("from"));
        to =
                date(
                        layout,
                        R.string.import_to,
                        R.id.import_to,
                        saved == null
                                ? LocalDate.now(ZoneId.of(zone)).plusDays(1).toString()
                                : saved.getString("to"));
        incoming =
                check(
                        layout,
                        R.string.auto_incoming,
                        saved == null || saved.getBoolean("incoming"));
        outgoing =
                check(
                        layout,
                        R.string.auto_outgoing,
                        saved == null || saved.getBoolean("outgoing"));
        missed = check(layout, R.string.auto_missed, saved == null || saved.getBoolean("missed"));
        create = button(layout, R.string.import_create, R.id.import_create);
        create.setOnClickListener(v -> startPreview());
        summary = new TextView(this);
        summary.setId(R.id.import_summary);
        summary.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        summary.setText(R.string.import_none);
        layout.addView(summary);
        continueRun = button(layout, R.string.import_continue, R.id.import_continue);
        continueRun.setOnClickListener(
                v -> {
                    running = true;
                    step();
                });
        confirm = button(layout, R.string.import_confirm, R.id.import_confirm);
        confirm.setOnClickListener(v -> confirmImport());
        stop = button(layout, R.string.import_stop, R.id.import_stop);
        stop.setOnClickListener(
                v -> {
                    running = false;
                    feedback.setText(R.string.import_stopping);
                    buttons();
                });
        cancel = button(layout, R.string.import_cancel, R.id.import_cancel);
        cancel.setOnClickListener(v -> cancelImport());
        feedback = new TextView(this);
        feedback.setId(R.id.import_feedback);
        layout.addView(feedback);
        TextView historyLabel = new TextView(this);
        historyLabel.setText(R.string.import_history);
        layout.addView(historyLabel);
        history = new LinearLayout(this);
        history.setOrientation(LinearLayout.VERTICAL);
        history.setId(R.id.import_history);
        layout.addView(history);
        newer = button(layout, R.string.import_newer, 0);
        newer.setOnClickListener(
                v -> {
                    if (!historyBack.isEmpty())
                        loadHistory(historyBack.remove(historyBack.size() - 1));
                });
        older = button(layout, R.string.import_older, 0);
        older.setOnClickListener(
                v -> {
                    if (nextHistoryCursor > 0) {
                        historyBack.add(historyCursor);
                        loadHistory(nextHistoryCursor);
                    }
                });
        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        ActivityInsets.apply(scroll);
        setContentView(scroll);
        buttons();
        loadCalendars(saved == null ? -1 : saved.getLong("calendar", -1));
        loadHistory(-1);
        if (currentId != null) loadSession(currentId);
    }

    private Button button(LinearLayout layout, int text, int id) {
        Button b = new Button(this);
        b.setText(text);
        if (id != 0) b.setId(id);
        layout.addView(b);
        return b;
    }

    private EditText date(LinearLayout layout, int label, int id, String value) {
        TextView text = new TextView(this);
        text.setText(label);
        text.setLabelFor(id);
        layout.addView(text);
        EditText field = new EditText(this);
        field.setId(id);
        field.setSingleLine(true);
        field.setInputType(
                android.text.InputType.TYPE_CLASS_DATETIME
                        | android.text.InputType.TYPE_DATETIME_VARIATION_DATE);
        field.setText(value);
        layout.addView(field);
        return field;
    }

    private CheckBox check(LinearLayout layout, int label, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setChecked(checked);
        layout.addView(box);
        box.setOnCheckedChangeListener((v, c) -> buttons());
        return box;
    }

    private boolean access() {
        return checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                        == PackageManager.PERMISSION_GRANTED
                && calendarAccess();
    }

    private boolean calendarAccess() {
        return checkSelfPermission(Manifest.permission.READ_CALENDAR)
                        == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.WRITE_CALENDAR)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private void buttons() {
        if (create != null)
            create.setEnabled(
                    !busy
                            && selected >= 0
                            && access()
                            && (incoming.isChecked()
                                    || outgoing.isChecked()
                                    || missed.isChecked()));
        if (destination != null) destination.setEnabled(!busy && calendarAccess());
        if (continueRun != null)
            continueRun.setEnabled(
                    !busy
                            && session != null
                            && (session.phase == HistoricalImport.Phase.BUILDING
                                    || session.phase == HistoricalImport.Phase.CONFIRMED));
        if (confirm != null)
            confirm.setEnabled(
                    !busy && session != null && session.phase == HistoricalImport.Phase.PREVIEW);
        if (cancel != null)
            cancel.setEnabled(
                    session != null
                            && session.phase != HistoricalImport.Phase.COMPLETE
                            && session.phase != HistoricalImport.Phase.CANCELLED);
        if (stop != null) stop.setEnabled(running);
    }

    private void loadCalendars(long preferred) {
        executor.execute(
                () -> {
                    try {
                        List<AndroidCalendarSink.Choice> loaded =
                                calendarAccess()
                                        ? new AndroidCalendarSink(getApplicationContext()).choices()
                                        : Collections.emptyList();
                        runOnUiThread(
                                () -> {
                                    if (isDestroyed()) return;
                                    choices = loaded;
                                    List<String> labels = new ArrayList<>();
                                    labels.add(getString(R.string.slice_choose));
                                    int position = 0;
                                    for (int i = 0; i < loaded.size(); i++) {
                                        labels.add(loaded.get(i).name);
                                        if (loaded.get(i).id == preferred) position = i + 1;
                                    }
                                    destination.setAdapter(
                                            new ArrayAdapter<>(
                                                    this,
                                                    android.R.layout.simple_spinner_dropdown_item,
                                                    labels));
                                    destination.setSelection(position);
                                    if (session != null) render();
                                    buttons();
                                });
                    } catch (RuntimeException failed) {
                        showFailure();
                    }
                });
    }

    private void startPreview() {
        if (busy || selected < 0) return;
        final ImportRange range;
        try {
            range = ImportRange.parse(from.getText().toString(), to.getText().toString(), zone);
        } catch (RuntimeException invalid) {
            feedback.setText(R.string.import_invalid_range);
            return;
        }
        final long calendar = selected;
        final boolean in = incoming.isChecked(),
                out = outgoing.isChecked(),
                miss = missed.isChecked();
        Runnable start =
                () -> {
                    running = true;
                    perform(
                            (importer, store) ->
                                    importer.start(
                                            calendar,
                                            range.timezone,
                                            range.from,
                                            range.to,
                                            in,
                                            out,
                                            miss),
                            true);
                };
        if (session != null
                && session.phase != HistoricalImport.Phase.COMPLETE
                && session.phase != HistoricalImport.Phase.CANCELLED)
            new AlertDialog.Builder(this)
                    .setMessage(R.string.import_another)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.import_create, (d, w) -> start.run())
                    .show();
        else start.run();
    }

    private void confirmImport() {
        if (busy || session == null || session.phase != HistoricalImport.Phase.PREVIEW) return;
        final String id = currentId;
        new AlertDialog.Builder(this)
                .setTitle(R.string.import_confirm)
                .setMessage(
                        getString(
                                R.string.import_confirm_text,
                                session.count,
                                destinationName(session),
                                dates(session)))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                        R.string.import_confirm,
                        (d, w) -> {
                            running = true;
                            perform((importer, store) -> importer.confirm(id), true);
                        })
                .show();
    }

    private void cancelImport() {
        if (session == null) return;
        final String id = currentId;
        new AlertDialog.Builder(this)
                .setMessage(R.string.import_cancel_text)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                        R.string.import_cancel,
                        (d, w) -> {
                            running = false;
                            perform((importer, store) -> importer.cancel(id), false);
                        })
                .show();
    }

    private void step() {
        if (!running || busy || currentId == null) return;
        final String id = currentId;
        perform(
                (importer, store) -> {
                    HistoricalImport.Session latest = store.get(id);
                    if (latest == null) throw new IllegalArgumentException();
                    if (latest.phase == HistoricalImport.Phase.BUILDING)
                        return importer.preview(id, 20);
                    if (latest.phase == HistoricalImport.Phase.CONFIRMED)
                        return importer.deliver(id, 20, () -> running);
                    return latest;
                },
                true);
    }

    private interface Action {
        HistoricalImport.Session run(HistoricalImport importer, SqliteImportStore store);
    }

    private void perform(Action action, boolean follow) {
        final long ticket = ++renderVersion;
        busy = true;
        buttons();
        executor.execute(
                () -> {
                    try (SqliteImportStore store = new SqliteImportStore(getApplicationContext());
                            SqliteLedger ledger = new SqliteLedger(getApplicationContext())) {
                        ledger.expirePayloads(50);
                        HistoricalImport importer =
                                new HistoricalImport(
                                        store,
                                        new AndroidCallLogSource(getApplicationContext()),
                                        ledger.outbox(),
                                        new DurableDelivery(
                                                ledger.outbox(),
                                                new AndroidCalendarSink(getApplicationContext())));
                        HistoricalImport.Session result = action.run(importer, store);
                        runOnUiThread(
                                () -> {
                                    if (isDestroyed() || ticket != renderVersion) return;
                                    busy = false;
                                    if (currentId == null
                                            || !currentId.equals(result.id)
                                            || session == null
                                            || result.revision >= session.revision) {
                                        currentId = result.id;
                                        session = result;
                                        render();
                                    }
                                    if (result.stopped != null) {
                                        running = false;
                                        feedback.setText(R.string.import_blocked);
                                    } else {
                                        // Feedback describes this result, not an earlier stopped
                                        // batch.
                                        feedback.setText("");
                                    }
                                    buttons();
                                    if (follow
                                            && running
                                            && (result.phase == HistoricalImport.Phase.BUILDING
                                                    || result.phase
                                                            == HistoricalImport.Phase.CONFIRMED))
                                        summary.post(this::step);
                                    else {
                                        running = false;
                                        buttons();
                                        loadHistory(historyCursor);
                                    }
                                });
                    } catch (RuntimeException failed) {
                        runOnUiThread(
                                () -> {
                                    if (isDestroyed() || ticket != renderVersion) return;
                                    running = false;
                                    busy = false;
                                    feedback.setText(R.string.import_failed);
                                    buttons();
                                });
                    }
                });
    }

    private void loadSession(String id) {
        if (busy) return;
        running = false;
        currentId = id;
        session = null;
        perform(
                (importer, store) -> {
                    HistoricalImport.Session found = store.get(id);
                    if (found == null) throw new IllegalArgumentException();
                    return found;
                },
                false);
    }

    private String dates(HistoricalImport.Session s) {
        ZoneId z = TimeZone.getTimeZone(s.timezone).toZoneId();
        return Instant.ofEpochMilli(s.from).atZone(z).toLocalDate()
                + " → "
                + Instant.ofEpochMilli(s.to).atZone(z).toLocalDate()
                + " ("
                + s.timezone
                + ")";
    }

    private String destinationName(HistoricalImport.Session value) {
        for (AndroidCalendarSink.Choice choice : choices)
            if (choice.id == value.calendar) return choice.name;
        return getString(R.string.auto_destination_unavailable);
    }

    private String typeNames(HistoricalImport.Session value) {
        List<String> names = new ArrayList<>();
        if (value.incoming) names.add(getString(R.string.auto_incoming));
        if (value.outgoing) names.add(getString(R.string.auto_outgoing));
        if (value.missed) names.add(getString(R.string.auto_missed));
        return android.text.TextUtils.join(", ", names);
    }

    private void render() {
        summary.setText(
                getString(
                        R.string.import_progress,
                        dates(session),
                        getString(phaseLabel(session.phase)),
                        destinationName(session),
                        typeNames(session),
                        session.count,
                        session.saved,
                        session.review));
    }

    private int phaseLabel(HistoricalImport.Phase phase) {
        switch (phase) {
            case BUILDING:
                return R.string.import_building;
            case PREVIEW:
                return R.string.import_preview;
            case CONFIRMED:
                return R.string.import_confirmed;
            case COMPLETE:
                return R.string.import_complete;
            case CANCELLED:
                return R.string.import_cancelled;
            default:
                return R.string.import_review;
        }
    }

    private void loadHistory(long before) {
        executor.execute(
                () -> {
                    try (SqliteImportStore store = new SqliteImportStore(getApplicationContext())) {
                        List<SqliteImportStore.ListedSession> rows = store.sessions(before, 11);
                        runOnUiThread(
                                () -> {
                                    if (isDestroyed()) return;
                                    historyCursor = before;
                                    history.removeAllViews();
                                    int size = Math.min(10, rows.size());
                                    for (int i = 0; i < size; i++) {
                                        HistoricalImport.Session item = rows.get(i).session;
                                        Button open = new Button(this);
                                        open.setText(
                                                getString(
                                                        R.string.import_history_item,
                                                        dates(item),
                                                        getString(phaseLabel(item.phase))));
                                        open.setOnClickListener(v -> loadSession(item.id));
                                        history.addView(open);
                                    }
                                    if (size == 0) {
                                        TextView empty = new TextView(this);
                                        empty.setText(R.string.import_empty_history);
                                        history.addView(empty);
                                    }
                                    nextHistoryCursor = rows.size() > 10 ? rows.get(9).cursor : -1;
                                    older.setEnabled(nextHistoryCursor > 0);
                                    newer.setEnabled(!historyBack.isEmpty());
                                });
                    } catch (RuntimeException failed) {
                        showFailure();
                    }
                });
    }

    private void showFailure() {
        runOnUiThread(
                () -> {
                    if (isDestroyed()) return;
                    feedback.setText(R.string.import_failed);
                    buttons();
                });
    }

    @Override
    public void onPause() {
        running = false;
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("zone", zone);
        out.putString("session", currentId);
        out.putString("from", from.getText().toString());
        out.putString("to", to.getText().toString());
        out.putLong("calendar", selected);
        out.putBoolean("incoming", incoming.isChecked());
        out.putBoolean("outgoing", outgoing.isChecked());
        out.putBoolean("missed", missed.isChecked());
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(code, permissions, grants);
        loadCalendars(selected);
        buttons();
    }

    @Override
    public void onDestroy() {
        running = false;
        executor.shutdown();
        super.onDestroy();
    }
}
