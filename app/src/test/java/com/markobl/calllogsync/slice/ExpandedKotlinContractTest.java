package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

import java.lang.reflect.Modifier;

/** Java-era contracts established before the next model/policy conversion batch. */
public class ExpandedKotlinContractTest {
    @Test
    public void callRecordOverloadsKeepRawPresentationAndNullableIdentity() {
        CallRecord legacy = new CallRecord("n😀", "id", 0, 0, null, null, null, true);
        assertEquals("Unknown", legacy.direction);
        assertEquals("3:n😀2:id", legacy.key());
        assertFalse(legacy.presentationSupplied);
        assertNull(legacy.numberPresentation);
        assertEquals(0, legacy.type);
        for (int type : new int[] {0, 1, 2, 3, 5, Integer.MAX_VALUE}) {
            CallRecord raw = new CallRecord("n", "x", 1, 2, type, null, null, (Integer) null, null);
            assertTrue(raw.presentationSupplied);
            assertFalse(raw.identityAllowed);
            assertNull(raw.name);
            assertNull(raw.number);
            assertNull(raw.phoneAccountId);
            assertEquals(type, raw.type);
        }
        assertTrue(
                new CallRecord("n", "x", 1, 2, 1, null, null, (Integer) 1, null).identityAllowed);
        assertThrows(
                IllegalArgumentException.class,
                () -> new CallRecord(null, "x", 0, 0, null, null, null, true));
        assertThrows(
                IllegalArgumentException.class,
                () -> new CallRecord("n", "", 0, 0, null, null, null, true));
    }

    @Test
    public void publicFieldAndStaticContractsRemainJavaCallable() throws Exception {
        assertEquals(long.class, DeliveryCounts.class.getField("verified").getType());
        assertTrue(Modifier.isFinal(CallRecord.class.getField("namespace").getModifiers()));
        assertEquals(Integer.class, CallRecord.class.getField("numberPresentation").getType());
        assertTrue(
                Modifier.isStatic(
                        ImportRange.class
                                .getMethod("parse", String.class, String.class, String.class)
                                .getModifiers()));
        assertTrue(
                Modifier.isStatic(
                        WorkStatus.Code.class.getMethod("parse", String.class).getModifiers()));
        assertEquals(7L * 24 * 60 * 60 * 1000, PayloadRetention.WINDOW_MILLIS);
        assertEquals(-1, new DeliveryCounts(-1, -2, -3, -4).pending);
    }

    @Test
    public void workStatusNullValidationSaturationAndStaleIdentityRemainStable() {
        assertSame(WorkStatus.Code.UNKNOWN, WorkStatus.Code.parse(null));
        assertSame(WorkStatus.Code.UNKNOWN, WorkStatus.Code.parse("future-code"));
        WorkStatus full = new WorkStatus(2, 0, "a", WorkStatus.Code.FAILED, 0, 0, Long.MAX_VALUE);
        assertEquals(
                Long.MAX_VALUE,
                WorkStatus.next(full, 2, 1, "b", WorkStatus.Code.FAILED, 0, 0).failures);
        assertSame(full, WorkStatus.next(full, 1, -1, null, null, -1, -1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new WorkStatus(1, 0, null, WorkStatus.Code.COMPLETE, 0, 0, 0));
        assertThrows(
                IllegalArgumentException.class, () -> new WorkStatus(1, 0, "x", null, 0, 0, 0));
    }

    @Test
    public void retentionAndRetryEdgeContractsRemainStable() {
        assertFalse(PayloadRetention.expired(null, Long.MAX_VALUE));
        assertFalse(PayloadRetention.expired(Long.MAX_VALUE, 0));
        assertTrue(PayloadRetention.expired(0L, PayloadRetention.WINDOW_MILLIS));
        assertThrows(IllegalArgumentException.class, () -> PayloadRetention.expired(null, -1));
        ScanDelivery.Batch failed =
                new ScanDelivery.Batch(0, 0, 0, 0, false, DurableDelivery.Result.FAILED);
        assertSame(AutomaticWorkPolicy.Action.RETRY, AutomaticWorkPolicy.decide(failed, 4));
        assertSame(AutomaticWorkPolicy.Action.STOP, AutomaticWorkPolicy.decide(failed, 5));
        assertThrows(NullPointerException.class, () -> AutomaticWorkPolicy.decide(null, 0));
    }
}
