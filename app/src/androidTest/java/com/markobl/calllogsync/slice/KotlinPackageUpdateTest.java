package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

/**
 * Private synthetic state across actual package replacement; no provider permissions or effects.
 */
public class KotlinPackageUpdateTest {
    private Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private String token() {
        String token = InstrumentationRegistry.getArguments().getString("fixtureToken", "");
        if (!token.matches("[a-f0-9]{32}")) throw new IllegalArgumentException("Fixture required");
        return token;
    }

    private CallRecord call(int id) {
        return new CallRecord(
                "package-update/" + token(),
                Integer.toString(id),
                1234,
                0,
                1,
                null,
                null,
                (Integer) null,
                null);
    }

    @Test
    public void freshInstallIsUnconfiguredAndDenied() {
        SharedPreferences prefs = context().getSharedPreferences("slice", Context.MODE_PRIVATE);
        assertFalse(prefs.contains("calendar"));
        assertFalse(prefs.contains("kotlinUpdateFixture"));
        assertNull(new AndroidActivationStore(context()).get());
        for (String permission :
                new String[] {
                    "READ_CALL_LOG",
                    "READ_PHONE_STATE",
                    "READ_CALENDAR",
                    "WRITE_CALENDAR",
                    "WRITE_CALL_LOG",
                    "INTERNET"
                }) {
            assertEquals(
                    android.content.pm.PackageManager.PERMISSION_DENIED,
                    context().checkSelfPermission("android.permission." + permission));
        }
        assertFalse(context().getDatabasePath("calltrack-ledger.db").exists());
        assertFalse(context().getDatabasePath("calltrack-imports.db").exists());
    }

    @Test
    public void seedJavaEraState() {
        SharedPreferences prefs = context().getSharedPreferences("slice", Context.MODE_PRIVATE);
        assertFalse(prefs.contains("kotlinUpdateFixture"));
        assertTrue(
                prefs.edit()
                        .putString("kotlinUpdateFixture", token())
                        .putString("sourceNamespace", "package-update/" + token())
                        .putLong("calendar", 7)
                        .commit());
        new AndroidActivationStore(context())
                .put(new ActivationControl.Settings(7, false, 7, "UTC", 0, true, false, true));
        try (SqliteLedger ledger = new SqliteLedger(context())) {
            for (int id = 0; id < 4; id++) {
                CallRecord call = call(id);
                assertNull(ledger.outbox().get(call.key(), 7));
                CallEvent event = CallEvent.from(call, "UTC");
                DurableDelivery.State state =
                        new DurableDelivery.State[] {
                                    DurableDelivery.State.READY, DurableDelivery.State.IN_FLIGHT,
                                    DurableDelivery.State.REVIEW, DurableDelivery.State.CONFIRMED
                                }
                                [id];
                ledger.outbox()
                        .put(
                                new DurableDelivery.Intent(
                                        state,
                                        call.key(),
                                        7,
                                        CallIdentity.signature(call),
                                        event.marker,
                                        event.fingerprint(),
                                        id < 2 ? -1 : 40 + id,
                                        id < 2 ? event : null,
                                        new DurableDelivery.Operation("automatic", 7),
                                        DurableDelivery.SourceFacts.from(call)));
            }
        }
        verifyPersistedState();
    }

    @Test
    public void verifyPersistedState() {
        SharedPreferences prefs = context().getSharedPreferences("slice", Context.MODE_PRIVATE);
        assertEquals(token(), prefs.getString("kotlinUpdateFixture", null));
        assertEquals("package-update/" + token(), prefs.getString("sourceNamespace", null));
        assertEquals(7, prefs.getLong("calendar", -1));
        ActivationControl.Settings settings = new AndroidActivationStore(context()).get();
        assertNotNull(settings);
        assertFalse(settings.enabled);
        assertEquals(7, settings.generation);
        assertTrue(settings.incoming);
        assertFalse(settings.outgoing);
        assertTrue(settings.missed);
        try (SqliteLedger ledger = new SqliteLedger(context())) {
            assertEquals(6, ledger.getReadableDatabase().getVersion());
            for (int id = 0; id < 4; id++) {
                CallRecord call = call(id);
                DurableDelivery.Intent saved = ledger.outbox().get(call.key(), 7);
                assertNotNull(saved);
                assertEquals(
                        new DurableDelivery.State[] {
                                    DurableDelivery.State.READY,
                                    DurableDelivery.State.IN_FLIGHT,
                                    DurableDelivery.State.REVIEW,
                                    DurableDelivery.State.CONFIRMED
                                }
                                [id],
                        saved.state);
                assertEquals(CallIdentity.signature(call), saved.sourceSignature);
                assertEquals(CallEvent.from(call, "UTC").fingerprint(), saved.fingerprint);
                assertEquals(id < 2 ? -1 : 40 + id, saved.eventId);
                assertEquals(7, saved.operation.generation);
                assertNotNull(saved.facts);
                assertTrue(saved.facts.presentationSupplied);
                assertNull(saved.facts.presentation);
                if (id < 2) assertNotNull(saved.payload);
                else assertNull(saved.payload);
            }
        }
    }
}
