package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.CallLog.Calls;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.*;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

/** Dedicated emulator only. Real providers/worker with explicitly tagged synthetic call rows. */
public class WorkerProviderTest {
    private final ParityProviderTest fixture = new ParityProviderTest();

    private Context context() {
        return fixture.context();
    }

    private String tag() {
        return fixture.account() + "-worker";
    }

    private SharedPreferences state() {
        return context().getSharedPreferences(tag(), Context.MODE_PRIVATE);
    }

    private void shellCalls(Runnable action) {
        FixtureCallLog.asShell(action);
    }

    @Test
    public void shellFixtureRoundTripDoesNotGrantAppWritePermission() {
        assertEquals(
                android.content.pm.PackageManager.PERMISSION_DENIED,
                context().checkSelfPermission(Manifest.permission.WRITE_CALL_LOG));
        shellCalls(
                () -> {
                    try (Cursor before =
                            FixtureCallLog.query(
                                    context(), new String[] {Calls._ID}, null, null, null)) {
                        assertEquals("Refusing a nonempty source", 0, before.getCount());
                    }
                    try {
                        ContentValues call = new ContentValues();
                        call.put(Calls.NUMBER, "0000000000");
                        call.put(Calls.CACHED_NAME, "Fixture O'Brien");
                        call.put(Calls.DATE, System.currentTimeMillis());
                        call.put(Calls.DURATION, 1);
                        call.put(Calls.TYPE, Calls.INCOMING_TYPE);
                        call.put(Calls.PHONE_ACCOUNT_ID, tag());
                        assertNotNull(FixtureCallLog.insert(context(), call));
                        try (Cursor row =
                                FixtureCallLog.query(
                                        context(),
                                        new String[] {Calls._ID, Calls.CACHED_NAME},
                                        Calls.PHONE_ACCOUNT_ID + "=?",
                                        new String[] {tag()},
                                        null)) {
                            assertEquals(1, row.getCount());
                            assertTrue(row.moveToFirst());
                            assertEquals("Fixture O'Brien", row.getString(1));
                        }
                        if (android.os.Build.VERSION.SDK_INT < 29)
                            assertEquals(
                                    android.content.pm.PackageManager.PERMISSION_DENIED,
                                    context()
                                            .checkSelfPermission(
                                                    Manifest.permission.WRITE_CALL_LOG));
                    } finally {
                        FixtureCallLog.delete(
                                context(), Calls.PHONE_ACCOUNT_ID + "=?", new String[] {tag()});
                        try (Cursor after =
                                FixtureCallLog.query(
                                        context(),
                                        new String[] {Calls._ID},
                                        Calls.PHONE_ACCOUNT_ID + "=?",
                                        new String[] {tag()},
                                        null)) {
                            assertEquals(0, after.getCount());
                        }
                    }
                });
        assertEquals(
                android.content.pm.PackageManager.PERMISSION_DENIED,
                context().checkSelfPermission(Manifest.permission.WRITE_CALL_LOG));
    }

    private void insertCalls(int first, int last, long at) {
        shellCalls(
                () -> {
                    for (int i = first; i <= last; i++) {
                        ContentValues call = new ContentValues();
                        call.put(Calls.NUMBER, "0000000000");
                        call.put(Calls.CACHED_NAME, "Synthetic Worker Fixture");
                        call.put(Calls.DATE, at);
                        call.put(Calls.DURATION, i % 4 == 3 ? 0 : 45);
                        call.put(Calls.TYPE, i % 4 == 0 ? Calls.BLOCKED_TYPE : i % 4);
                        call.put(Calls.NUMBER_PRESENTATION, Calls.PRESENTATION_ALLOWED);
                        call.put(Calls.PHONE_ACCOUNT_ID, tag());
                        assertNotNull(FixtureCallLog.insert(context(), call));
                    }
                });
    }

    @Test
    public void prepareTaggedCallsAndExplicitActivation() {
        long calendar = fixture.createCalendar();
        // A clean dedicated emulator is required; never treat unrelated rows as fixtures.
        shellCalls(
                () -> {
                    try (Cursor c =
                            FixtureCallLog.query(
                                    context(), new String[] {Calls._ID}, null, null, null)) {
                        assertNotNull(c);
                        assertEquals(
                                "Refusing a nonempty call-log fixture environment",
                                0,
                                c.getCount());
                    }
                });
        long at = System.currentTimeMillis();
        assertTrue(state().edit().putLong("calendar", calendar).putLong("sourceAt", at).commit());
        ActivationControl control = new ActivationControl(new AndroidActivationStore(context()));
        assertNull(control.current());
        control.activate(calendar, "UTC", at, true, false, true);
        insertCalls(
                1, 1, at - 1000); // Before activation; excluded even though incoming is enabled.
        insertCalls(1, 125, at);
        assertEquals(0, fixture.events(calendar));
    }

    private WorkInfo execute(ActivationControl.Settings settings) throws Exception {
        WorkManager manager = WorkManager.getInstance(context());
        OneTimeWorkRequest request = AutomaticScheduling.prompt(settings);
        manager.enqueueUniqueWork(tag(), ExistingWorkPolicy.REPLACE, request)
                .getResult()
                .get(15, TimeUnit.SECONDS);
        // Native diagnostic at60s showed61 verified/1 pending and RUNNING, not a fixed latency SLA.
        long deadline = SystemClock.elapsedRealtime() + 120000;
        WorkInfo last = null;
        do {
            WorkInfo info = manager.getWorkInfoById(request.getId()).get(5, TimeUnit.SECONDS);
            last = info;
            if (info != null && info.getState().isFinished()) return info;
            SystemClock.sleep(50);
        } while (SystemClock.elapsedRealtime() < deadline);
        String observed = last == null ? "missing" : last.getState().name();
        int attempts = last == null ? -1 : last.getRunAttemptCount();
        try (SqliteLedger ledger = new SqliteLedger(context())) {
            DeliveryCounts counts = ledger.counts();
            throw new AssertionError(
                    "Worker did not finish in the bounded observation window; state="
                            + observed
                            + "; attempts="
                            + attempts
                            + "; verified="
                            + counts.verified
                            + "; pending="
                            + counts.pending
                            + "; review="
                            + counts.review);
        }
    }

    @Test
    public void promptWorkerCatchesUpFiltersRepeatsPausesAndReactivates() throws Exception {
        assertEquals(
                PackageManager.PERMISSION_GRANTED,
                context().checkSelfPermission(Manifest.permission.READ_CALL_LOG));
        for (String permission :
                new String[] {
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_CALL_LOG
                }) {
            assertEquals(
                    PackageManager.PERMISSION_DENIED, context().checkSelfPermission(permission));
        }
        ActivationControl control = new ActivationControl(new AndroidActivationStore(context()));
        ActivationControl.Settings settings = control.current();
        long calendar = state().getLong("calendar", -1);
        long requestedAt = SystemClock.elapsedRealtime();
        WorkInfo first = execute(settings);
        long verifiedAt = System.currentTimeMillis();
        assertEquals(WorkInfo.State.SUCCEEDED, first.getState());
        assertEquals("COMPLETE", first.getOutputData().getString("status"));
        assertEquals(63, first.getOutputData().getInt("delivered", -1));
        assertEquals(0, first.getOutputData().getInt("review", -1));
        assertEquals(63, fixture.events(calendar));
        assertEquals(NetworkType.NOT_REQUIRED, first.getConstraints().getRequiredNetworkType());
        try (SqliteLedger ledger = new SqliteLedger(context())) {
            PagedCalls.Checkpoint checkpoint = ledger.checkpoint(settings.operation());
            assertEquals(checkpoint.upperId, checkpoint.lastId);
            assertEquals(63, ledger.counts().verified);
            assertEquals(0, ledger.counts().pending);
            assertEquals(0, ledger.counts().review);
        }
        WorkStatus status = new AndroidWorkStatusStore(context()).read(settings.generation);
        assertEquals(WorkStatus.Code.COMPLETE, status.code);
        assertEquals(63, status.delivered);
        Bundle observation = new Bundle();
        observation.putLong(
                "requestToVerifiedObservationMillis", SystemClock.elapsedRealtime() - requestedAt);
        observation.putLong(
                "sourceToVerifiedObservationMillis", verifiedAt - state().getLong("sourceAt", 0));
        observation.putLong("workerPublishedAt", status.at);
        observation.putInt("verifiedEvents", 63);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, observation);
        WorkInfo repeat = execute(settings);
        assertEquals(WorkInfo.State.SUCCEEDED, repeat.getState());
        assertEquals(0, repeat.getOutputData().getInt("delivered", -1));
        assertEquals(63, fixture.events(calendar));
        control.pause();
        insertCalls(126, 128, System.currentTimeMillis());
        assertEquals("PAUSED", execute(settings).getOutputData().getString("status"));
        assertEquals(63, fixture.events(calendar));
        // A new activation boundary excludes all the rows inserted while paused.
        long nextAt = System.currentTimeMillis() + 1;
        while (System.currentTimeMillis() < nextAt) SystemClock.sleep(1);
        ActivationControl.Settings next =
                control.activate(calendar, "UTC", nextAt, false, true, false);
        insertCalls(
                129, 131, System.currentTimeMillis()); // Incoming/outgoing/missed: only outgoing.
        WorkInfo reactivated = execute(next);
        assertEquals(WorkInfo.State.SUCCEEDED, reactivated.getState());
        assertEquals(1, reactivated.getOutputData().getInt("delivered", -1));
        assertEquals(64, fixture.events(calendar));
        try (SqliteLedger ledger = new SqliteLedger(context())) {
            assertEquals(64, ledger.counts().verified);
        }
    }

    @Test
    public void singleNewCallHasMeasuredPromptDeliveryAndOverlappingRequestsDoNotDuplicate()
            throws Exception {
        ActivationControl.Settings settings =
                new ActivationControl(new AndroidActivationStore(context())).current();
        assertEquals(64, fixture.events(settings.calendar));
        long sourceAt = System.currentTimeMillis();
        insertCalls(134, 134, sourceAt); // A single outgoing row under the current policy.
        long requestedAt = SystemClock.elapsedRealtime();
        WorkInfo info = execute(settings);
        assertEquals(WorkInfo.State.SUCCEEDED, info.getState());
        assertEquals(1, info.getOutputData().getInt("delivered", -1));
        assertEquals(65, fixture.events(settings.calendar));
        Bundle observation = new Bundle();
        observation.putLong(
                "singleRequestToVerifiedObservationMillis",
                SystemClock.elapsedRealtime() - requestedAt);
        observation.putLong(
                "singleSourceToVerifiedObservationMillis", System.currentTimeMillis() - sourceAt);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, observation);
        WorkManager manager = WorkManager.getInstance(context());
        OneTimeWorkRequest first = AutomaticScheduling.prompt(settings),
                second = AutomaticScheduling.prompt(settings);
        try {
            manager.enqueue(java.util.Arrays.asList(first, second))
                    .getResult()
                    .get(15, TimeUnit.SECONDS);
            long deadline = SystemClock.elapsedRealtime() + 60000;
            WorkInfo a = null, b = null;
            do {
                a = manager.getWorkInfoById(first.getId()).get(5, TimeUnit.SECONDS);
                b = manager.getWorkInfoById(second.getId()).get(5, TimeUnit.SECONDS);
                if (a != null
                        && b != null
                        && a.getState().isFinished()
                        && b.getState().isFinished()) break;
                SystemClock.sleep(50);
            } while (SystemClock.elapsedRealtime() < deadline);
            assertNotNull(a);
            assertNotNull(b);
            assertEquals(WorkInfo.State.SUCCEEDED, a.getState());
            assertEquals(WorkInfo.State.SUCCEEDED, b.getState());
            assertEquals(
                    0,
                    a.getOutputData().getInt("delivered", -1)
                            + b.getOutputData().getInt("delivered", -1));
            assertEquals(65, fixture.events(settings.calendar));
        } finally {
            manager.cancelWorkById(first.getId()).getResult().get(15, TimeUnit.SECONDS);
            manager.cancelWorkById(second.getId()).getResult().get(15, TimeUnit.SECONDS);
        }
    }

    @Test
    public void revokedCallAccessStopsWorkerWithoutAnotherEffect() throws Exception {
        assertEquals(
                PackageManager.PERMISSION_DENIED,
                context().checkSelfPermission(Manifest.permission.READ_CALL_LOG));
        ActivationControl.Settings settings =
                new ActivationControl(new AndroidActivationStore(context())).current();
        assertEquals(65, fixture.events(settings.calendar));
        WorkInfo denied = execute(settings);
        assertEquals(WorkInfo.State.SUCCEEDED, denied.getState());
        assertEquals("PERMISSION_REQUIRED", denied.getOutputData().getString("status"));
        assertEquals(65, fixture.events(settings.calendar));
    }

    @Test
    public void cleanupFixture() throws Exception {
        new ActivationControl(new AndroidActivationStore(context())).pause();
        WorkManager.getInstance(context())
                .cancelUniqueWork(tag())
                .getResult()
                .get(15, TimeUnit.SECONDS);
        shellCalls(
                () -> {
                    FixtureCallLog.delete(
                            context(), Calls.PHONE_ACCOUNT_ID + "=?", new String[] {tag()});
                    try (Cursor c =
                            FixtureCallLog.query(
                                    context(),
                                    new String[] {Calls._ID},
                                    Calls.PHONE_ACCOUNT_ID + "=?",
                                    new String[] {tag()},
                                    null)) {
                        assertNotNull(c);
                        assertEquals(0, c.getCount());
                    }
                });
        fixture.cleanupFixture();
    }
}
