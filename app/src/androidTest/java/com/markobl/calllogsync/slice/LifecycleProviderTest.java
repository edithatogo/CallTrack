package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.CallLog.Calls;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.*;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

/** One tagged synthetic call per real lifecycle phase; never a physical phone. */
public class LifecycleProviderTest {
    private final ParityProviderTest fixture = new ParityProviderTest();

    private Context context() {
        return fixture.context();
    }

    private String tag() {
        return fixture.account() + "-lifecycle";
    }

    private SharedPreferences state() {
        return context().getSharedPreferences(tag(), Context.MODE_PRIVATE);
    }

    private void shellCalls(Runnable action) {
        FixtureCallLog.asShell(action);
    }

    private void seed() {
        long calendar = state().getLong("calendar", -1);
        int prior = state().getInt("expected", 0);
        assertEquals(prior, fixture.events(calendar));
        long at = System.currentTimeMillis();
        shellCalls(
                () -> {
                    ContentValues call = new ContentValues();
                    call.put(Calls.NUMBER, "0000000000");
                    call.put(Calls.CACHED_NAME, "Synthetic Lifecycle Fixture");
                    call.put(Calls.DATE, at);
                    call.put(Calls.DURATION, 7);
                    call.put(Calls.TYPE, Calls.INCOMING_TYPE);
                    call.put(Calls.NUMBER_PRESENTATION, Calls.PRESENTATION_ALLOWED);
                    call.put(Calls.PHONE_ACCOUNT_ID, tag());
                    android.net.Uri uri = FixtureCallLog.insert(context(), call);
                    assertNotNull(uri);
                    assertTrue(
                            state().edit()
                                    .putLong("callId", ContentUris.parseId(uri))
                                    .putLong("sourceAt", at)
                                    .putInt("expected", prior + 1)
                                    .commit());
                });
        Bundle metadata = new Bundle();
        metadata.putLong("fixtureCalendar", calendar);
        metadata.putInt("expectedEvents", prior + 1);
        metadata.putLong("fixtureSourceAt", at);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, metadata);
    }

    @Test
    public void prepareReopenFixtureWithoutScheduledWork() {
        long calendar = fixture.createCalendar();
        shellCalls(
                () -> {
                    try (Cursor c =
                            FixtureCallLog.query(
                                    context(), new String[] {Calls._ID}, null, null, null)) {
                        assertNotNull(c);
                        assertEquals("Dedicated empty call provider required", 0, c.getCount());
                    }
                });
        assertTrue(state().edit().putLong("calendar", calendar).putInt("expected", 0).commit());
        ActivationControl control = new ActivationControl(new AndroidActivationStore(context()));
        assertNull(control.current());
        control.activate(calendar, "UTC", System.currentTimeMillis(), true, true, true);
        seed();
    }

    @Test
    public void prepareNextCallWithoutWake() throws Exception {
        WorkManager manager = WorkManager.getInstance(context());
        manager.cancelAllWorkByTag(AutomaticScheduling.TAG).getResult().get(15, TimeUnit.SECONDS);
        manager.cancelUniqueWork(tag()).getResult().get(15, TimeUnit.SECONDS);
        assertTrue(new ActivationControl(new AndroidActivationStore(context())).current().enabled);
        seed();
    }

    @Test
    public void launcherResumeWithoutSystemWakeRequestsRecovery() throws Exception {
        // Isolate the launcher path from Android15+ unstopped-package BOOT_COMPLETED.
        android.content.pm.PackageManager packages = context().getPackageManager();
        android.content.ComponentName receiver =
                new android.content.ComponentName(context(), AutomaticWakeReceiver.class);
        int original = packages.getComponentEnabledSetting(receiver);
        android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        android.app.Activity activity = null;
        try {
            packages.setComponentEnabledSetting(
                    receiver,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP);
            prepareNextCallWithoutWake();
            activity =
                    instrumentation.startActivitySync(
                            new android.content.Intent(context(), CalendarSliceActivity.class)
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
            verifyExpectedDeliveryAndReceipt();
        } finally {
            if (activity != null) {
                android.app.Activity opened = activity;
                instrumentation.runOnMainSync(opened::finish);
            }
            packages.setComponentEnabledSetting(
                    receiver, original, android.content.pm.PackageManager.DONT_KILL_APP);
        }
    }

    @Test
    public void prepareDelayedCallForDoze() throws Exception {
        prepareNextCallWithoutWake();
        ActivationControl.Settings settings =
                new ActivationControl(new AndroidActivationStore(context())).current();
        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(AutomaticLogWorker.class)
                        .setInputData(
                                new Data.Builder()
                                        .putLong(AutomaticLogWorker.GENERATION, settings.generation)
                                        .build())
                        .setInitialDelay(30, TimeUnit.SECONDS)
                        .addTag(AutomaticScheduling.TAG)
                        .build();
        WorkManager.getInstance(context())
                .enqueueUniqueWork(tag(), ExistingWorkPolicy.REPLACE, request)
                .getResult()
                .get(15, TimeUnit.SECONDS);
    }

    @Test
    public void requestOfflineWakeAndVerifyDelivery() {
        assertNull(
                ((ConnectivityManager) context().getSystemService(Context.CONNECTIVITY_SERVICE))
                        .getActiveNetwork());
        AutomaticScheduling.reconcile(context());
        verifyExpectedDeliveryAndReceipt();
    }

    @Test
    public void verifyExpectedDeliveryAndReceipt() {
        long calendar = state().getLong("calendar", -1);
        int expected = state().getInt("expected", -1);
        long deadline = SystemClock.elapsedRealtime() + 30000;
        ActivationControl.Settings settings =
                new ActivationControl(new AndroidActivationStore(context())).current();
        WorkStatus status;
        do {
            status = new AndroidWorkStatusStore(context()).read(settings.generation);
            if (fixture.events(calendar) == expected
                    && status != null
                    && status.at >= state().getLong("sourceAt", 0)
                    && status.code == WorkStatus.Code.COMPLETE) break;
            SystemClock.sleep(100);
        } while (SystemClock.elapsedRealtime() < deadline);
        assertEquals(expected, fixture.events(calendar));
        assertNotNull(status);
        assertTrue(status.at >= state().getLong("sourceAt", 0));
        assertTrue(settings.enabled);
        assertEquals(calendar, settings.calendar);
        assertEquals(1, settings.generation);
        CallRecord call = new AndroidCallLogSource(context()).at(state().getLong("callId", -1));
        assertNotNull(call);
        try (SqliteLedger ledger = new SqliteLedger(context())) {
            DurableDelivery.Intent receipt = ledger.outbox().get(call.key(), calendar);
            assertNotNull(receipt);
            assertEquals(DurableDelivery.State.CONFIRMED, receipt.state);
            assertEquals(call.start, receipt.facts.start);
            assertEquals(7, receipt.facts.seconds);
            assertNull(receipt.payload);
            java.util.List<SliceEngine.StoredEvent> found =
                    new AndroidCalendarSink(context()).find(calendar, receipt.marker);
            assertEquals(1, found.size());
            assertEquals(receipt.eventId, found.get(0).id);
            assertEquals(receipt.fingerprint, found.get(0).event.fingerprint());
            assertEquals(expected, ledger.counts().verified);
        }
        assertEquals(WorkStatus.Code.COMPLETE, status.code);
        Bundle observation = new Bundle();
        observation.putInt("verifiedEvents", expected);
        observation.putLong(
                "sourceToVerifiedObservationMillis",
                System.currentTimeMillis() - state().getLong("sourceAt", 0));
        observation.putLong("workerPublishedAt", status.at);
        observation.putInt("workerReportedDelivered", status.delivered);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, observation);
    }

    @Test
    public void insertAndWaitBeforeReturningProviderId() throws Exception {
        prepareNextCallWithoutWake();
        ActivationControl control = new ActivationControl(new AndroidActivationStore(context()));
        ActivationControl.Settings settings = control.current();
        CallRecord call = new AndroidCallLogSource(context()).at(state().getLong("callId", -1));
        AndroidCalendarSink sink = new AndroidCalendarSink(context());
        try (SqliteLedger ledger = new SqliteLedger(context())) {
            SliceEngine.CalendarSink interrupted =
                    new SliceEngine.CalendarSink() {
                        public boolean writable(long calendar) {
                            return sink.writable(calendar);
                        }

                        public java.util.List<SliceEngine.StoredEvent> find(
                                long calendar, String marker) {
                            return sink.find(calendar, marker);
                        }

                        public SliceEngine.StoredEvent read(long id) {
                            return sink.read(id);
                        }

                        public long insert(long calendar, CallEvent event) {
                            long id = sink.insert(calendar, event);
                            assertEquals(
                                    DurableDelivery.State.IN_FLIGHT,
                                    ledger.outbox().get(call.key(), calendar).state);
                            // Host observes this exact calendar row, then terminates this process.
                            SystemClock.sleep(60000);
                            throw new AssertionError(
                                    "Host did not terminate the held provider-boundary process");
                        }
                    };
            new DurableDelivery(ledger.outbox(), interrupted)
                    .deliver(
                            call,
                            settings.calendar,
                            "UTC",
                            settings.operation(),
                            () -> control.allowed(settings));
            fail("The held insertion must end through actual host process termination");
        }
    }

    @Test
    public void cleanupFixture() throws Exception {
        new ActivationControl(new AndroidActivationStore(context())).pause();
        WorkManager manager = WorkManager.getInstance(context());
        manager.cancelAllWorkByTag(AutomaticScheduling.TAG).getResult().get(15, TimeUnit.SECONDS);
        manager.cancelUniqueWork(tag()).getResult().get(15, TimeUnit.SECONDS);
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
