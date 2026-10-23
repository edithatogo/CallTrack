package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

public class CallSelectionTest {
    private CallRecord call(int type) {
        return new CallRecord("fixture", "1", 1000, 0, type, "Cached", "000", true, "SIM-fixture");
    }

    @Test
    public void independentFiltersCoverAllCombinationsAndExcludeExtendedTypes() {
        for (int mask = 0; mask < 8; mask++) {
            CallSelection filter =
                    new CallSelection((mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0);
            for (int type = 1; type <= 3; type++)
                assertEquals((mask & (1 << (type - 1))) != 0, filter.accepts(call(type)));
            for (int type : new int[] {0, 4, 5, 6, 7, 8, -1, 999})
                assertFalse(filter.accepts(call(type)));
        }
    }

    @Test
    public void rawExtendedTypeAndSimMetadataAreNotReinterpretedAsMissed() {
        CallRecord rejected = call(5);
        assertEquals(5, rejected.type);
        assertEquals("Unknown", rejected.direction);
        assertEquals("SIM-fixture", rejected.phoneAccountId);
    }

    @Test
    public void rangesAreStartInclusiveEndExclusiveAndPreserveMilliseconds() {
        assertTrue(CallSelection.inRange(call(1), 1000, 1001));
        assertFalse(CallSelection.inRange(call(1), 0, 1000));
        assertThrows(
                IllegalArgumentException.class, () -> CallSelection.inRange(call(1), 1001, 1000));
    }
}
