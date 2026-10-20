package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

public class ReadyReapprovalTest {
    private DurableDelivery.Intent intent() {
        CallRecord call = PagedCallsTest.row(1, 1);
        CallEvent event = CallEvent.from(call, "UTC");
        return new DurableDelivery.Intent(
                DurableDelivery.State.READY,
                call.key(),
                7,
                CallIdentity.signature(call),
                event.marker,
                event.fingerprint(),
                -1,
                event,
                new DurableDelivery.Operation("cancelled-import", 1),
                DurableDelivery.SourceFacts.from(call));
    }

    @Test
    public void explicitApprovalUsesFreshOwnerAndPreservesOriginalFrozenData() {
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        DurableDelivery.Intent prior = intent();
        store.put(prior);
        SliceTest.Sink sink = new SliceTest.Sink();
        assertEquals(
                DurableDelivery.Result.SAVED,
                new ReadyReapproval(store, sink).approve(prior, () -> true));
        DurableDelivery.Intent after = store.get(prior.key, 7);
        assertTrue(after.operation.owner.startsWith("manual-reapproval/"));
        assertEquals(prior.sourceSignature, after.sourceSignature);
        assertEquals(prior.fingerprint, after.fingerprint);
        assertEquals(prior.facts.start, after.facts.start);
        assertEquals(
                DurableDelivery.Result.PAUSED,
                new DurableDelivery(store, sink).resume(prior.key, 7, prior.operation, () -> true));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void attemptedSuppressedOrChangedIntentCannotBeReapproved() {
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        SliceTest.Sink sink = new SliceTest.Sink();
        ReadyReapproval approval = new ReadyReapproval(store, sink);
        DurableDelivery.Intent prior = intent();
        for (DurableDelivery.State state : DurableDelivery.State.values())
            if (state != DurableDelivery.State.READY) {
                store.put(prior.state(state, -1));
                assertEquals(
                        DurableDelivery.Result.REVIEW_REQUIRED,
                        approval.approve(prior, () -> true));
            }
        store.put(
                new DurableDelivery.Intent(
                        DurableDelivery.State.READY,
                        prior.key,
                        7,
                        "different",
                        prior.marker,
                        prior.fingerprint,
                        -1,
                        prior.payload,
                        prior.operation,
                        prior.facts));
        assertEquals(DurableDelivery.Result.SOURCE_CHANGED, approval.approve(prior, () -> true));
        assertEquals(0, sink.inserts);
    }

    @Test
    public void crashAfterInsertRequiresReadOnlyReconciliationNotNewApproval() {
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        DurableDelivery.Intent prior = intent();
        store.put(prior);
        SliceTest.Sink sink =
                new SliceTest.Sink() {
                    @Override
                    public long insert(long c, CallEvent e) {
                        super.insert(c, e);
                        throw new DurableDeliveryTest.Crash();
                    }
                };
        ReadyReapproval approval = new ReadyReapproval(store, sink);
        assertThrows(DurableDeliveryTest.Crash.class, () -> approval.approve(prior, () -> true));
        assertEquals(DurableDelivery.Result.REVIEW_REQUIRED, approval.approve(prior, () -> true));
        assertEquals(1, sink.inserts);
        ReceiptRecovery recovery =
                new ReceiptRecovery(
                        store,
                        new ReceiptRecovery.ExistingEvents() {
                            public SliceEngine.StoredEvent read(long id) {
                                return sink.read(id);
                            }

                            public java.util.List<SliceEngine.StoredEvent> find(long c, String m) {
                                return sink.find(c, m);
                            }
                        });
        assertEquals(ReceiptRecovery.Result.VERIFIED, recovery.reconcile(prior.key, 7));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void deniedPermissionOrWithdrawnConfirmationDoesNotReassignIntent() {
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        DurableDelivery.Intent prior = intent();
        store.put(prior);
        SliceTest.Sink sink =
                new SliceTest.Sink() {
                    @Override
                    public boolean writable(long c) {
                        throw new SecurityException();
                    }
                };
        ReadyReapproval approval = new ReadyReapproval(store, sink);
        assertEquals(DurableDelivery.Result.PAUSED, approval.approve(prior, () -> false));
        assertEquals(
                DurableDelivery.Result.PERMISSION_REQUIRED, approval.approve(prior, () -> true));
        assertSame(prior, store.get(prior.key, 7));
        assertEquals(0, sink.inserts);
    }
}
