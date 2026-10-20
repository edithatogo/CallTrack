package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.SystemClock;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.*;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

/** Actual WorkManager execution of no-access branches; no calls, calendar or permission changes. */
public class AutomaticWorkTest {
    private Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private String fixture() {
        String token = InstrumentationRegistry.getArguments().getString("fixtureToken", "");
        if (!token.matches("[a-f0-9]{32}")) throw new IllegalArgumentException("Fixture required");
        return "work-" + token;
    }

    private void clearStatus(SharedPreferences p) {
        assertTrue(
                p.edit()
                        .remove("automaticActivation")
                        .remove("automaticStatusGeneration")
                        .remove("automaticStatusAt")
                        .remove("automaticStatusCode")
                        .remove("automaticStatusDelivered")
                        .remove("automaticStatusReview")
                        .remove("automaticStatusFailures")
                        .remove("automaticStatusToken")
                        .commit());
    }

    private WorkInfo execute(long generation) throws Exception {
        WorkManager manager = WorkManager.getInstance(context());
        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(AutomaticLogWorker.class)
                        .setInputData(
                                new Data.Builder()
                                        .putLong(AutomaticLogWorker.GENERATION, generation)
                                        .build())
                        .addTag(fixture())
                        .build();
        manager.enqueueUniqueWork(fixture(), ExistingWorkPolicy.REPLACE, request)
                .getResult()
                .get(15, TimeUnit.SECONDS);
        long deadline = SystemClock.elapsedRealtime() + 20000;
        WorkInfo info;
        do {
            info = manager.getWorkInfoById(request.getId()).get(5, TimeUnit.SECONDS);
            if (info != null && info.getState().isFinished()) return info;
            SystemClock.sleep(50);
        } while (SystemClock.elapsedRealtime() < deadline);
        throw new AssertionError("Worker did not finish within fixture observation window");
    }

    @Test
    public void absentActivationExecutesWithoutCallCalendarPermission() throws Exception {
        SharedPreferences p = context().getSharedPreferences("slice", Context.MODE_PRIVATE);
        assertFalse(p.contains("automaticActivation"));
        try {
            assertEquals(
                    PackageManager.PERMISSION_DENIED,
                    context().checkSelfPermission(Manifest.permission.READ_CALL_LOG));
            WorkInfo info = execute(1);
            assertEquals(WorkInfo.State.SUCCEEDED, info.getState());
            assertEquals("PAUSED", info.getOutputData().getString("status"));
            assertEquals(NetworkType.NOT_REQUIRED, info.getConstraints().getRequiredNetworkType());
        } finally {
            WorkManager.getInstance(context())
                    .cancelUniqueWork(fixture())
                    .getResult()
                    .get(15, TimeUnit.SECONDS);
            clearStatus(p);
        }
    }

    @Test
    public void staleGenerationExecutesWithoutOpeningProviders() throws Exception {
        SharedPreferences p = context().getSharedPreferences("slice", Context.MODE_PRIVATE);
        assertFalse(p.contains("automaticActivation"));
        try {
            ActivationControl control =
                    new ActivationControl(new AndroidActivationStore(context()));
            control.activate(7, "UTC", 1000, true, true, true);
            control.activate(8, "UTC", 2000, true, false, true);
            WorkInfo info = execute(1);
            assertEquals(WorkInfo.State.SUCCEEDED, info.getState());
            assertEquals("PAUSED", info.getOutputData().getString("status"));
            assertEquals(2, control.current().generation);
            assertEquals(8, control.current().calendar);
            assertEquals(
                    PackageManager.PERMISSION_DENIED,
                    context().checkSelfPermission(Manifest.permission.READ_CALENDAR));
        } finally {
            WorkManager.getInstance(context())
                    .cancelUniqueWork(fixture())
                    .getResult()
                    .get(15, TimeUnit.SECONDS);
            clearStatus(p);
        }
    }

    @Test
    public void promptAndPeriodicRequestsPersistWithoutNetworkConstraint() throws Exception {
        WorkManager manager = WorkManager.getInstance(context());
        ActivationControl.Settings settings =
                new ActivationControl.Settings(1, true, 7, "UTC", 1000, true, true, true);
        OneTimeWorkRequest prompt = AutomaticScheduling.prompt(settings);
        PeriodicWorkRequest periodic = AutomaticScheduling.periodic(settings);
        try {
            manager.enqueueUniqueWork(fixture() + "-prompt", ExistingWorkPolicy.REPLACE, prompt)
                    .getResult()
                    .get(15, TimeUnit.SECONDS);
            manager.enqueueUniquePeriodicWork(
                            fixture() + "-periodic", ExistingPeriodicWorkPolicy.KEEP, periodic)
                    .getResult()
                    .get(15, TimeUnit.SECONDS);
            assertEquals(
                    NetworkType.NOT_REQUIRED,
                    manager.getWorkInfoById(prompt.getId())
                            .get(5, TimeUnit.SECONDS)
                            .getConstraints()
                            .getRequiredNetworkType());
            WorkInfo periodicInfo =
                    manager.getWorkInfoById(periodic.getId()).get(5, TimeUnit.SECONDS);
            assertEquals(
                    NetworkType.NOT_REQUIRED,
                    periodicInfo.getConstraints().getRequiredNetworkType());
            assertNotNull(periodicInfo.getPeriodicityInfo());
        } finally {
            manager.cancelUniqueWork(fixture() + "-prompt").getResult().get(15, TimeUnit.SECONDS);
            manager.cancelUniqueWork(fixture() + "-periodic").getResult().get(15, TimeUnit.SECONDS);
            clearStatus(context().getSharedPreferences("slice", Context.MODE_PRIVATE));
        }
    }
}
