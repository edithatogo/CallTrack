package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

/** Own private fixture database only; no calls, calendars or device settings are changed. */
public class ParityStoreTest {
    private static void expectFailure(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("Unexpected failure type", failure);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private String database() {
        String token = InstrumentationRegistry.getArguments().getString("fixtureToken", "");
        if (!token.matches("[a-f0-9]{32}"))
            throw new IllegalArgumentException("Required fixture token");
        return "parity-ledger-" + token + ".db";
    }

    @Test
    public void preservesV1ReceiptsAndCreatesLegacyBindings() {
        String name = database();
        context().deleteDatabase(name);
        try {
            try (SQLiteDatabase db =
                    context().openOrCreateDatabase(name, Context.MODE_PRIVATE, null)) {
                db.execSQL(
                        "CREATE TABLE receipts (source TEXT NOT NULL, calendar INTEGER NOT NULL,"
                                + " event INTEGER NOT NULL, fingerprint TEXT NOT NULL, PRIMARY"
                                + " KEY(source,calendar))");
                db.execSQL("INSERT INTO receipts VALUES ('fixture-key',7,42,'old-fingerprint')");
                db.setVersion(1);
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                assertEquals(42, ledger.get("fixture-key", 7).id);
                DurableDelivery.Intent migrated = ledger.outbox().get("fixture-key", 7);
                assertEquals(DurableDelivery.State.LEGACY, migrated.state);
                assertEquals(42, migrated.eventId);
                assertEquals("old-fingerprint", migrated.fingerprint);
                assertNull(migrated.sourceSignature);
                assertEquals(6, ledger.getReadableDatabase().getVersion());
            }
        } finally {
            context().deleteDatabase(name);
        }
    }

    @Test
    public void outboxCommitsInflightAndPurgesConfirmedPayloadAcrossReopen() {
        String name = database();
        context().deleteDatabase(name);
        CallRecord call =
                new CallRecord(
                        "fixture",
                        "1",
                        1700000000123L,
                        65,
                        "Incoming",
                        "Fixture Name",
                        "0000000000",
                        true);
        CallEvent event = CallEvent.from(call, "UTC");
        try {
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                ledger.outbox()
                        .put(
                                new DurableDelivery.Intent(
                                        DurableDelivery.State.IN_FLIGHT,
                                        call.key(),
                                        7,
                                        CallIdentity.signature(call),
                                        event.marker,
                                        event.fingerprint(),
                                        -1,
                                        event));
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                DurableDelivery.Intent intent = ledger.outbox().get(call.key(), 7);
                assertEquals(DurableDelivery.State.IN_FLIGHT, intent.state);
                assertEquals(event.fingerprint(), intent.payload.fingerprint());
                ledger.outbox().put(intent.state(DurableDelivery.State.CONFIRMED, 42));
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                DurableDelivery.Intent receipt = ledger.outbox().get(call.key(), 7);
                assertEquals(42, receipt.eventId);
                assertNull(receipt.payload);
                try (Cursor cursor =
                        ledger.getReadableDatabase()
                                .rawQuery(
                                        "SELECT payload_title,payload_description FROM outbox",
                                        null)) {
                    assertTrue(cursor.moveToFirst());
                    assertTrue(cursor.isNull(0));
                    assertTrue(cursor.isNull(1));
                }
            }
        } finally {
            context().deleteDatabase(name);
        }
    }

    @Test
    public void rejectsOutboxWriteInsideAnUncommittedTransaction() {
        String name = database();
        context().deleteDatabase(name);
        try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
            CallRecord call = new CallRecord("fixture", "1", 1, 0, "Missed", null, null, false);
            CallEvent event = CallEvent.from(call, "UTC");
            ledger.transaction(
                    () -> {
                        try {
                            ledger.outbox()
                                    .put(
                                            new DurableDelivery.Intent(
                                                    DurableDelivery.State.READY,
                                                    call.key(),
                                                    7,
                                                    CallIdentity.signature(call),
                                                    event.marker,
                                                    event.fingerprint(),
                                                    -1,
                                                    event));
                            fail("Outbox boundary must not hide an uncommitted intent");
                        } catch (IllegalStateException expected) {
                            /* Required guard. */
                        }
                        return SliceEngine.Result.NO_CALL;
                    });
        } finally {
            context().deleteDatabase(name);
        }
    }

    @Test
    public void kotlinMappingRecognisesPreservedJavaReceiptWithoutReinsert() {
        String name = database();
        context().deleteDatabase(name);
        CallRecord call = new CallRecord("fixture", "1", 1234, 0, 1, "Cached", "000", true, "SIM");
        String javaFingerprint = "c19af50b5118168637d7c85bc6d34bce6739086cbc4510d416bbd2d7799539b1";
        CallEvent original = CallEvent.from(call, "UTC");
        assertEquals(javaFingerprint, original.fingerprint());
        CallEvent[] observed = {original};
        SliceEngine.CalendarSink sink =
                new SliceEngine.CalendarSink() {
                    @Override
                    public boolean writable(long calendar) {
                        return calendar == 7;
                    }

                    @Override
                    public java.util.List<SliceEngine.StoredEvent> find(
                            long calendar, String marker) {
                        throw new AssertionError("Known Java receipt must use its event ID");
                    }

                    @Override
                    public long insert(long calendar, CallEvent event) {
                        throw new AssertionError("Migration must not create a second event");
                    }

                    @Override
                    public SliceEngine.StoredEvent read(long id) {
                        assertEquals(42, id);
                        return new SliceEngine.StoredEvent(42, 7, observed[0]);
                    }
                };
        try {
            try (SQLiteDatabase db =
                    context().openOrCreateDatabase(name, Context.MODE_PRIVATE, null)) {
                db.execSQL(
                        "CREATE TABLE receipts (source TEXT NOT NULL, calendar INTEGER NOT NULL,"
                                + " event INTEGER NOT NULL, fingerprint TEXT NOT NULL, PRIMARY"
                                + " KEY(source,calendar))");
                db.execSQL(
                        "INSERT INTO receipts VALUES (?,7,42,?)",
                        new Object[] {call.key(), javaFingerprint});
                db.setVersion(1);
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                assertEquals(
                        SliceEngine.Result.ALREADY_LOGGED,
                        new SliceEngine(() -> call, sink, ledger, "UTC").run(7));
                assertEquals(javaFingerprint, ledger.get(call.key(), 7).fingerprint);
            }
            observed[0] =
                    new CallEvent(
                            "Human edit",
                            original.description,
                            original.start,
                            original.end,
                            original.timezone,
                            original.marker);
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                assertEquals(
                        SliceEngine.Result.REVIEW_REQUIRED,
                        new SliceEngine(() -> call, sink, ledger, "UTC").run(7));
                assertEquals(javaFingerprint, ledger.get(call.key(), 7).fingerprint);
                assertEquals(6, ledger.getReadableDatabase().getVersion());
            }
        } finally {
            context().deleteDatabase(name);
        }
    }

    @Test
    public void preservesV2PendingRowsWithoutInventingOperationOwnership() {
        String name = database();
        context().deleteDatabase(name);
        try {
            try (SQLiteDatabase db =
                    context().openOrCreateDatabase(name, Context.MODE_PRIVATE, null)) {
                db.execSQL(
                        "CREATE TABLE receipts (source TEXT NOT NULL, calendar INTEGER NOT NULL,"
                                + " event INTEGER NOT NULL, fingerprint TEXT NOT NULL, PRIMARY"
                                + " KEY(source,calendar))");
                db.execSQL(
                        "CREATE TABLE outbox (source TEXT NOT NULL, calendar INTEGER NOT NULL,"
                            + " state TEXT NOT NULL, event INTEGER NOT NULL, signature TEXT, marker"
                            + " TEXT, fingerprint TEXT NOT NULL, payload_title TEXT,"
                            + " payload_description TEXT, payload_start INTEGER, payload_end"
                            + " INTEGER, payload_timezone TEXT, PRIMARY KEY(source,calendar))");
                db.execSQL("CREATE INDEX outbox_state ON outbox(calendar,state)");
                db.execSQL(
                        "INSERT INTO outbox VALUES"
                                + " ('source',7,'READY',-1,'sig','marker','fp','Synthetic"
                                + " title','Synthetic description',1,2,'UTC')");
                db.setVersion(2);
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                DurableDelivery.Intent intent = ledger.outbox().get("source", 7);
                assertEquals(6, ledger.getReadableDatabase().getVersion());
                assertNull(intent.operation);
                assertEquals(DurableDelivery.State.READY, intent.state);
                assertEquals("Synthetic title", intent.payload.title);
                assertEquals("sig", intent.sourceSignature);
                assertEquals("fp", intent.fingerprint);
            }
        } finally {
            context().deleteDatabase(name);
        }
    }

    @Test
    public void operationIdentitySurvivesReopenAndPayloadPurge() {
        String name = database();
        context().deleteDatabase(name);
        try {
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                ledger.outbox()
                        .put(
                                new DurableDelivery.Intent(
                                        DurableDelivery.State.READY,
                                        "source",
                                        7,
                                        "sig",
                                        "marker",
                                        "fp",
                                        -1,
                                        new CallEvent("Synthetic", "", 1, 2, "UTC", "marker"),
                                        new DurableDelivery.Operation("import-1", 42)));
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                DurableDelivery.Intent intent = ledger.outbox().get("source", 7);
                assertEquals("import-1", intent.operation.owner);
                assertEquals(42, intent.operation.generation);
                ledger.outbox().put(intent.state(DurableDelivery.State.CONFIRMED, 123));
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                DurableDelivery.Intent intent = ledger.outbox().get("source", 7);
                assertEquals("import-1", intent.operation.owner);
                assertEquals(42, intent.operation.generation);
                assertNull(intent.payload);
                assertEquals(123, intent.eventId);
            }
        } finally {
            context().deleteDatabase(name);
        }
    }

    @Test
    public void preservesV3OwnershipWithoutInferringSourceFactsFromPayload() {
        String name = database();
        context().deleteDatabase(name);
        try {
            try (SQLiteDatabase db =
                    context().openOrCreateDatabase(name, Context.MODE_PRIVATE, null)) {
                db.execSQL(
                        "CREATE TABLE receipts (source TEXT NOT NULL, calendar INTEGER NOT NULL,"
                                + " event INTEGER NOT NULL, fingerprint TEXT NOT NULL, PRIMARY"
                                + " KEY(source,calendar))");
                db.execSQL(
                        "CREATE TABLE outbox (source TEXT NOT NULL, calendar INTEGER NOT NULL,"
                            + " state TEXT NOT NULL, event INTEGER NOT NULL, signature TEXT, marker"
                            + " TEXT, fingerprint TEXT NOT NULL, payload_title TEXT,"
                            + " payload_description TEXT, payload_start INTEGER, payload_end"
                            + " INTEGER, payload_timezone TEXT, operation_owner TEXT,"
                            + " operation_generation INTEGER NOT NULL DEFAULT 0, PRIMARY"
                            + " KEY(source,calendar))");
                db.execSQL("CREATE INDEX outbox_state ON outbox(calendar,state)");
                db.execSQL(
                        "INSERT INTO outbox"
                            + " (source,calendar,state,event,signature,marker,fingerprint,payload_title,payload_start,payload_end,payload_timezone,operation_owner,operation_generation)"
                            + " VALUES"
                            + " ('source',7,'READY',-1,'sig','marker','fp','Synthetic',100,60100,'UTC','import-1',42)");
                db.setVersion(3);
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                DurableDelivery.Intent intent = ledger.outbox().get("source", 7);
                assertEquals(6, ledger.getReadableDatabase().getVersion());
                assertNull(intent.facts);
                assertEquals("import-1", intent.operation.owner);
                assertEquals(42, intent.operation.generation);
                assertEquals(100, intent.payload.start);
                assertEquals(60100, intent.payload.end);
            }
        } finally {
            context().deleteDatabase(name);
        }
    }

    @Test
    public void sourceFactsSurviveReopenAndPayloadPurgeWithNullPresentation() {
        String name = database();
        context().deleteDatabase(name);
        try {
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                ledger.outbox()
                        .put(
                                new DurableDelivery.Intent(
                                        DurableDelivery.State.READY,
                                        "source",
                                        7,
                                        "sig",
                                        "marker",
                                        "fp",
                                        -1,
                                        new CallEvent("Synthetic", "", 123, 60123, "UTC", "marker"),
                                        new DurableDelivery.Operation("import-1", 42),
                                        new DurableDelivery.SourceFacts(123, 0, 3, null, true)));
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                DurableDelivery.Intent intent = ledger.outbox().get("source", 7);
                assertEquals(123, intent.facts.start);
                assertEquals(0, intent.facts.seconds);
                assertEquals(3, intent.facts.type);
                assertNull(intent.facts.presentation);
                assertTrue(intent.facts.presentationSupplied);
                ledger.outbox().put(intent.state(DurableDelivery.State.REVIEW, -1));
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                DurableDelivery.Intent intent = ledger.outbox().get("source", 7);
                assertNull(intent.payload);
                assertEquals(123, intent.facts.start);
                assertEquals(0, intent.facts.seconds);
                assertNull(intent.facts.presentation);
                assertTrue(intent.facts.presentationSupplied);
            }
        } finally {
            context().deleteDatabase(name);
        }
    }

    @Test
    public void frozenCheckpointPersistsAndRejectsRegressionOrRebinding() {
        String name = database();
        context().deleteDatabase(name);
        DurableDelivery.Operation op = new DurableDelivery.Operation("import-1", 42);
        PagedCalls.Checkpoint frozen =
                new PagedCalls.Checkpoint("fixture", 7, "upper-binding", -1, null);
        CallRecord row =
                new CallRecord("fixture", "1", 123, 0, 1, null, null, Integer.valueOf(3), null);
        try {
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                ledger.checkpoint(op, frozen);
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                PagedCalls.Checkpoint loaded = ledger.checkpoint(op);
                assertEquals(-1, loaded.lastId);
                assertEquals(7, loaded.upperId);
                ledger.checkpoint(op, loaded.advance(row));
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                PagedCalls.Checkpoint loaded = ledger.checkpoint(op);
                assertEquals(1, loaded.lastId);
                assertEquals(PagedCalls.binding(row), loaded.lastBinding);
                assertNull(ledger.checkpoint(new DurableDelivery.Operation("import-1", 43)));
                expectFailure(IllegalStateException.class, () -> ledger.checkpoint(op, frozen));
                expectFailure(
                        IllegalStateException.class,
                        () ->
                                ledger.checkpoint(
                                        op,
                                        new PagedCalls.Checkpoint(
                                                "fixture", 8, "changed", 1, loaded.lastBinding)));
                expectFailure(
                        IllegalStateException.class,
                        () ->
                                ledger.checkpoint(
                                        op,
                                        new PagedCalls.Checkpoint(
                                                "fixture", 7, "upper-binding", 1, "changed")));
                assertEquals(1, ledger.checkpoint(op).lastId);
                assertEquals(7, ledger.checkpoint(op).upperId);
            }
        } finally {
            context().deleteDatabase(name);
        }
    }

    @Test
    public void preservesV4FactsAndCreatesEmptyCheckpointStore() {
        String name = database();
        context().deleteDatabase(name);
        try {
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                ledger.outbox()
                        .put(
                                new DurableDelivery.Intent(
                                        DurableDelivery.State.CONFIRMED,
                                        "source",
                                        7,
                                        "sig",
                                        "marker",
                                        "fp",
                                        123,
                                        null,
                                        new DurableDelivery.Operation("automatic", 42),
                                        new DurableDelivery.SourceFacts(123, 0, 3, null, true)));
                SQLiteDatabase db = ledger.getWritableDatabase();
                db.execSQL("DROP TABLE scan_cursors");
                db.execSQL("ALTER TABLE outbox DROP COLUMN payload_saved_at");
                db.setVersion(4);
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                assertEquals(6, ledger.getReadableDatabase().getVersion());
                DurableDelivery.Intent saved = ledger.outbox().get("source", 7);
                assertEquals(123, saved.eventId);
                assertEquals(123, saved.facts.start);
                assertEquals(0, saved.facts.seconds);
                assertNull(saved.facts.presentation);
                assertEquals("automatic", saved.operation.owner);
                assertEquals(42, saved.operation.generation);
                assertNull(ledger.checkpoint(new DurableDelivery.Operation("automatic", 42)));
            }
        } finally {
            context().deleteDatabase(name);
        }
    }

    @Test
    public void rolloverPreservesProcessedAnchorAndRejectsStalePublication() {
        String name = database();
        context().deleteDatabase(name);
        DurableDelivery.Operation operation = new DurableDelivery.Operation("automatic", 5);
        PagedCalls.Checkpoint done = new PagedCalls.Checkpoint("fixture", 1, "one", 1, "one");
        PagedCalls.Checkpoint next = new PagedCalls.Checkpoint("fixture", 2, "two", 1, "one");
        try {
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                ledger.checkpoint(operation, done);
                ledger.rollover(operation, done, next);
                expectFailure(
                        IllegalStateException.class, () -> ledger.rollover(operation, done, next));
                expectFailure(
                        IllegalStateException.class, () -> ledger.checkpoint(operation, done));
                assertTrue(PagedCalls.equal(next, ledger.checkpoint(operation)));
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                assertTrue(PagedCalls.equal(next, ledger.checkpoint(operation)));
                expectFailure(
                        IllegalArgumentException.class,
                        () ->
                                ledger.rollover(
                                        operation,
                                        next,
                                        new PagedCalls.Checkpoint(
                                                "fixture", 3, "three", 1, "one")));
                PagedCalls.Checkpoint complete =
                        new PagedCalls.Checkpoint("fixture", 2, "two", 2, "two");
                ledger.checkpoint(operation, complete);
                ledger.rollover(
                        operation,
                        complete,
                        new PagedCalls.Checkpoint("fixture", 3, "three", 2, "two"));
                assertEquals(3, ledger.checkpoint(operation).upperId);
                assertEquals(2, ledger.checkpoint(operation).lastId);
            }
        } finally {
            assertTrue(context().deleteDatabase(name));
        }
    }

    @Test
    public void pendingRecoveryQueryIsBoundedAndOwnerDestinationScoped() {
        String name = database();
        context().deleteDatabase(name);
        DurableDelivery.Operation active = new DurableDelivery.Operation("automatic", 1);
        try {
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                DurableDelivery.Store outbox = ledger.outbox();
                int index = 0;
                for (DurableDelivery.State state : DurableDelivery.State.values()) {
                    outbox.put(
                            new DurableDelivery.Intent(
                                    state,
                                    "key-" + (index++),
                                    7,
                                    "signature",
                                    "marker",
                                    "fingerprint",
                                    -1,
                                    null,
                                    active));
                }
                outbox.put(
                        new DurableDelivery.Intent(
                                DurableDelivery.State.READY,
                                "other-generation",
                                7,
                                "s",
                                "m",
                                "f",
                                -1,
                                null,
                                new DurableDelivery.Operation("automatic", 2)));
                outbox.put(
                        new DurableDelivery.Intent(
                                DurableDelivery.State.READY,
                                "other-owner",
                                7,
                                "s",
                                "m",
                                "f",
                                -1,
                                null,
                                new DurableDelivery.Operation("import", 1)));
                outbox.put(
                        new DurableDelivery.Intent(
                                DurableDelivery.State.READY,
                                "other-calendar",
                                8,
                                "s",
                                "m",
                                "f",
                                -1,
                                null,
                                active));
                outbox.put(
                        new DurableDelivery.Intent(
                                DurableDelivery.State.READY,
                                "unowned",
                                7,
                                "s",
                                "m",
                                "f",
                                -1,
                                null));
                assertEquals(
                        java.util.Arrays.asList("key-0", "key-1"), ledger.pending(active, 7, 2));
                assertEquals(
                        java.util.Arrays.asList("key-0", "key-1", "key-2"),
                        ledger.pending(active, 7, 500));
                expectFailure(IllegalArgumentException.class, () -> ledger.pending(active, 7, 0));
                expectFailure(IllegalArgumentException.class, () -> ledger.pending(active, 7, 501));
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                assertEquals(3, ledger.pending(active, 7, 4).size());
                ledger.outbox()
                        .put(
                                new DurableDelivery.Intent(
                                        DurableDelivery.State.REVIEW,
                                        "key-0",
                                        7,
                                        "s",
                                        "m",
                                        "f",
                                        -1,
                                        null,
                                        active));
                assertEquals(
                        java.util.Arrays.asList("key-1", "key-2"), ledger.pending(active, 7, 4));
            }
        } finally {
            assertTrue(context().deleteDatabase(name));
        }
    }

    @Test
    public void deliveryCountsIncludeLateLegacyWithoutDoubleCountingBoundReceipt() {
        String name = database();
        context().deleteDatabase(name);
        try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
            int index = 0;
            for (DurableDelivery.State state : DurableDelivery.State.values())
                ledger.outbox()
                        .put(
                                new DurableDelivery.Intent(
                                        state, "count-" + (index++), 7, "s", "m", "f", -1, null));
            DeliveryCounts counts = ledger.counts();
            assertEquals(3, counts.pending);
            assertEquals(1, counts.verified);
            assertEquals(3, counts.review);
            assertEquals(1, counts.suppressed);
            ledger.put("late-manual", 7, new SliceEngine.Entry(123, "fingerprint"));
            assertEquals(4, ledger.counts().review);
            assertEquals(DurableDelivery.State.LEGACY, ledger.outbox().get("late-manual", 7).state);
            ledger.outbox()
                    .put(
                            new DurableDelivery.Intent(
                                    DurableDelivery.State.CONFIRMED,
                                    "late-manual",
                                    7,
                                    "s",
                                    "m",
                                    "fingerprint",
                                    123,
                                    null));
            assertEquals(3, ledger.counts().review);
            assertEquals(2, ledger.counts().verified);
        } finally {
            assertTrue(context().deleteDatabase(name));
        }
    }

    @Test
    public void recoveryPagingIncludesLegacyAndRemainsStableWhenStatesChange() {
        String name = database();
        context().deleteDatabase(name);
        try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
            for (int i = 0; i < 22; i++)
                ledger.outbox()
                        .put(
                                new DurableDelivery.Intent(
                                        DurableDelivery.State.READY,
                                        "key-" + (i / 2),
                                        7 + i % 2,
                                        "s",
                                        "m",
                                        "f",
                                        -1,
                                        null,
                                        null,
                                        new DurableDelivery.SourceFacts(
                                                1000 + (i / 2) % 3,
                                                1,
                                                1,
                                                Integer.valueOf(1),
                                                true)));
            for (int i = 0; i < 3; i++)
                ledger.put("legacy-" + i, 7, new SliceEngine.Entry(100 + i, "f"));
            java.util.Set<String> seen = new java.util.HashSet<>();
            SqliteLedger.RecoveryCursor after = null;
            ReceiptRecovery recovery =
                    new ReceiptRecovery(
                            ledger.outbox(),
                            new ReceiptRecovery.ExistingEvents() {
                                public SliceEngine.StoredEvent read(long id) {
                                    throw new AssertionError("Suppression cannot read calendar");
                                }

                                public java.util.List<SliceEngine.StoredEvent> find(
                                        long c, String m) {
                                    throw new AssertionError("Suppression cannot read calendar");
                                }
                            });
            while (true) {
                java.util.List<SqliteLedger.RecoveryItem> page = ledger.recoveryPage(after, 4);
                if (page.isEmpty()) break;
                assertTrue(page.size() <= 4);
                for (SqliteLedger.RecoveryItem item : page) {
                    assertTrue(seen.add(item.intent.key + "/" + item.intent.calendar));
                    assertEquals(
                            ReceiptRecovery.Result.SUPPRESSED,
                            recovery.suppress(item.intent.key, item.intent.calendar));
                }
                after = page.get(page.size() - 1).cursor;
            }
            assertEquals(25, seen.size());
            assertEquals(25, ledger.counts().suppressed);
            assertEquals(0, ledger.counts().review);
            expectFailure(IllegalArgumentException.class, () -> ledger.recoveryPage(null, 51));
        } finally {
            assertTrue(context().deleteDatabase(name));
        }
    }

    @Test
    public void explicitReapprovalPersistsFreshOwnerAndSuppressionClearsReadyPayload() {
        String name = database();
        context().deleteDatabase(name);
        DurableDelivery.Operation old = new DurableDelivery.Operation("cancelled-import", 1);
        HistoricalImportStoreTest.Sink sink = new HistoricalImportStoreTest.Sink();
        CallRecord call = HistoricalImportStoreTest.row(1);
        CallEvent event = CallEvent.from(call, "UTC");
        CallRecord other = HistoricalImportStoreTest.row(2);
        CallEvent pending = CallEvent.from(other, "UTC");
        try {
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                ledger.outbox()
                        .put(
                                new DurableDelivery.Intent(
                                        DurableDelivery.State.READY,
                                        call.key(),
                                        7,
                                        CallIdentity.signature(call),
                                        event.marker,
                                        event.fingerprint(),
                                        -1,
                                        event,
                                        old,
                                        DurableDelivery.SourceFacts.from(call)));
                DurableDelivery.Intent expected = ledger.outbox().get(call.key(), 7);
                assertEquals(
                        DurableDelivery.Result.SAVED,
                        new ReadyReapproval(ledger.outbox(), sink).approve(expected, () -> true));
                ledger.outbox()
                        .put(
                                new DurableDelivery.Intent(
                                        DurableDelivery.State.READY,
                                        other.key(),
                                        7,
                                        CallIdentity.signature(other),
                                        pending.marker,
                                        pending.fingerprint(),
                                        -1,
                                        pending,
                                        old,
                                        DurableDelivery.SourceFacts.from(other)));
                ReceiptRecovery recovery =
                        new ReceiptRecovery(
                                ledger.outbox(),
                                new ReceiptRecovery.ExistingEvents() {
                                    public SliceEngine.StoredEvent read(long id) {
                                        throw new AssertionError(
                                                "Suppression cannot read provider");
                                    }

                                    public java.util.List<SliceEngine.StoredEvent> find(
                                            long c, String m) {
                                        throw new AssertionError(
                                                "Suppression cannot read provider");
                                    }
                                });
                assertEquals(ReceiptRecovery.Result.SUPPRESSED, recovery.suppress(other.key(), 7));
            }
            try (SqliteLedger ledger = new SqliteLedger(context(), name)) {
                DurableDelivery.Intent saved = ledger.outbox().get(call.key(), 7);
                assertEquals(DurableDelivery.State.CONFIRMED, saved.state);
                assertTrue(saved.operation.owner.startsWith("manual-reapproval/"));
                assertEquals(event.fingerprint(), saved.fingerprint);
                assertEquals(call.start, saved.facts.start);
                assertNull(saved.payload);
                assertEquals(
                        DurableDelivery.Result.PAUSED,
                        new DurableDelivery(ledger.outbox(), sink)
                                .resume(call.key(), 7, old, () -> true));
                assertEquals(1, sink.inserts);
                DurableDelivery.Intent suppressed = ledger.outbox().get(other.key(), 7);
                assertEquals(DurableDelivery.State.SUPPRESSED, suppressed.state);
                assertNull(suppressed.payload);
                assertEquals(-1, suppressed.eventId);
                assertEquals(old.owner, suppressed.operation.owner);
                assertEquals(other.start, suppressed.facts.start);
            }
        } finally {
            assertTrue(context().deleteDatabase(name));
        }
    }

    @Test
    public void payloadStorageAgeSurvivesTransitionsAndExpiryPreservesRecoveryMetadata() {
        String name = database();
        context().deleteDatabase(name);
        long base = 1000000000000L;
        java.util.concurrent.atomic.AtomicLong time =
                new java.util.concurrent.atomic.AtomicLong(base);
        CallRecord call = HistoricalImportStoreTest.row(1),
                attempted = HistoricalImportStoreTest.row(2);
        CallEvent event = CallEvent.from(call, "UTC"), other = CallEvent.from(attempted, "UTC");
        DurableDelivery.Operation operation = new DurableDelivery.Operation("automatic", 1);
        try {
            try (SqliteLedger ledger = new SqliteLedger(context(), name, time::get)) {
                ledger.outbox()
                        .put(
                                new DurableDelivery.Intent(
                                        DurableDelivery.State.READY,
                                        call.key(),
                                        7,
                                        CallIdentity.signature(call),
                                        event.marker,
                                        event.fingerprint(),
                                        -1,
                                        event,
                                        operation,
                                        DurableDelivery.SourceFacts.from(call)));
                ledger.outbox()
                        .put(
                                new DurableDelivery.Intent(
                                        DurableDelivery.State.ATTEMPTED,
                                        attempted.key(),
                                        7,
                                        CallIdentity.signature(attempted),
                                        other.marker,
                                        other.fingerprint(),
                                        42,
                                        other,
                                        operation,
                                        DurableDelivery.SourceFacts.from(attempted)));
                time.set(base + PayloadRetention.WINDOW_MILLIS / 2);
                DurableDelivery.Intent current = ledger.outbox().get(call.key(), 7);
                assertNotNull(current.payload);
                ledger.outbox().put(current);
                try (Cursor cursor =
                        ledger.getReadableDatabase()
                                .rawQuery(
                                        "SELECT payload_saved_at FROM outbox WHERE source=?",
                                        new String[] {call.key()})) {
                    assertTrue(cursor.moveToFirst());
                    assertEquals(base, cursor.getLong(0));
                }
            }
            time.set(base + PayloadRetention.WINDOW_MILLIS);
            try (SqliteLedger ledger = new SqliteLedger(context(), name, time::get)) {
                assertEquals(1, ledger.expirePayloads(1));
                DurableDelivery.Intent expired = ledger.outbox().get(call.key(), 7);
                assertEquals(DurableDelivery.State.EXPIRED_READY, expired.state);
                assertNull(expired.payload);
                assertEquals(event.fingerprint(), expired.fingerprint);
                assertEquals(call.start, expired.facts.start);
                assertEquals(operation.owner, expired.operation.owner);
                assertEquals(-1, expired.eventId);
                DurableDelivery.Intent known = ledger.outbox().get(attempted.key(), 7);
                assertEquals(DurableDelivery.State.ATTEMPTED, known.state);
                assertNull(known.payload);
                assertEquals(42, known.eventId);
                assertEquals(other.marker, known.marker);
                try (Cursor cursor =
                        ledger.getReadableDatabase()
                                .rawQuery(
                                        "SELECT COUNT(*) FROM outbox WHERE payload_title IS NOT"
                                                + " NULL OR payload_description IS NOT NULL OR"
                                                + " payload_start IS NOT NULL OR payload_end IS NOT"
                                                + " NULL OR payload_timezone IS NOT NULL",
                                        null)) {
                    assertTrue(cursor.moveToFirst());
                    assertEquals(0, cursor.getLong(0));
                }
                assertEquals(0, ledger.expirePayloads(50));
            }
        } finally {
            assertTrue(context().deleteDatabase(name));
        }
    }

    @Test
    public void preservesV5PendingPayloadAndCheckpointWithExplicitMigrationGrace() {
        String name = database();
        context().deleteDatabase(name);
        long base = 1000000000000L;
        java.util.concurrent.atomic.AtomicLong time =
                new java.util.concurrent.atomic.AtomicLong(base);
        CallRecord call = HistoricalImportStoreTest.row(1);
        CallEvent event = CallEvent.from(call, "UTC");
        DurableDelivery.Operation operation = new DurableDelivery.Operation("import-old", 3);
        try {
            try (SqliteLedger ledger = new SqliteLedger(context(), name, time::get)) {
                ledger.outbox()
                        .put(
                                new DurableDelivery.Intent(
                                        DurableDelivery.State.READY,
                                        call.key(),
                                        7,
                                        CallIdentity.signature(call),
                                        event.marker,
                                        event.fingerprint(),
                                        -1,
                                        event,
                                        operation,
                                        DurableDelivery.SourceFacts.from(call)));
                ledger.put("legacy", 7, new SliceEngine.Entry(99, "old-fingerprint"));
                ledger.checkpoint(
                        operation, new PagedCalls.Checkpoint("fixture", 2, "upper", 1, "last"));
                ledger.getWritableDatabase()
                        .execSQL("ALTER TABLE outbox DROP COLUMN payload_saved_at");
                ledger.getWritableDatabase().setVersion(5);
            }
            long migrated = base + 100 * PayloadRetention.WINDOW_MILLIS;
            time.set(migrated);
            try (SqliteLedger ledger = new SqliteLedger(context(), name, time::get)) {
                assertEquals(6, ledger.getReadableDatabase().getVersion());
                assertNotNull(ledger.outbox().get(call.key(), 7).payload);
                assertEquals(1, ledger.checkpoint(operation).lastId);
                assertEquals(99, ledger.get("legacy", 7).id);
                time.set(migrated + PayloadRetention.WINDOW_MILLIS - 1);
                assertNotNull(ledger.outbox().get(call.key(), 7).payload);
                time.incrementAndGet();
                assertEquals(
                        DurableDelivery.State.EXPIRED_READY,
                        ledger.outbox().get(call.key(), 7).state);
                assertEquals(operation.owner, ledger.outbox().get(call.key(), 7).operation.owner);
                assertEquals(call.start, ledger.outbox().get(call.key(), 7).facts.start);
            }
        } finally {
            assertTrue(context().deleteDatabase(name));
        }
    }
}
