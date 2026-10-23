package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

public class PayloadRetentionTest {
    @Test
    public void expiryUsesStorageAgeAndDoesNotGuessUnknownOrFutureAge() {
        assertFalse(PayloadRetention.expired(null, PayloadRetention.WINDOW_MILLIS));
        assertFalse(PayloadRetention.expired(1000L, 999));
        assertFalse(PayloadRetention.expired(1000L, 1000 + PayloadRetention.WINDOW_MILLIS - 1));
        assertTrue(PayloadRetention.expired(1000L, 1000 + PayloadRetention.WINDOW_MILLIS));
    }

    @Test
    public void expiredNeverAttemptedIntentCannotBeInsertedByAutomaticOrReadOnlyRecovery() {
        CallRecord call = PagedCallsTest.row(1, 1);
        CallEvent event = CallEvent.from(call, "UTC");
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        store.put(
                new DurableDelivery.Intent(
                        DurableDelivery.State.EXPIRED_READY,
                        call.key(),
                        7,
                        CallIdentity.signature(call),
                        event.marker,
                        event.fingerprint(),
                        -1,
                        null,
                        new DurableDelivery.Operation("automatic", 1),
                        DurableDelivery.SourceFacts.from(call)));
        SliceTest.Sink sink = new SliceTest.Sink();
        assertEquals(
                DurableDelivery.Result.REVIEW_REQUIRED,
                new DurableDelivery(store, sink)
                        .deliver(
                                call,
                                7,
                                "UTC",
                                new DurableDelivery.Operation("automatic", 1),
                                () -> true));
        assertEquals(DurableDelivery.State.EXPIRED_READY, store.get(call.key(), 7).state);
        ReceiptRecovery recovery =
                new ReceiptRecovery(
                        store,
                        new ReceiptRecovery.ExistingEvents() {
                            public SliceEngine.StoredEvent read(long id) {
                                throw new AssertionError("No effect attempted");
                            }

                            public java.util.List<SliceEngine.StoredEvent> find(long c, String m) {
                                throw new AssertionError("No effect attempted");
                            }
                        });
        assertEquals(ReceiptRecovery.Result.NEVER_ATTEMPTED, recovery.reconcile(call.key(), 7));
        assertEquals(0, sink.inserts);
    }

    @Test
    public void
            confirmedImportCanCollectFreshSourceAfterExpiredPayloadButCannotResumeCancelledOwner() {
        CallRecord call = PagedCallsTest.row(1, 1);
        CallEvent event = CallEvent.from(call, "UTC");
        DurableDeliveryTest.Store receipts = new DurableDeliveryTest.Store();
        DurableDelivery.Operation old = new DurableDelivery.Operation("automatic", 1);
        receipts.put(
                new DurableDelivery.Intent(
                        DurableDelivery.State.EXPIRED_READY,
                        call.key(),
                        7,
                        CallIdentity.signature(call),
                        event.marker,
                        event.fingerprint(),
                        -1,
                        null,
                        old,
                        DurableDelivery.SourceFacts.from(call)));
        HistoricalImportTest.Store sessions = new HistoricalImportTest.Store();
        PagedCallsTest.Source source = new PagedCallsTest.Source();
        source.rows.put(1L, call);
        SliceTest.Sink sink = new SliceTest.Sink();
        HistoricalImport importer =
                new HistoricalImport(
                        sessions, source, receipts, new DurableDelivery(receipts, sink));
        String id = importer.start(7, "UTC", 0, 2000, true, true, true).id;
        importer.preview(id, 4);
        assertEquals(0, sink.inserts);
        importer.confirm(id);
        importer.deliver(id, 4);
        assertEquals(1, sink.inserts);
        assertEquals(HistoricalImport.Phase.COMPLETE, sessions.value.phase);
        assertEquals(sessions.value.operation().owner, receipts.get(call.key(), 7).operation.owner);
        assertEquals(
                DurableDelivery.Result.PAUSED,
                new DurableDelivery(receipts, sink).resume(call.key(), 7, old, () -> true));
    }
}
