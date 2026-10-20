package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.util.*;

/** Synthetic sources/calendar port and isolated SQLite only; never reads actual device calls. */
public class HistoricalImportStoreTest {
    private Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private String fixture() {
        String token = InstrumentationRegistry.getArguments().getString("fixtureToken", "");
        if (!token.matches("[a-f0-9]{32}")) throw new IllegalArgumentException("Fixture required");
        return "import-" + token;
    }

    static CallRecord row(long id) {
        return new CallRecord(
                "fixture",
                Long.toString(id),
                1000 + id,
                1,
                1,
                null,
                null,
                Integer.valueOf(1),
                null);
    }

    static class Source implements PagedCalls.Source {
        final NavigableMap<Long, CallRecord> rows = new TreeMap<>();

        public String namespace() {
            return "fixture";
        }

        public CallRecord upper() {
            return rows.isEmpty() ? null : rows.lastEntry().getValue();
        }

        public CallRecord at(long id) {
            return rows.get(id);
        }

        public List<CallRecord> page(long after, long upper, int limit) {
            List<CallRecord> result = new ArrayList<>();
            for (Map.Entry<Long, CallRecord> entry : rows.entrySet())
                if (entry.getKey() > after && entry.getKey() <= upper && result.size() < limit)
                    result.add(entry.getValue());
            return result;
        }
    }

    static class Sink implements SliceEngine.CalendarSink {
        final Map<Long, CallEvent> rows = new HashMap<>();
        int inserts;

        public boolean writable(long calendar) {
            return calendar == 7;
        }

        public long insert(long calendar, CallEvent event) {
            long id = ++inserts;
            rows.put(id, event);
            return id;
        }

        public SliceEngine.StoredEvent read(long id) {
            return rows.containsKey(id) ? new SliceEngine.StoredEvent(id, 7, rows.get(id)) : null;
        }

        public List<SliceEngine.StoredEvent> find(long calendar, String marker) {
            List<SliceEngine.StoredEvent> found = new ArrayList<>();
            for (Map.Entry<Long, CallEvent> entry : rows.entrySet())
                if (entry.getValue().marker.equals(marker))
                    found.add(
                            new SliceEngine.StoredEvent(
                                    entry.getKey(), calendar, entry.getValue()));
            return found;
        }
    }

    @Test
    public void frozenPreviewAndPartialDeliverySurviveReopenAndRepeatWithoutDuplicate() {
        String name = fixture();
        Source source = new Source();
        for (long i = 0; i < 12; i++) source.rows.put(i, row(i));
        Sink sink = new Sink();
        String id;
        try {
            try (SqliteImportStore imports = new SqliteImportStore(context(), name + ".db");
                    SqliteLedger ledger = new SqliteLedger(context(), name + "-ledger.db")) {
                HistoricalImport importer =
                        new HistoricalImport(
                                imports,
                                source,
                                ledger.outbox(),
                                new DurableDelivery(ledger.outbox(), sink));
                id = importer.start(7, "UTC", 1003, 1009, true, true, true).id;
                while (imports.get(id).phase == HistoricalImport.Phase.BUILDING)
                    importer.preview(id, 3);
                assertEquals(6, imports.get(id).count);
                assertEquals(0, sink.inserts);
                assertEquals(2, imports.candidates(id, -1, 2).size());
                importer.confirm(id);
                importer.deliver(id, 2);
                assertEquals(2, imports.get(id).saved);
            }
            try (SqliteImportStore imports = new SqliteImportStore(context(), name + ".db");
                    SqliteLedger ledger = new SqliteLedger(context(), name + "-ledger.db")) {
                HistoricalImport importer =
                        new HistoricalImport(
                                imports,
                                source,
                                ledger.outbox(),
                                new DurableDelivery(ledger.outbox(), sink));
                assertEquals(2, imports.get(id).saved);
                while (imports.get(id).phase == HistoricalImport.Phase.CONFIRMED)
                    importer.deliver(id, 2);
                assertEquals(6, sink.inserts);
                assertEquals(6, imports.get(id).saved);
                String repeat = importer.start(7, "UTC", 1003, 1009, true, true, true).id;
                while (imports.get(repeat).phase == HistoricalImport.Phase.BUILDING)
                    importer.preview(repeat, 3);
                importer.confirm(repeat);
                while (imports.get(repeat).phase == HistoricalImport.Phase.CONFIRMED)
                    importer.deliver(repeat, 2);
                assertEquals(6, sink.inserts);
                assertEquals(6, imports.get(repeat).saved);
            }
        } finally {
            assertTrue(context().deleteDatabase(name + ".db"));
            assertTrue(context().deleteDatabase(name + "-ledger.db"));
        }
    }

    @Test
    public void failedCandidateAppendRollsBackPreviewAndStaleRevisionIsRejected() {
        String name = fixture() + ".db";
        Source source = new Source();
        for (long i = 0; i < 3; i++) source.rows.put(i, row(i));
        try (SqliteImportStore imports = new SqliteImportStore(context(), name)) {
            HistoricalImport importer = new HistoricalImport(imports, source, null, null);
            HistoricalImport.Session initial = importer.start(7, "UTC", 0, 2000, true, true, true);
            imports.getWritableDatabase()
                    .execSQL(
                            "CREATE TRIGGER fail_fixture BEFORE INSERT ON candidates WHEN"
                                    + " NEW.source_id=1 BEGIN SELECT RAISE(ABORT,'fixture'); END");
            try {
                importer.preview(initial.id, 3);
                fail("Append must fail");
            } catch (android.database.SQLException expected) {
            }
            assertEquals(0, imports.get(initial.id).revision);
            assertEquals(0, imports.candidates(initial.id, -1, 3).size());
            imports.getWritableDatabase().execSQL("DROP TRIGGER fail_fixture");
            importer.preview(initial.id, 3);
            assertEquals(3, imports.get(initial.id).count);
            try {
                imports.save(
                        initial,
                        initial.next(
                                HistoricalImport.Phase.CANCELLED,
                                initial.checkpoint,
                                0,
                                -1,
                                0,
                                0,
                                null),
                        Collections.emptyList());
                fail("Stale session must fail");
            } catch (IllegalStateException expected) {
            }
            assertEquals(HistoricalImport.Phase.PREVIEW, imports.get(initial.id).phase);
        } finally {
            assertTrue(context().deleteDatabase(name));
        }
    }

    @Test
    public void schemaOneMigrationPreservesAggregateProgressWithoutInventingOutcomes() {
        String name = fixture() + ".db";
        String ledgerName = fixture() + "-ledger.db";
        Source source = new Source();
        source.rows.put(0L, row(0));
        source.rows.put(1L, row(1));
        Sink sink = new Sink();
        String id;
        try {
            try (SqliteImportStore imports = new SqliteImportStore(context(), name);
                    SqliteLedger ledger = new SqliteLedger(context(), ledgerName)) {
                HistoricalImport importer =
                        new HistoricalImport(
                                imports,
                                source,
                                ledger.outbox(),
                                new DurableDelivery(ledger.outbox(), sink));
                id = importer.start(7, "UTC", 0, 2000, true, true, true).id;
                importer.preview(id, 4);
                importer.confirm(id);
                importer.deliver(id, 1);
                assertEquals(
                        DurableDelivery.Result.SAVED, imports.candidates(id, -1, 4).get(0).outcome);
                imports.getWritableDatabase().execSQL("ALTER TABLE candidates DROP COLUMN outcome");
                imports.getWritableDatabase().setVersion(1);
            }
            try (SqliteImportStore imports = new SqliteImportStore(context(), name);
                    SqliteLedger ledger = new SqliteLedger(context(), ledgerName)) {
                assertEquals(2, imports.getReadableDatabase().getVersion());
                assertEquals(1, imports.get(id).saved);
                assertNull(imports.candidates(id, -1, 4).get(0).outcome);
                assertNull(imports.candidates(id, -1, 4).get(1).outcome);
                source.rows.remove(1L);
                HistoricalImport importer =
                        new HistoricalImport(
                                imports,
                                source,
                                ledger.outbox(),
                                new DurableDelivery(ledger.outbox(), sink));
                importer.deliver(id, 4);
                assertEquals(1, imports.get(id).saved);
                assertEquals(1, imports.get(id).review);
                assertEquals(
                        DurableDelivery.Result.SOURCE_CHANGED,
                        imports.candidates(id, -1, 4).get(1).outcome);
                assertEquals(1, sink.inserts);
            }
        } finally {
            assertTrue(context().deleteDatabase(name));
            assertTrue(context().deleteDatabase(ledgerName));
        }
    }

    @Test
    public void failedOutcomeProgressCommitRollsBackBothRecords() {
        String name = fixture() + ".db";
        Source source = new Source();
        source.rows.put(0L, row(0));
        try (SqliteImportStore imports = new SqliteImportStore(context(), name)) {
            HistoricalImport importer = new HistoricalImport(imports, source, null, null);
            String id = importer.start(7, "UTC", 0, 2000, true, true, true).id;
            importer.preview(id, 4);
            importer.confirm(id);
            HistoricalImport.Session before = imports.get(id);
            HistoricalImport.Session next =
                    before.next(
                            HistoricalImport.Phase.COMPLETE,
                            before.checkpoint,
                            before.count,
                            0,
                            1,
                            0,
                            null);
            HistoricalImport.Candidate candidate = imports.candidates(id, -1, 1).get(0);
            imports.getWritableDatabase()
                    .execSQL(
                            "CREATE TRIGGER fail_outcome_fixture BEFORE UPDATE ON sessions BEGIN"
                                    + " SELECT RAISE(ABORT,'fixture'); END");
            try {
                imports.finish(before, next, candidate, DurableDelivery.Result.SAVED);
                fail("Progress must fail");
            } catch (android.database.SQLException expected) {
            }
            assertNull(imports.candidates(id, -1, 1).get(0).outcome);
            assertEquals(-1, imports.get(id).processedId);
            assertEquals(before.revision, imports.get(id).revision);
            imports.getWritableDatabase().execSQL("DROP TRIGGER fail_outcome_fixture");
            imports.finish(before, next, candidate, DurableDelivery.Result.SAVED);
            assertEquals(1, imports.get(id).saved);
            assertEquals(
                    DurableDelivery.Result.SAVED, imports.candidates(id, -1, 1).get(0).outcome);
        } finally {
            assertTrue(context().deleteDatabase(name));
        }
    }

    @Test
    public void sessionHistoryPagesNewestFirstWithoutLosingOlderSessions() {
        String name = fixture() + ".db";
        try (SqliteImportStore imports = new SqliteImportStore(context(), name)) {
            HistoricalImport importer = new HistoricalImport(imports, new Source(), null, null);
            java.util.Set<String> expected = new java.util.HashSet<>();
            for (int i = 0; i < 25; i++)
                expected.add(importer.start(7, "UTC", 0, 2000, true, true, true).id);
            java.util.Set<String> seen = new java.util.HashSet<>();
            long before = -1, previous = Long.MAX_VALUE;
            while (true) {
                java.util.List<SqliteImportStore.ListedSession> page = imports.sessions(before, 4);
                if (page.isEmpty()) break;
                assertTrue(page.size() <= 4);
                for (SqliteImportStore.ListedSession row : page) {
                    assertTrue(row.cursor < previous);
                    assertTrue(seen.add(row.session.id));
                    previous = row.cursor;
                }
                before = page.get(page.size() - 1).cursor;
            }
            assertEquals(expected, seen);
            try {
                imports.sessions(0, 4);
                fail("Invalid cursor must fail");
            } catch (IllegalArgumentException expectedFailure) {
            }
            try {
                imports.sessions(-1, 51);
                fail("Oversized page must fail");
            } catch (IllegalArgumentException expectedFailure) {
            }
        } finally {
            assertTrue(context().deleteDatabase(name));
        }
    }
}
