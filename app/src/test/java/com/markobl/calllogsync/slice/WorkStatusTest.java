package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

public class WorkStatusTest {
    @Test
    public void staleGenerationCannotReplaceNewerStatus() {
        WorkStatus current = WorkStatus.next(null, 2, 1000, "new", WorkStatus.Code.COMPLETE, 4, 0);
        assertSame(current, WorkStatus.next(current, 1, 2000, "old", WorkStatus.Code.FAILED, 0, 0));
    }

    @Test
    public void failureCountsArePerGenerationAndDoNotDoubleCountOneReport() {
        WorkStatus first = WorkStatus.next(null, 1, 1000, "a", WorkStatus.Code.FAILED, 0, 0);
        assertEquals(1, first.failures);
        WorkStatus repeated = WorkStatus.next(first, 1, 1001, "a", WorkStatus.Code.FAILED, 0, 0);
        assertEquals(1, repeated.failures);
        WorkStatus second = WorkStatus.next(repeated, 1, 1002, "b", WorkStatus.Code.FAILED, 0, 0);
        assertEquals(2, second.failures);
        assertEquals(
                2, WorkStatus.next(second, 1, 1003, "c", WorkStatus.Code.COMPLETE, 1, 0).failures);
        assertEquals(
                0, WorkStatus.next(second, 2, 1004, "d", WorkStatus.Code.COMPLETE, 1, 0).failures);
    }
}
