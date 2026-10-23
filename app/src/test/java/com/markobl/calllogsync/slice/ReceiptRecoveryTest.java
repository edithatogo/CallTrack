package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.*;

public class ReceiptRecoveryTest {
    private CallRecord call() {
        return PagedCallsTest.row(1, 1);
    }

    private ReceiptRecovery recovery(DurableDeliveryTest.Store store, SliceTest.Sink sink) {
        return new ReceiptRecovery(
                store,
                new ReceiptRecovery.ExistingEvents() {
                    public SliceEngine.StoredEvent read(long id) {
                        return sink.read(id);
                    }

                    public List<SliceEngine.StoredEvent> find(long calendar, String marker) {
                        return sink.find(calendar, marker);
                    }
                });
    }

    @Test
    public void neverAttemptedIntentCannotBeInsertedByReconciliation() {
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        CallEvent event = CallEvent.from(call(), "UTC");
        store.put(
                new DurableDelivery.Intent(
                        DurableDelivery.State.READY,
                        call().key(),
                        7,
                        CallIdentity.signature(call()),
                        event.marker,
                        event.fingerprint(),
                        -1,
                        event));
        SliceTest.Sink sink = new SliceTest.Sink();
        assertEquals(
                ReceiptRecovery.Result.NEVER_ATTEMPTED,
                recovery(store, sink).reconcile(call().key(), 7));
        assertEquals(0, sink.inserts);
        assertNotNull(store.get(call().key(), 7).payload);
    }

    @Test
    public void interruptedEffectCanBeRecoveredAcrossInactiveOwnershipWithoutNewInsert() {
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        SliceTest.Sink sink = new SliceTest.Sink();
        CallEvent event = CallEvent.from(call(), "UTC");
        sink.insert(7, event);
        store.put(
                new DurableDelivery.Intent(
                        DurableDelivery.State.IN_FLIGHT,
                        call().key(),
                        7,
                        CallIdentity.signature(call()),
                        event.marker,
                        event.fingerprint(),
                        -1,
                        event,
                        new DurableDelivery.Operation("cancelled-import", 1)));
        assertEquals(
                ReceiptRecovery.Result.VERIFIED, recovery(store, sink).reconcile(call().key(), 7));
        assertEquals(1, sink.inserts);
        assertEquals(DurableDelivery.State.CONFIRMED, store.get(call().key(), 7).state);
        assertNull(store.get(call().key(), 7).payload);
    }

    @Test
    public void changedOrMissingEventRemainsReviewAndSuppressionKeepsTombstone() {
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        SliceTest.Sink sink = new SliceTest.Sink();
        new DurableDelivery(store, sink).deliver(call(), 7, "UTC", () -> true);
        sink.rows.clear();
        ReceiptRecovery recovery = recovery(store, sink);
        assertEquals(ReceiptRecovery.Result.REVIEW_REQUIRED, recovery.reconcile(call().key(), 7));
        assertEquals(ReceiptRecovery.Result.SUPPRESSED, recovery.suppress(call().key(), 7));
        assertNotNull(store.get(call().key(), 7));
        assertEquals(1, store.get(call().key(), 7).eventId);
        assertNull(store.get(call().key(), 7).payload);
        assertEquals(ReceiptRecovery.Result.SUPPRESSED, recovery.reconcile(call().key(), 7));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void ambiguousOrPermissionDeniedInspectionCannotInventBinding() {
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        CallEvent event = CallEvent.from(call(), "UTC");
        store.put(
                new DurableDelivery.Intent(
                        DurableDelivery.State.IN_FLIGHT,
                        call().key(),
                        7,
                        "s",
                        event.marker,
                        event.fingerprint(),
                        -1,
                        event));
        ReceiptRecovery denied =
                new ReceiptRecovery(
                        store,
                        new ReceiptRecovery.ExistingEvents() {
                            public SliceEngine.StoredEvent read(long id) {
                                throw new SecurityException();
                            }

                            public List<SliceEngine.StoredEvent> find(long c, String m) {
                                throw new SecurityException();
                            }
                        });
        assertEquals(ReceiptRecovery.Result.PERMISSION_REQUIRED, denied.reconcile(call().key(), 7));
        assertEquals(DurableDelivery.State.IN_FLIGHT, store.get(call().key(), 7).state);
        ReceiptRecovery ambiguous =
                new ReceiptRecovery(
                        store,
                        new ReceiptRecovery.ExistingEvents() {
                            public SliceEngine.StoredEvent read(long id) {
                                return null;
                            }

                            public List<SliceEngine.StoredEvent> find(long c, String m) {
                                return Arrays.asList(
                                        new SliceEngine.StoredEvent(1, 7, event),
                                        new SliceEngine.StoredEvent(2, 7, event));
                            }
                        });
        assertEquals(ReceiptRecovery.Result.REVIEW_REQUIRED, ambiguous.reconcile(call().key(), 7));
        assertEquals(-1, store.get(call().key(), 7).eventId);
    }

    @Test
    public void discoveredIdSurvivesDeniedReadbackWithoutSourceOrReinsertion() {
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        CallEvent event = CallEvent.from(call(), "UTC");
        store.put(
                new DurableDelivery.Intent(
                        DurableDelivery.State.IN_FLIGHT,
                        call().key(),
                        7,
                        "s",
                        event.marker,
                        event.fingerprint(),
                        -1,
                        event));
        ReceiptRecovery recovery =
                new ReceiptRecovery(
                        store,
                        new ReceiptRecovery.ExistingEvents() {
                            public SliceEngine.StoredEvent read(long id) {
                                throw new SecurityException();
                            }

                            public List<SliceEngine.StoredEvent> find(long c, String m) {
                                return Collections.singletonList(
                                        new SliceEngine.StoredEvent(42, 7, event));
                            }
                        });
        assertEquals(
                ReceiptRecovery.Result.PERMISSION_REQUIRED, recovery.reconcile(call().key(), 7));
        assertEquals(42, store.get(call().key(), 7).eventId);
        assertEquals(DurableDelivery.State.ATTEMPTED, store.get(call().key(), 7).state);
    }
}
