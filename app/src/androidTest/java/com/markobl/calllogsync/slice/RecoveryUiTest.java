package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.CalendarContract;

import androidx.test.platform.app.InstrumentationRegistry;

import com.markobl.calllogsync.R;

import org.junit.Test;

/** Rendered recovery controls backed by a token-owned real local calendar. */
public class RecoveryUiTest {
    private final ParityProviderTest fixture = new ParityProviderTest();

    private Context context() {
        return fixture.context();
    }

    private SharedPreferences state() {
        return context()
                .getSharedPreferences(fixture.account() + "-ui-recovery", Context.MODE_PRIVATE);
    }

    private long calendar() {
        return state().getLong("calendar", -1);
    }

    private CallRecord call(String id) {
        return new CallRecord(
                fixture.account(),
                id,
                1700000000123L,
                7,
                1,
                "Synthetic recovery fixture",
                "0000000000",
                Integer.valueOf(1),
                null);
    }

    private DurableDelivery.Intent saved(String id) {
        try (SqliteLedger ledger = new SqliteLedger(context())) {
            return ledger.outbox().get(call(id).key(), calendar());
        }
    }

    private void seed(String id) {
        CallRecord call = call(id);
        CallEvent event = CallEvent.from(call, "UTC");
        try (SqliteLedger ledger = new SqliteLedger(context())) {
            ledger.outbox()
                    .put(
                            new DurableDelivery.Intent(
                                    DurableDelivery.State.READY,
                                    call.key(),
                                    calendar(),
                                    CallIdentity.signature(call),
                                    event.marker,
                                    event.fingerprint(),
                                    -1,
                                    event,
                                    new DurableDelivery.Operation("inactive-fixture", 1),
                                    DurableDelivery.SourceFacts.from(call)));
        }
    }

    private Activity open(Instrumentation i) {
        return i.startActivitySync(
                new Intent(context(), ReceiptRecoveryActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private interface Journey {
        void run(RenderedUi ui);
    }

    private void journey(String name, Journey action) {
        Instrumentation i = InstrumentationRegistry.getInstrumentation();
        RenderedUi ui = new RenderedUi(i);
        Activity activity = open(i);
        try {
            action.run(ui);
        } catch (Throwable failure) {
            try {
                ui.capture(name + "-failure");
            } catch (Throwable capture) {
                failure.addSuppressed(capture);
            }
            throw failure;
        } finally {
            i.runOnMainSync(activity::finish);
        }
    }

    private void tap(RenderedUi ui, int resource) {
        ui.tapText(context().getString(resource));
    }

    private void awaitState(RenderedUi ui, DurableDelivery.State expected) {
        ui.await(
                () -> saved("pending").state == expected,
                "Recovery state did not become " + expected);
    }

    @Test
    public void preparePendingWithoutCallAccess() {
        assertTrue(state().edit().putLong("calendar", fixture.createCalendar()).commit());
        seed("pending");
        assertEquals(0, fixture.events(calendar()));
    }

    @Test
    public void recoveryExplanationsRenderWithStableComposeHosts() {
        journey(
                "recovery-copy",
                ui -> {
                    assertNotNull(ui.shown("recovery_intro_panel", true));
                    assertNotNull(ui.shown(context().getString(R.string.recovery_intro), false));
                    assertNotNull(ui.shown("recovery_retention_copy", true));
                    assertNotNull(ui.shown(context().getString(R.string.retention_policy), false));
                });
    }

    private Object field(Activity activity, String name) {
        try {
            java.lang.reflect.Field field = ReceiptRecoveryActivity.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(activity);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    @Test
    public void leavingAndReturningWithdrawsQueuedApproval() throws Exception {
        Instrumentation i = InstrumentationRegistry.getInstrumentation();
        RenderedUi ui = new RenderedUi(i);
        Activity activity = open(i);
        java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        try {
            ui.await(
                    () -> ui.find(context().getString(R.string.reapproval_open), false) != null,
                    "Pending reapproval control missing");
            java.util.concurrent.ExecutorService executor =
                    (java.util.concurrent.ExecutorService) field(activity, "executor");
            // Hold only the executor: real UI confirmation is queued behind simulated delayed IO.
            executor.execute(
                    () -> {
                        entered.countDown();
                        try {
                            release.await();
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                        }
                    });
            assertTrue(entered.await(10, java.util.concurrent.TimeUnit.SECONDS));
            tap(ui, R.string.reapproval_open);
            ui.tapId("button1");
            assertTrue((Boolean) field(activity, "busy"));
            assertTrue(
                    ui.automation.performGlobalAction(
                            android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME));
            ui.await(() -> !(Boolean) field(activity, "resumed"), "Leaving did not pause activity");
            i.runOnMainSync(
                    () ->
                            context()
                                    .startActivity(
                                            new Intent(context(), ReceiptRecoveryActivity.class)
                                                    .addFlags(
                                                            Intent.FLAG_ACTIVITY_NEW_TASK
                                                                    | Intent
                                                                            .FLAG_ACTIVITY_REORDER_TO_FRONT)));
            ui.await(
                    () -> (Boolean) field(activity, "resumed"),
                    "Returning did not resume same activity");
            release.countDown();
            ui.await(() -> !(Boolean) field(activity, "busy"), "Queued action did not settle");
            assertEquals(
                    "Leaving must permanently withdraw the earlier queued approval",
                    DurableDelivery.State.READY,
                    saved("pending").state);
            assertEquals(0, fixture.events(calendar()));
            ui.capture("recovery-queued-approval-withdrawn-after-return");
        } catch (Throwable failure) {
            try {
                ui.capture("withdrawal-failure");
            } catch (Throwable capture) {
                failure.addSuppressed(capture);
            }
            throw failure;
        } finally {
            release.countDown();
            i.runOnMainSync(activity::finish);
        }
    }

    @Test
    public void renderedPermissionCancelAndExplicitReapproval() {
        journey(
                "reapproval",
                ui -> {
                    tap(ui, R.string.reapproval_open);
                    ui.permissionDialogs(
                            false,
                            () ->
                                    context()
                                                    .checkSelfPermission(
                                                            Manifest.permission.WRITE_CALENDAR)
                                            == PackageManager.PERMISSION_GRANTED);
                    assertEquals(DurableDelivery.State.READY, saved("pending").state);
                    assertEquals(0, fixture.events(calendar()));
                    ui.capture("recovery-permission-is-not-approval");
                    tap(ui, R.string.reapproval_open);
                    ui.await(
                            () -> ui.find("button1", true) != null,
                            "Reapproval preview dialog missing");
                    ui.capture("recovery-explicit-preview");
                    ui.tapId("button2");
                    assertEquals(DurableDelivery.State.READY, saved("pending").state);
                    assertEquals(0, fixture.events(calendar()));
                    tap(ui, R.string.reapproval_open);
                    ui.tapId("button1");
                    awaitState(ui, DurableDelivery.State.CONFIRMED);
                    ui.await(
                            () ->
                                    ui.find(
                                                    context()
                                                            .getString(
                                                                    R.string.reapproval_verified),
                                                    false)
                                            != null,
                            "Saved approval feedback missing");
                    assertEquals(1, fixture.events(calendar()));
                    assertNull(saved("pending").payload);
                    ui.capture("recovery-reapproved-verified");
                });
    }

    @Test
    public void renderedExistingCheckWithoutWriteAccess() {
        assertEquals(
                PackageManager.PERMISSION_DENIED,
                context().checkSelfPermission(Manifest.permission.WRITE_CALENDAR));
        journey(
                "read-only",
                ui -> {
                    tap(ui, R.string.recovery_check);
                    ui.await(
                            () ->
                                    ui.find(context().getString(R.string.recovery_verified), false)
                                            != null,
                            "Read-only verification feedback missing");
                    assertEquals(DurableDelivery.State.CONFIRMED, saved("pending").state);
                    assertEquals(1, fixture.events(calendar()));
                    ui.capture("recovery-read-only-existing-check");
                });
    }

    private android.net.Uri event() {
        return ContentUris.withAppendedId(
                CalendarContract.Events.CONTENT_URI, saved("pending").eventId);
    }

    @Test
    public void renderedEditDeletionAndSuppression() {
        String title = CallEvent.from(call("pending"), "UTC").title;
        ContentValues changed = new ContentValues();
        changed.put(CalendarContract.Events.TITLE, "Human edited synthetic event");
        assertEquals(1, context().getContentResolver().update(event(), changed, null, null));
        journey(
                "edit-delete",
                ui -> {
                    tap(ui, R.string.recovery_check);
                    awaitState(ui, DurableDelivery.State.REVIEW);
                    assertEquals(
                            "Human edited synthetic event",
                            new AndroidCalendarSink(context())
                                    .read(saved("pending").eventId)
                                    .event
                                    .title);
                    assertEquals(1, fixture.events(calendar()));
                    ui.capture("recovery-preserves-human-edit");
                    ContentValues original = new ContentValues();
                    original.put(CalendarContract.Events.TITLE, title);
                    assertEquals(
                            1,
                            context().getContentResolver().update(event(), original, null, null));
                    tap(ui, R.string.recovery_check);
                    awaitState(ui, DurableDelivery.State.CONFIRMED);
                    assertEquals(1, context().getContentResolver().delete(event(), null, null));
                    tap(ui, R.string.recovery_check);
                    awaitState(ui, DurableDelivery.State.REVIEW);
                    assertEquals(0, fixture.events(calendar()));
                    ui.capture("recovery-does-not-recreate-deleted-event");
                    tap(ui, R.string.recovery_suppress);
                    ui.tapId("button2");
                    assertEquals(DurableDelivery.State.REVIEW, saved("pending").state);
                    tap(ui, R.string.recovery_suppress);
                    ui.tapId("button1");
                    awaitState(ui, DurableDelivery.State.SUPPRESSED);
                    assertNull(saved("pending").payload);
                    assertTrue(saved("pending").eventId >= 0);
                    assertEquals(0, fixture.events(calendar()));
                    ui.capture("recovery-explicit-suppression");
                });
    }

    @Test
    public void renderedExplicitRetentionClearsOnlyExpiredPayload() {
        seed("expired");
        journey(
                "retention",
                ui -> {
                    ui.await(
                            () ->
                                    ui.find(context().getString(R.string.reapproval_open), false)
                                            != null,
                            "Fresh pending intent not visible before aging");
                    try (SqliteLedger ledger = new SqliteLedger(context())) {
                        ledger.getWritableDatabase()
                                .execSQL(
                                        "UPDATE outbox SET payload_saved_at=? WHERE source=? AND"
                                                + " calendar=?",
                                        new Object[] {
                                            System.currentTimeMillis() - 8L * 24 * 60 * 60 * 1000,
                                            call("expired").key(),
                                            calendar()
                                        });
                    }
                    tap(ui, R.string.retention_run);
                    ui.await(
                            () -> saved("expired").state == DurableDelivery.State.EXPIRED_READY,
                            "Explicit retention did not expire old payload");
                    DurableDelivery.Intent expired = saved("expired");
                    assertNull(expired.payload);
                    assertNotNull(expired.marker);
                    assertNotNull(expired.facts);
                    assertEquals(DurableDelivery.State.SUPPRESSED, saved("pending").state);
                    assertEquals(0, fixture.events(calendar()));
                    ui.capture("recovery-expired-payload-metadata-retained");
                });
    }

    @Test
    public void cleanupFixture() {
        new ActivationControl(new AndroidActivationStore(context())).pause();
        fixture.cleanupFixture();
    }
}
