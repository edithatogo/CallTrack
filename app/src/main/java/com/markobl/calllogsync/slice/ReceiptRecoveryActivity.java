package com.markobl.calllogsync.slice;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.widget.*;

import com.markobl.calllogsync.R;

import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Existing-effect checks are separate from explicitly confirmed never-attempted insertion. */
public final class ReceiptRecoveryActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<SqliteLedger.RecoveryCursor> back = new ArrayList<>();
    private SqliteLedger.RecoveryCursor current, next;
    private LinearLayout rows;
    private TextView feedback;
    private Button previous, more;
    private boolean busy;
    private volatile boolean resumed;
    private long version;

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        TextView intro = new TextView(this);
        intro.setText(R.string.recovery_intro);
        layout.addView(intro);
        Button access = button(layout, R.string.recovery_access);
        access.setOnClickListener(
                v -> requestPermissions(new String[] {Manifest.permission.READ_CALENDAR}, 11));
        Button refresh = button(layout, R.string.recovery_refresh);
        refresh.setId(R.id.recovery_refresh);
        refresh.setOnClickListener(
                v -> {
                    if (!busy) {
                        back.clear();
                        load(null);
                    }
                });
        TextView retention = new TextView(this);
        retention.setText(R.string.retention_policy);
        layout.addView(retention);
        Button expire = button(layout, R.string.retention_run);
        expire.setOnClickListener(v -> expireOldPayloads());
        feedback = new TextView(this);
        feedback.setId(R.id.recovery_feedback);
        feedback.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);
        layout.addView(feedback);
        rows = new LinearLayout(this);
        rows.setId(R.id.recovery_rows);
        rows.setOrientation(LinearLayout.VERTICAL);
        layout.addView(rows);
        previous = button(layout, R.string.recovery_previous);
        previous.setOnClickListener(
                v -> {
                    if (!busy && !back.isEmpty()) load(back.remove(back.size() - 1));
                });
        more = button(layout, R.string.recovery_more);
        more.setOnClickListener(
                v -> {
                    if (!busy && next != null) {
                        back.add(current);
                        load(next);
                    }
                });
        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        ActivityInsets.apply(scroll);
        setContentView(scroll);
        load(null);
    }

    private Button button(LinearLayout layout, int label) {
        Button b = new Button(this);
        b.setText(label);
        layout.addView(b);
        return b;
    }

    private void load(SqliteLedger.RecoveryCursor cursor) {
        if (busy) return;
        busy = true;
        final long ticket = ++version;
        navigation();
        executor.execute(
                () -> {
                    try (SqliteLedger ledger = new SqliteLedger(getApplicationContext())) {
                        ledger.expirePayloads(50);
                        List<SqliteLedger.RecoveryItem> page = ledger.recoveryPage(cursor, 11);
                        Map<Long, String> labels = new HashMap<>();
                        AndroidCalendarSink sink = new AndroidCalendarSink(getApplicationContext());
                        for (SqliteLedger.RecoveryItem item : page)
                            if (!labels.containsKey(item.intent.calendar)) {
                                String label = getString(R.string.recovery_calendar_hidden);
                                if (checkSelfPermission(Manifest.permission.READ_CALENDAR)
                                        == PackageManager.PERMISSION_GRANTED) {
                                    try {
                                        String found = sink.calendarLabel(item.intent.calendar);
                                        label =
                                                found == null
                                                        ? getString(
                                                                R.string
                                                                        .auto_destination_unavailable)
                                                        : found;
                                    } catch (RuntimeException unavailable) {
                                        label = getString(R.string.recovery_calendar_unverified);
                                    }
                                }
                                labels.put(item.intent.calendar, label);
                            }
                        runOnUiThread(
                                () -> {
                                    if (isDestroyed() || ticket != version) return;
                                    busy = false;
                                    current = cursor;
                                    rows.removeAllViews();
                                    int size = Math.min(10, page.size());
                                    for (int i = 0; i < size; i++)
                                        card(
                                                page.get(i).intent,
                                                labels.get(page.get(i).intent.calendar));
                                    if (size == 0) {
                                        TextView empty = new TextView(this);
                                        empty.setId(R.id.recovery_empty);
                                        empty.setText(R.string.recovery_empty);
                                        rows.addView(empty);
                                    }
                                    next = page.size() > 10 ? page.get(9).cursor : null;
                                    navigation();
                                });
                    } catch (RuntimeException failed) {
                        runOnUiThread(
                                () -> {
                                    if (isDestroyed() || ticket != version) return;
                                    busy = false;
                                    feedback.setText(R.string.recovery_failed);
                                    navigation();
                                });
                    }
                });
    }

    private void navigation() {
        if (previous != null) previous.setEnabled(!busy && !back.isEmpty());
        if (more != null) more.setEnabled(!busy && next != null);
    }

    private void card(DurableDelivery.Intent intent, String calendar) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        card.setPadding(0, pad, 0, pad);
        TextView detail = new TextView(this);
        String source = getString(R.string.recovery_source_unknown);
        if (intent.facts != null) {
            int type =
                    intent.facts.type == 1
                            ? R.string.auto_incoming
                            : intent.facts.type == 2
                                    ? R.string.auto_outgoing
                                    : intent.facts.type == 3
                                            ? R.string.auto_missed
                                            : R.string.recovery_call;
            source =
                    getString(
                            R.string.recovery_source,
                            getString(type),
                            DateFormat.getDateTimeInstance().format(new Date(intent.facts.start)),
                            intent.facts.seconds);
        }
        detail.setText(
                getString(
                        R.string.recovery_item,
                        source,
                        calendar,
                        getString(stateLabel(intent.state))));
        card.addView(detail);
        Button check = button(card, R.string.recovery_check);
        check.setEnabled(intent.state != DurableDelivery.State.SUPPRESSED);
        check.setOnClickListener(
                v -> {
                    if (!busy) action(intent, false);
                });
        Button suppress = button(card, R.string.recovery_suppress);
        suppress.setEnabled(intent.state != DurableDelivery.State.SUPPRESSED);
        suppress.setOnClickListener(
                v -> {
                    if (busy) return;
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.recovery_suppress_confirm)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(
                                    R.string.recovery_suppress, (d, w) -> action(intent, true))
                            .show();
                });
        if (ReadyReapproval.eligible(intent)) {
            Button approve = button(card, R.string.reapproval_open);
            approve.setOnClickListener(v -> confirmReapproval(intent, calendar));
        }
        if (intent.eventId >= 0) {
            Button open = button(card, R.string.recovery_open_event);
            open.setOnClickListener(
                    v -> {
                        try {
                            startActivity(
                                    new Intent(
                                            Intent.ACTION_VIEW,
                                            ContentUris.withAppendedId(
                                                    CalendarContract.Events.CONTENT_URI,
                                                    intent.eventId)));
                        } catch (RuntimeException unavailable) {
                            feedback.setText(R.string.recovery_open_failed);
                        }
                    });
        }
        rows.addView(card);
    }

    private void expireOldPayloads() {
        if (busy) return;
        busy = true;
        navigation();
        executor.execute(
                () -> {
                    try (SqliteLedger ledger = new SqliteLedger(getApplicationContext())) {
                        int count = ledger.expirePayloads(50);
                        runOnUiThread(
                                () -> {
                                    if (isDestroyed()) return;
                                    busy = false;
                                    feedback.setText(getString(R.string.retention_result, count));
                                    load(current);
                                });
                    } catch (RuntimeException failed) {
                        runOnUiThread(
                                () -> {
                                    if (isDestroyed()) return;
                                    busy = false;
                                    feedback.setText(R.string.recovery_failed);
                                    navigation();
                                });
                    }
                });
    }

    private void confirmReapproval(DurableDelivery.Intent intent, String calendar) {
        if (busy || !ReadyReapproval.eligible(intent)) return;
        if (checkSelfPermission(Manifest.permission.READ_CALENDAR)
                        != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.WRITE_CALENDAR)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[] {
                        Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR
                    },
                    12);
            feedback.setText(R.string.reapproval_tap_again);
            return;
        }
        String preview =
                getString(
                        R.string.reapproval_preview,
                        calendar,
                        intent.payload.title,
                        intent.payload.description,
                        DateFormat.getDateTimeInstance().format(new Date(intent.payload.start)),
                        DateFormat.getDateTimeInstance().format(new Date(intent.payload.end)));
        new AlertDialog.Builder(this)
                .setTitle(R.string.reapproval_open)
                .setMessage(preview)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                        R.string.reapproval_confirm, (dialog, which) -> reapprove(intent))
                .show();
    }

    private void reapprove(DurableDelivery.Intent expected) {
        if (busy) return;
        busy = true;
        navigation();
        executor.execute(
                () -> {
                    DurableDelivery.Result result;
                    try (SqliteLedger ledger = new SqliteLedger(getApplicationContext())) {
                        result =
                                new ReadyReapproval(
                                                ledger.outbox(),
                                                new AndroidCalendarSink(getApplicationContext()))
                                        .approve(expected, () -> resumed);
                    } catch (RuntimeException failed) {
                        result = DurableDelivery.Result.FAILED;
                    }
                    final DurableDelivery.Result outcome = result;
                    runOnUiThread(
                            () -> {
                                if (isDestroyed()) return;
                                busy = false;
                                int label;
                                switch (outcome) {
                                    case SAVED:
                                    case RECOVERED:
                                    case ALREADY_LOGGED:
                                        label = R.string.reapproval_verified;
                                        break;
                                    case PERMISSION_REQUIRED:
                                        label = R.string.reapproval_tap_again;
                                        break;
                                    case DESTINATION_UNAVAILABLE:
                                        label = R.string.slice_calendar_unavailable;
                                        break;
                                    case SOURCE_CHANGED:
                                    case REVIEW_REQUIRED:
                                        label = R.string.reapproval_changed;
                                        break;
                                    case PAUSED:
                                        label = R.string.reapproval_paused;
                                        break;
                                    default:
                                        label = R.string.reapproval_failed;
                                }
                                feedback.setText(label);
                                load(current);
                            });
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        resumed = true;
    }

    @Override
    public void onPause() {
        resumed = false;
        super.onPause();
    }

    private void action(DurableDelivery.Intent intent, boolean suppress) {
        if (busy) return;
        busy = true;
        navigation();
        executor.execute(
                () -> {
                    ReceiptRecovery.Result result;
                    try (SqliteLedger ledger = new SqliteLedger(getApplicationContext())) {
                        AndroidCalendarSink sink = new AndroidCalendarSink(getApplicationContext());
                        ReceiptRecovery recovery =
                                new ReceiptRecovery(
                                        ledger.outbox(),
                                        new ReceiptRecovery.ExistingEvents() {
                                            public SliceEngine.StoredEvent read(long id) {
                                                return sink.read(id);
                                            }

                                            public List<SliceEngine.StoredEvent> find(
                                                    long calendar, String marker) {
                                                return sink.find(calendar, marker);
                                            }
                                        });
                        result =
                                suppress
                                        ? recovery.suppress(intent.key, intent.calendar)
                                        : recovery.reconcile(intent.key, intent.calendar);
                    } catch (RuntimeException failed) {
                        result = ReceiptRecovery.Result.FAILED;
                    }
                    final ReceiptRecovery.Result outcome = result;
                    runOnUiThread(
                            () -> {
                                if (isDestroyed()) return;
                                busy = false;
                                feedback.setText(resultLabel(outcome));
                                load(current);
                            });
                });
    }

    private int stateLabel(DurableDelivery.State state) {
        switch (state) {
            case READY:
                return R.string.recovery_ready;
            case EXPIRED_READY:
                return R.string.recovery_expired;
            case IN_FLIGHT:
                return R.string.recovery_inflight;
            case ATTEMPTED:
                return R.string.recovery_attempted;
            case CONFIRMED:
                return R.string.recovery_confirmed;
            case REVIEW:
                return R.string.recovery_review;
            case SUPPRESSED:
                return R.string.recovery_suppressed;
            default:
                return R.string.recovery_legacy;
        }
    }

    private int resultLabel(ReceiptRecovery.Result result) {
        switch (result) {
            case VERIFIED:
                return R.string.recovery_verified;
            case NEVER_ATTEMPTED:
                return R.string.recovery_never_attempted;
            case SUPPRESSED:
                return R.string.recovery_suppressed_result;
            case PERMISSION_REQUIRED:
                return R.string.recovery_permission;
            case REVIEW_REQUIRED:
                return R.string.recovery_review_result;
            case NOT_FOUND:
                return R.string.recovery_not_found;
            default:
                return R.string.recovery_failed;
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(code, permissions, grants);
        if (!busy) load(current);
    }

    @Override
    public void onDestroy() {
        resumed = false;
        version++;
        executor.shutdown();
        super.onDestroy();
    }
}
