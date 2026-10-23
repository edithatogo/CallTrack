package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;

import androidx.compose.ui.platform.ComposeView;
import androidx.test.platform.app.InstrumentationRegistry;

import com.markobl.calllogsync.R;

import org.junit.Test;

import java.time.LocalDate;
import java.util.UUID;

/** Exercises the Activity-to-Compose history action with one synthetic local session. */
public class HistoricalImportHistoryActivityTest {
    @Test
    public void explicitHistoryTapOpensActivityOwnedSession() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        String id = UUID.randomUUID().toString();
        LocalDate fromDate = LocalDate.of(2026, 10, 20);
        LocalDate toDate = fromDate.plusDays(1);
        long from = fromDate.atStartOfDay(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli();
        long to = toDate.atStartOfDay(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli();
        HistoricalImport.Session session =
                new HistoricalImport.Session(
                        id,
                        1,
                        "UTC",
                        from,
                        to,
                        true,
                        false,
                        false,
                        HistoricalImport.Phase.BUILDING,
                        new PagedCalls.Checkpoint("synthetic-history", -1, null, -1, null),
                        0,
                        0,
                        -1,
                        0,
                        0,
                        null);
        RenderedUi ui = new RenderedUi(instrumentation);
        Activity activity = null;
        try {
            try (SqliteImportStore store = new SqliteImportStore(context)) {
                store.create(session);
            }
            activity =
                    instrumentation.startActivitySync(
                            new Intent(context, HistoricalImportActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            assertTrue(
                    "Historical session host must be a ComposeView",
                    activity.findViewById(R.id.import_history) instanceof ComposeView);
            String range = fromDate + " → " + toDate + " (UTC)";
            String row =
                    context.getString(
                            R.string.import_history_item,
                            range,
                            context.getString(R.string.import_building));
            ui.tapText(row);
            String summary =
                    context.getString(
                            R.string.import_progress,
                            range,
                            context.getString(R.string.import_building),
                            context.getString(R.string.auto_destination_unavailable),
                            context.getString(R.string.auto_incoming),
                            0L,
                            0L,
                            0L);
            ui.await(() -> ui.find(summary, false) != null, "History tap did not open the session");
        } finally {
            if (activity != null) {
                Activity launched = activity;
                instrumentation.runOnMainSync(launched::finish);
            }
            try (SqliteImportStore store = new SqliteImportStore(context)) {
                store.getWritableDatabase().delete("sessions", "id=?", new String[] {id});
                assertNull(store.get(id));
            }
        }
    }
}
