package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.*;

public class HistoricalImportTest {
    static class Store implements HistoricalImport.Store {
        HistoricalImport.Session value;
        final List<HistoricalImport.Candidate> rows = new ArrayList<>();
        boolean crash;

        public void create(HistoricalImport.Session session) {
            if (value != null) throw new IllegalStateException();
            value = session;
        }

        public HistoricalImport.Session get(String id) {
            return value != null && value.id.equals(id) ? value : null;
        }

        public void save(
                HistoricalImport.Session expected,
                HistoricalImport.Session next,
                List<HistoricalImport.Candidate> added) {
            if (value.revision != expected.revision) throw new IllegalStateException();
            if (crash && next.processedId >= 0) {
                crash = false;
                throw new DurableDeliveryTest.Crash();
            }
            rows.addAll(added);
            value = next;
        }

        public void finish(
                HistoricalImport.Session expected,
                HistoricalImport.Session next,
                HistoricalImport.Candidate candidate,
                DurableDelivery.Result outcome) {
            save(expected, next, java.util.Collections.emptyList());
            for (int i = 0; i < rows.size(); i++)
                if (rows.get(i).sourceId == candidate.sourceId) {
                    rows.set(
                            i,
                            new HistoricalImport.Candidate(
                                    candidate.sourceId, candidate.key, candidate.binding, outcome));
                    return;
                }
            throw new IllegalStateException("Missing candidate");
        }

        public List<HistoricalImport.Candidate> candidates(String id, long after, int limit) {
            List<HistoricalImport.Candidate> page = new ArrayList<>();
            for (HistoricalImport.Candidate row : rows)
                if (row.sourceId > after && page.size() < limit) page.add(row);
            return page;
        }
    }

    @Test
    public void previewIsReadOnlyBoundedFrozenAndRequiresConfirmation() {
        Store store = new Store();
        PagedCallsTest.Source source = new PagedCallsTest.Source();
        for (int i = 0; i < 21; i++) source.rows.put((long) i, PagedCallsTest.row(i, 1));
        SliceTest.Sink sink = new SliceTest.Sink();
        DurableDeliveryTest.Store receipts = new DurableDeliveryTest.Store();
        HistoricalImport importer =
                new HistoricalImport(store, source, receipts, new DurableDelivery(receipts, sink));
        String id = importer.start(7, "UTC", 1005, 1015, true, true, true).id;
        assertEquals(HistoricalImport.Phase.BUILDING, importer.deliver(id, 4).phase);
        source.rows.put(21L, PagedCallsTest.row(21, 1));
        while (store.value.phase == HistoricalImport.Phase.BUILDING) importer.preview(id, 4);
        assertEquals(10, store.value.count);
        assertEquals(0, sink.inserts);
        importer.confirm(id);
        while (store.value.phase == HistoricalImport.Phase.CONFIRMED) importer.deliver(id, 3);
        assertEquals(10, sink.inserts);
        assertEquals(10, store.value.saved);
        assertEquals(HistoricalImport.Phase.COMPLETE, store.value.phase);
    }

    @Test
    public void cancellationAndSourceMutationCannotCauseUnconfirmedOrChangedInsert() {
        Store store = new Store();
        PagedCallsTest.Source source = new PagedCallsTest.Source();
        source.rows.put(1L, PagedCallsTest.row(1, 1));
        SliceTest.Sink sink = new SliceTest.Sink();
        DurableDeliveryTest.Store receipts = new DurableDeliveryTest.Store();
        HistoricalImport importer =
                new HistoricalImport(store, source, receipts, new DurableDelivery(receipts, sink));
        String id = importer.start(7, "UTC", 0, 2000, true, true, true).id;
        importer.preview(id, 4);
        importer.confirm(id);
        source.rows.put(1L, PagedCallsTest.row(1, 2));
        importer.deliver(id, 4);
        assertEquals(0, sink.inserts);
        assertEquals(1, store.value.review);
        importer.cancel(id);
        assertEquals(HistoricalImport.Phase.COMPLETE, store.value.phase);
    }

    @Test
    public void crashAfterReceiptBeforeProgressReplaysWithoutDuplicateAndCancelStopsRemaining() {
        Store store = new Store();
        PagedCallsTest.Source source = new PagedCallsTest.Source();
        source.rows.put(1L, PagedCallsTest.row(1, 1));
        source.rows.put(2L, PagedCallsTest.row(2, 1));
        SliceTest.Sink sink = new SliceTest.Sink();
        DurableDeliveryTest.Store receipts = new DurableDeliveryTest.Store();
        HistoricalImport importer =
                new HistoricalImport(store, source, receipts, new DurableDelivery(receipts, sink));
        String id = importer.start(7, "UTC", 0, 2000, true, true, true).id;
        importer.preview(id, 4);
        importer.confirm(id);
        store.crash = true;
        assertThrows(DurableDeliveryTest.Crash.class, () -> importer.deliver(id, 1));
        assertEquals(1, sink.inserts);
        source.rows.remove(1L);
        importer.deliver(id, 1);
        assertEquals(1, sink.inserts);
        assertEquals(1, store.value.saved);
        importer.cancel(id);
        importer.deliver(id, 4);
        assertEquals(1, sink.inserts);
        assertEquals(HistoricalImport.Phase.CANCELLED, store.value.phase);
    }

    @Test
    public void sourcePermissionFailureRemainsPendingAndRecordedOutcomeCanRecover() {
        Store store = new Store();
        PagedCallsTest.Source source =
                new PagedCallsTest.Source() {
                    boolean denied;

                    @Override
                    public CallRecord at(long id) {
                        if (denied) throw new SecurityException("fixture");
                        return super.at(id);
                    }
                };
        source.rows.put(1L, PagedCallsTest.row(1, 1));
        SliceTest.Sink sink = new SliceTest.Sink();
        DurableDeliveryTest.Store receipts = new DurableDeliveryTest.Store();
        HistoricalImport importer =
                new HistoricalImport(store, source, receipts, new DurableDelivery(receipts, sink));
        String id = importer.start(7, "UTC", 0, 2000, true, true, true).id;
        importer.preview(id, 4);
        importer.confirm(id);
        // A denied source port must leave the candidate available to retry.
        PagedCalls.Source denied =
                new PagedCalls.Source() {
                    public String namespace() {
                        return "fixture";
                    }

                    public CallRecord upper() {
                        return source.upper();
                    }

                    public CallRecord at(long key) {
                        throw new SecurityException("fixture");
                    }

                    public java.util.List<CallRecord> page(long a, long b, int c) {
                        return source.page(a, b, c);
                    }
                };
        HistoricalImport blocked =
                new HistoricalImport(store, denied, receipts, new DurableDelivery(receipts, sink));
        assertEquals(DurableDelivery.Result.PERMISSION_REQUIRED, blocked.deliver(id, 4).stopped);
        assertEquals(-1, store.value.processedId);
        assertEquals(0, sink.inserts);
        assertEquals(DurableDelivery.Result.PERMISSION_REQUIRED, store.rows.get(0).outcome);
        importer.deliver(id, 4);
        assertEquals(1, sink.inserts);
        assertEquals(DurableDelivery.Result.SAVED, store.rows.get(0).outcome);
    }

    @Test
    public void stoppedRunCanResumeButDoesNotInsertAfterStopAtIntentBoundary() {
        Store store = new Store();
        PagedCallsTest.Source source = new PagedCallsTest.Source();
        source.rows.put(1L, PagedCallsTest.row(1, 1));
        java.util.concurrent.atomic.AtomicBoolean running =
                new java.util.concurrent.atomic.AtomicBoolean(true);
        DurableDeliveryTest.Store receipts =
                new DurableDeliveryTest.Store() {
                    @Override
                    public void put(DurableDelivery.Intent intent) {
                        super.put(intent);
                        if (intent.state == DurableDelivery.State.READY) running.set(false);
                    }
                };
        SliceTest.Sink sink = new SliceTest.Sink();
        HistoricalImport importer =
                new HistoricalImport(store, source, receipts, new DurableDelivery(receipts, sink));
        String id = importer.start(7, "UTC", 0, 2000, true, true, true).id;
        importer.preview(id, 4);
        importer.confirm(id);
        assertEquals(DurableDelivery.Result.PAUSED, importer.deliver(id, 4, running::get).stopped);
        assertEquals(0, sink.inserts);
        assertEquals(HistoricalImport.Phase.CONFIRMED, store.value.phase);
        assertEquals(-1, store.value.processedId);
        importer.deliver(id, 4, () -> true);
        assertEquals(1, sink.inserts);
        assertEquals(HistoricalImport.Phase.COMPLETE, store.value.phase);
    }
}
