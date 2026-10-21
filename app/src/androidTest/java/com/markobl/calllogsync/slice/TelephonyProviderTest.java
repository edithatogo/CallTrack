package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.CallLog.Calls;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

/** Actual emulator telephony only. No provider call-row insertion and no physical calls. */
public class TelephonyProviderTest {
    static final String NUMBER = "+12025550109";
    private final ParityProviderTest fixture = new ParityProviderTest();

    private Context context() {
        return fixture.context();
    }

    private SharedPreferences state() {
        return context()
                .getSharedPreferences(fixture.account() + "-telephony", Context.MODE_PRIVATE);
    }

    private void shellCalls(Runnable action) {
        FixtureCallLog.asShell(action);
    }

    private Cursor calls(String selection, String[] args) {
        Cursor c =
                FixtureCallLog.query(
                        context(),
                        new String[] {
                            Calls._ID, Calls.NUMBER, Calls.DATE, Calls.TYPE, Calls.DURATION
                        },
                        selection,
                        args,
                        null);
        assertNotNull(c);
        return c;
    }

    private String selection() {
        return Calls.NUMBER + " IN (?,?) AND " + Calls.DATE + ">=?";
    }

    private String[] args() {
        return new String[] {
            NUMBER, NUMBER.substring(1), Long.toString(state().getLong("sourceAt", Long.MAX_VALUE))
        };
    }

    @Test
    public void prepareEmptyProviderAndCalendar() {
        long calendar = fixture.createCalendar(); // READ_CALL_LOG must still be denied here.
        shellCalls(
                () -> {
                    try (Cursor c = calls(null, null)) {
                        assertEquals("Refusing nonempty call provider", 0, c.getCount());
                    }
                });
        assertTrue(
                state().edit()
                        .putLong("calendar", calendar)
                        .putLong("sourceAt", System.currentTimeMillis())
                        .putBoolean("emptyBaseline", true)
                        .commit());
    }

    @Test
    public void armPolicyAndRemoveInitialWakes() throws Exception {
        assertEquals(
                android.content.pm.PackageManager.PERMISSION_GRANTED,
                context().checkSelfPermission(Manifest.permission.READ_PHONE_STATE));
        long calendar = state().getLong("calendar", -1);
        ActivationControl.Settings settings =
                AutomaticScheduling.activate(context(), calendar, "UTC", true, true, true);
        long deadline = SystemClock.elapsedRealtime() + 60000;
        WorkStatus status;
        do {
            status = new AndroidWorkStatusStore(context()).read(settings.generation);
            if (status != null && status.code == WorkStatus.Code.COMPLETE) break;
            SystemClock.sleep(100);
        } while (SystemClock.elapsedRealtime() < deadline);
        assertNotNull(status);
        assertEquals(WorkStatus.Code.COMPLETE, status.code);
        WorkManager manager = WorkManager.getInstance(context());
        manager.cancelAllWorkByTag(AutomaticScheduling.TAG).getResult().get(15, TimeUnit.SECONDS);
        for (WorkInfo work :
                manager.getWorkInfosByTag(AutomaticScheduling.TAG).get(10, TimeUnit.SECONDS))
            assertTrue(
                    "Initial work must be terminal before telephony", work.getState().isFinished());
        assertEquals(0, fixture.events(calendar));
        try (Cursor c = calls(null, null)) {
            assertEquals(0, c.getCount());
        }
        assertTrue(state().edit().putLong("armedAt", System.currentTimeMillis()).commit());
    }

    @Test
    public void verifyRealMissedCallAndUniqueReceipt() {
        assertTrue(state().getBoolean("emptyBaseline", false));
        long calendar = state().getLong("calendar", -1);
        long id;
        try (Cursor c = calls(selection(), args())) {
            assertEquals(1, c.getCount());
            assertTrue(c.moveToFirst());
            id = c.getLong(0);
            assertEquals(Calls.MISSED_TYPE, c.getInt(3));
            assertEquals(0, c.getLong(4));
        }
        CallRecord call = new AndroidCallLogSource(context()).at(id);
        assertNotNull(call);
        try (SqliteLedger ledger = new SqliteLedger(context())) {
            DurableDelivery.Intent intent = ledger.outbox().get(call.key(), calendar);
            assertNotNull(intent);
            assertEquals(DurableDelivery.State.CONFIRMED, intent.state);
            assertEquals(call.start, intent.facts.start);
            assertEquals(0, intent.facts.seconds);
            assertNull(intent.payload);
            java.util.List<SliceEngine.StoredEvent> events =
                    new AndroidCalendarSink(context()).find(calendar, intent.marker);
            assertEquals(1, events.size());
            assertEquals(intent.eventId, events.get(0).id);
            assertEquals(intent.fingerprint, events.get(0).event.fingerprint());
            assertEquals(1, ledger.counts().verified);
        }
        assertEquals(1, fixture.events(calendar));
        Bundle observation = new Bundle();
        observation.putLong("actualTelephonyCallId", id);
        observation.putLong(
                "sourceToVerifiedObservationMillis", System.currentTimeMillis() - call.start);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, observation);
    }

    @Test
    public void repeatCatchUpDoesNotDuplicate() throws Exception {
        long requestedAt = System.currentTimeMillis();
        AutomaticScheduling.reconcileCommitted(
                context(), System.nanoTime() + TimeUnit.SECONDS.toNanos(15));
        long deadline = SystemClock.elapsedRealtime() + 60000;
        WorkStatus status;
        do {
            status = new AndroidWorkStatusStore(context()).read(1);
            if (status != null
                    && status.at >= requestedAt
                    && status.code == WorkStatus.Code.COMPLETE) break;
            SystemClock.sleep(100);
        } while (SystemClock.elapsedRealtime() < deadline);
        assertNotNull(status);
        assertTrue(status.at >= requestedAt);
        assertEquals(WorkStatus.Code.COMPLETE, status.code);
        verifyRealMissedCallAndUniqueReceipt();
    }

    @Test
    public void cleanupFixture() throws Exception {
        new ActivationControl(new AndroidActivationStore(context())).pause();
        WorkManager.getInstance(context())
                .cancelAllWorkByTag(AutomaticScheduling.TAG)
                .getResult()
                .get(15, TimeUnit.SECONDS);
        if (state().getBoolean("emptyBaseline", false)) {
            shellCalls(
                    () -> {
                        java.util.List<Long> ids = new java.util.ArrayList<>();
                        try (Cursor c = calls(selection(), args())) {
                            while (c.moveToNext()) ids.add(c.getLong(0));
                        }
                        for (long id : ids) {
                            String[] scoped =
                                    new String[] {
                                        Long.toString(id), NUMBER, NUMBER.substring(1), args()[2]
                                    };
                            FixtureCallLog.delete(
                                    context(), Calls._ID + "=? AND " + selection(), scoped);
                        }
                        try (Cursor c = calls(selection(), args())) {
                            assertEquals(0, c.getCount());
                        }
                        try (Cursor c = calls(null, null)) {
                            assertEquals(
                                    "Unexpected call rows remain; inspect before uninstall",
                                    0,
                                    c.getCount());
                        }
                    });
        }
        fixture.cleanupFixture();
    }
}
