package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.CalendarContract;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.test.core.app.ActivityScenario;
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

    private ReceiptRecoveryActivity open(Instrumentation i) {
        return (ReceiptRecoveryActivity)
                i.startActivitySync(
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

    private void awaitRecoveryFeedback(RenderedUi ui, int message) {
        String expected = context().getString(message);
        ui.await(
                () -> ui.shown(expected, false) != null,
                "The recovery screen did not render the completed check");
    }

    private void awaitPendingAction(RenderedUi ui, String stage) {
        try {
            ui.await(
                    () -> ui.shown(context().getString(R.string.reapproval_open), false) != null,
                    "Pending recovery action did not appear",
                    90000);
        } catch (AssertionError failure) {
            try {
                ui.capture("recovery-" + stage + "-failure");
            } catch (Throwable capture) {
                failure.addSuppressed(capture);
            }
            throw new AssertionError(
                    "Pending recovery action did not appear; " + ui.dialogActionSummary(), failure);
        }
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
                    assertNotNull(ui.shown("recovery_intro_text", true));
                    assertNotNull(ui.shown(context().getString(R.string.recovery_intro), false));
                    assertNotNull(ui.shown("recovery_retention_copy_text", true));
                    assertNotNull(ui.shown(context().getString(R.string.retention_policy), false));
                });
    }

    @Test
    public void renderedPendingRecoverySurvivesProcessDeathWithoutApproval() {
        RenderedUi ui = new RenderedUi(InstrumentationRegistry.getInstrumentation());
        ui.openFromLauncher("open_recovery");
        awaitPendingAction(ui, "initial");
        tap(ui, R.string.reapproval_open);
        try {
            ui.await(
                    () -> ui.find("calltrack_dialog_confirm", true) != null,
                    "Reapproval dialog did not open");
        } catch (AssertionError failure) {
            try {
                ui.capture("recovery-reapproval-dialog-failure");
            } catch (Throwable capture) {
                failure.addSuppressed(capture);
            }
            throw new AssertionError(
                    "Reapproval dialog did not open; " + ui.dialogActionSummary(), failure);
        }
        assertEquals(DurableDelivery.State.READY, saved("pending").state);
        assertEquals(0, fixture.events(calendar()));
        ui.capture("recovery-before-process-death");

        ui.killStoppedProcessAndReturn(CalendarSliceActivity.class);
        assertEquals(DurableDelivery.State.READY, saved("pending").state);
        assertEquals(0, fixture.events(calendar()));
        awaitPendingAction(ui, "after-process-death");
        assertNull(
                "Process restoration must not revive the approval dialog",
                ui.find("calltrack_dialog_confirm", true));
        assertEquals(DurableDelivery.State.READY, saved("pending").state);
        assertEquals(0, fixture.events(calendar()));
        ui.capture("recovery-after-process-death");
    }

    @Test
    public void renderedPendingRecoverySurvivesRecreationWithoutApproval() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        RenderedUi ui = new RenderedUi(instrumentation);
        try (ActivityScenario<ReceiptRecoveryActivity> scenario =
                ActivityScenario.launch(ReceiptRecoveryActivity.class)) {
            ui.await(
                    () -> ui.shown(context().getString(R.string.reapproval_open), false) != null,
                    "Pending recovery action did not render");
            tap(ui, R.string.reapproval_open);
            ui.await(
                    () -> ui.find("calltrack_dialog_confirm", true) != null,
                    "Reapproval dialog did not open before recreation");
            assertEquals(DurableDelivery.State.READY, saved("pending").state);
            assertEquals(0, fixture.events(calendar()));
            ui.capture("recovery-before-recreation");

            scenario.recreate();
            ui.await(
                    () -> ui.shown(context().getString(R.string.reapproval_open), false) != null,
                    "Pending recovery action did not return after recreation");
            assertNull(
                    "Recreation must withdraw the earlier review",
                    ui.find("calltrack_dialog_confirm", true));
            assertEquals(
                    "Recreation must not approve a pending record",
                    DurableDelivery.State.READY,
                    saved("pending").state);
            assertEquals(0, fixture.events(calendar()));
            ui.capture("recovery-after-recreation");
        }
    }

    private boolean refreshEnabled(RenderedUi ui, boolean enabled) {
        AccessibilityNodeInfo refresh = ui.shown("recovery_refresh_action", true);
        return refresh != null && refresh.isEnabled() == enabled;
    }

    @Test
    public void leavingAndReturningWithdrawsQueuedApproval() {
        Instrumentation i = InstrumentationRegistry.getInstrumentation();
        RenderedUi ui = new RenderedUi(i);
        ReceiptRecoveryActivity activity = open(i);
        try {
            awaitPendingAction(ui, "before approval");
            ui.await(() -> refreshEnabled(ui, true), "Initial recovery read did not settle");
            // Hold the same lock that gates consent and insertion in ReadyReapproval.
            synchronized (DurableDelivery.DELIVERY_LOCK) {
                tap(ui, R.string.reapproval_open);
                ui.tapId("calltrack_dialog_confirm");
                ui.await(() -> refreshEnabled(ui, false), "Rendered approval did not queue");
                assertTrue(
                        ui.automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME));
                ui.await(
                        () -> !RenderedUi.isResumed(i, activity), "Leaving did not pause activity");
                ui.startActivity(ReceiptRecoveryActivity.class);
                ui.await(
                        () -> RenderedUi.isResumed(i, activity),
                        "Returning did not resume the same activity");
                ui.await(
                        () -> refreshEnabled(ui, false),
                        "Queued action settled before lock release");
            }
            ui.await(() -> refreshEnabled(ui, true), "Queued action did not settle");
            assertNotNull(ui.shown(context().getString(R.string.reapproval_paused), false));
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
                            () -> ui.find("calltrack_dialog_confirm", true) != null,
                            "Reapproval preview dialog missing");
                    ui.capture("recovery-explicit-preview");
                    ui.tapId("calltrack_dialog_cancel");
                    assertEquals(DurableDelivery.State.READY, saved("pending").state);
                    assertEquals(0, fixture.events(calendar()));
                    tap(ui, R.string.reapproval_open);
                    ui.tapId("calltrack_dialog_confirm");
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
                                    ui.shown(context().getString(R.string.recovery_verified), false)
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
                    awaitRecoveryFeedback(ui, R.string.recovery_review_result);
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
                    DurableDelivery.Intent restored = saved("pending");
                    SliceEngine.StoredEvent stored =
                            new AndroidCalendarSink(context()).read(restored.eventId);
                    assertNotNull("The restored fixture event must exist", stored);
                    assertTrue(
                            "The restored fixture must match its durable receipt",
                            restored.matchesEvent(stored.event));
                    tap(ui, R.string.recovery_check);
                    awaitState(ui, DurableDelivery.State.CONFIRMED);
                    awaitRecoveryFeedback(ui, R.string.recovery_verified);
                    assertEquals(1, context().getContentResolver().delete(event(), null, null));
                    tap(ui, R.string.recovery_check);
                    awaitState(ui, DurableDelivery.State.REVIEW);
                    awaitRecoveryFeedback(ui, R.string.recovery_review_result);
                    assertEquals(0, fixture.events(calendar()));
                    ui.capture("recovery-does-not-recreate-deleted-event");
                    tap(ui, R.string.recovery_suppress);
                    ui.tapId("calltrack_dialog_cancel");
                    assertEquals(DurableDelivery.State.REVIEW, saved("pending").state);
                    tap(ui, R.string.recovery_suppress);
                    ui.tapId("calltrack_dialog_confirm");
                    awaitState(ui, DurableDelivery.State.SUPPRESSED);
                    assertNull(saved("pending").payload);
                    assertTrue(saved("pending").eventId >= 0);
                    assertEquals(0, fixture.events(calendar()));
                    ui.capture("recovery-explicit-suppression");
                });
    }

    @Test
    public void renderedExplicitRetentionClearsOnlyExpiredPayload() {
        long fixtureCalendar = calendar();
        seed("expired");
        journey(
                "retention",
                ui -> {
                    ui.await(
                            () ->
                                    ui.shown(context().getString(R.string.reapproval_open), false)
                                            != null,
                            "Fresh pending intent not visible before aging");
                    try (SqliteLedger ledger = new SqliteLedger(context())) {
                        RoomFixtures.execute(
                                ledger,
                                "UPDATE outbox SET payload_saved_at=? WHERE source=? AND"
                                        + " calendar=?",
                                System.currentTimeMillis() - 8L * 24 * 60 * 60 * 1000,
                                call("expired").key(),
                                fixtureCalendar);
                    }
                    tap(ui, R.string.retention_run);
                    ui.await(
                            () -> saved("expired").state == DurableDelivery.State.EXPIRED_READY,
                            "Explicit retention did not expire old payload");
                    ui.revealId("recovery_retention_feedback");
                    assertEquals(
                            context()
                                    .getResources()
                                    .getQuantityString(R.plurals.retention_result, 1, 1),
                            String.valueOf(
                                    ui.shown("recovery_retention_feedback", true).getText()));
                    DurableDelivery.Intent expired = saved("expired");
                    assertNull(expired.payload);
                    assertNotNull(expired.marker);
                    assertNotNull(expired.facts);
                    assertEquals(DurableDelivery.State.SUPPRESSED, saved("pending").state);
                    assertEquals(0, fixture.events(fixtureCalendar));
                    ui.capture("recovery-expired-payload-metadata-retained");
                });
    }

    @Test
    public void cleanupFixture() {
        new ActivationControl(new AndroidActivationStore(context())).pause();
        fixture.cleanupFixture();
    }
}
