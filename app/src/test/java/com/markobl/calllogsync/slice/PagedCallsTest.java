package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.*;

public class PagedCallsTest {
    static CallRecord row(long id, long seconds) {
        return new CallRecord(
                "fixture",
                Long.toString(id),
                1000 + id % 1000,
                seconds,
                1,
                null,
                "000",
                Integer.valueOf(1),
                null);
    }

    static class Source implements PagedCalls.Source {
        String namespace = "fixture";
        final NavigableMap<Long, CallRecord> rows = new TreeMap<>();

        public String namespace() {
            return namespace;
        }

        public CallRecord upper() {
            return rows.isEmpty() ? null : rows.lastEntry().getValue();
        }

        public CallRecord at(long id) {
            return rows.get(id);
        }

        public List<CallRecord> page(long after, long upper, int limit) {
            List<CallRecord> page = new ArrayList<>();
            for (Map.Entry<Long, CallRecord> e : rows.entrySet())
                if (e.getKey() > after && e.getKey() <= upper && page.size() < limit)
                    page.add(e.getValue());
            return page;
        }
    }

    @Test
    public void walksManyPagesWithFrozenUpperBoundAndExplicitPerRowProgress() {
        Source source = new Source();
        for (int i = 0; i < 51; i++) source.rows.put((long) i, row(i, 1));
        PagedCalls.Checkpoint progress = PagedCalls.capture(source);
        source.rows.put(51L, row(51, 1));
        List<Long> seen = new ArrayList<>();
        while (true) {
            List<CallRecord> page = PagedCalls.read(source, progress, 7);
            if (page.isEmpty()) break;
            assertTrue(page.size() <= 7);
            for (CallRecord call : page) {
                seen.add(Long.parseLong(call.id));
                progress = progress.advance(call);
            }
        }
        assertEquals(51, seen.size());
        for (int i = 0; i < 51; i++) assertEquals(Long.valueOf(i), seen.get(i));
        assertEquals(50, progress.upperId);
        assertEquals(50, progress.lastId);
    }

    @Test
    public void deletedOrReusedProgressAnchorRequiresReview() {
        Source source = new Source();
        source.rows.put(1L, row(1, 1));
        source.rows.put(2L, row(2, 1));
        PagedCalls.Checkpoint progress = PagedCalls.capture(source).advance(source.rows.get(1L));
        source.rows.remove(1L);
        assertThrows(PagedCalls.SourceChanged.class, () -> PagedCalls.read(source, progress, 7));
        source.rows.put(1L, row(1, 2));
        assertThrows(PagedCalls.SourceChanged.class, () -> PagedCalls.read(source, progress, 7));
    }

    @Test
    public void namespaceResetOrUpperAnchorChangeCannotSkipHistory() {
        Source source = new Source();
        source.rows.put(7L, row(7, 1));
        PagedCalls.Checkpoint progress = PagedCalls.capture(source);
        source.namespace = "reset";
        assertThrows(PagedCalls.SourceChanged.class, () -> PagedCalls.read(source, progress, 7));
        source.namespace = "fixture";
        source.rows.clear();
        source.rows.put(1L, row(1, 1));
        assertThrows(PagedCalls.SourceChanged.class, () -> PagedCalls.read(source, progress, 7));
    }

    @Test
    public void rawPresentationChangesArePartOfAnchorBinding() {
        Source source = new Source();
        source.rows.put(
                1L,
                new CallRecord("fixture", "1", 1000, 1, 1, null, null, Integer.valueOf(2), null));
        PagedCalls.Checkpoint progress = PagedCalls.capture(source);
        source.rows.put(
                1L,
                new CallRecord("fixture", "1", 1000, 1, 1, null, null, Integer.valueOf(3), null));
        assertThrows(PagedCalls.SourceChanged.class, () -> PagedCalls.read(source, progress, 7));
    }

    @Test
    public void rejectsUnorderedOversizedOrIncompleteProviderPages() {
        Source source =
                new Source() {
                    @Override
                    public List<CallRecord> page(long after, long upper, int limit) {
                        return Arrays.asList(row(2, 1), row(1, 1));
                    }
                };
        source.rows.put(1L, row(1, 1));
        source.rows.put(2L, row(2, 1));
        PagedCalls.Checkpoint progress = PagedCalls.capture(source);
        assertThrows(PagedCalls.SourceChanged.class, () -> PagedCalls.read(source, progress, 7));
        assertThrows(PagedCalls.SourceChanged.class, () -> PagedCalls.read(source, progress, 1));
        Source empty =
                new Source() {
                    @Override
                    public List<CallRecord> page(long after, long upper, int limit) {
                        return Collections.emptyList();
                    }
                };
        empty.rows.put(1L, row(1, 1));
        assertThrows(
                PagedCalls.SourceChanged.class,
                () -> PagedCalls.read(empty, PagedCalls.capture(empty), 7));
    }

    @Test
    public void emptyAndMaximumIdSnapshotsDoNotOverflow() {
        Source source = new Source();
        assertTrue(PagedCalls.read(source, PagedCalls.capture(source), 7).isEmpty());
        source.rows.put(Long.MAX_VALUE, row(Long.MAX_VALUE, 1));
        PagedCalls.Checkpoint progress = PagedCalls.capture(source);
        CallRecord call = PagedCalls.read(source, progress, 7).get(0);
        assertTrue(PagedCalls.read(source, progress.advance(call), 7).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> PagedCalls.read(source, progress, 0));
        assertThrows(IllegalArgumentException.class, () -> PagedCalls.read(source, progress, 501));
    }

    @Test
    public void completedSnapshotExtendsWithoutReplayingProcessedIds() {
        Source source = new Source();
        source.rows.put(1L, row(1, 1));
        PagedCalls.Checkpoint done = PagedCalls.capture(source).advance(source.rows.get(1L));
        source.rows.put(2L, row(2, 1));
        PagedCalls.Checkpoint next = PagedCalls.extend(source, done);
        assertEquals(1, next.lastId);
        assertEquals(2, next.upperId);
        assertEquals("2", PagedCalls.read(source, next, 7).get(0).id);
        assertThrows(IllegalArgumentException.class, () -> PagedCalls.extend(source, next));
        source.rows.remove(1L);
        assertThrows(PagedCalls.SourceChanged.class, () -> PagedCalls.extend(source, done));
    }

    @Test
    public void emptySnapshotExtendsButNamespaceResetCannotBeAdopted() {
        Source source = new Source();
        PagedCalls.Checkpoint empty = PagedCalls.capture(source);
        source.rows.put(0L, row(0, 1));
        PagedCalls.Checkpoint next = PagedCalls.extend(source, empty);
        assertEquals(-1, next.lastId);
        assertEquals(0, next.upperId);
        source.namespace = "reset";
        assertThrows(PagedCalls.SourceChanged.class, () -> PagedCalls.extend(source, empty));
    }
}
