package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

import java.lang.reflect.Modifier;

/** Golden outputs captured from the pre-migration Java implementation at f163e4c. */
public class KotlinMigrationContractTest {
    private CallRecord[] fixtures() {
        return new CallRecord[] {
            new CallRecord("fixture", "1", 1234, 0, 1, "Cached", "000", true, "SIM"),
            new CallRecord("ns😀", "id:|", 1700000000123L, 65, 2, "\u2003", " 000 ", true, null),
            new CallRecord("fixture", "3", 1000, 7, 3, "Secret", "123", false, null),
            new CallRecord("fixture", "4", 999, 1, 5, " \t\r\n", null, true, null)
        };
    }

    @Test
    public void storedSourceAndEventFingerprintsMatchJavaGoldenOutputs() {
        String[] signatures = {
            "9f344402a6d4cc91192a65df83af1c9a7dd1cd04684b826b9fd33dde3e3a1bc9",
            "8d747459d88eada6087787805de80e6e5844e85bf7443669510bc1ce67d310c2",
            "49eb18c0a30e7e32f76f1eb2c1bab7a19b86c82b068abc2bffba71871226a6fa",
            "1b364cde79cd30cbd2410da6ca46f8ea4a3b87ee0cb12b936eb3a851f2708c2b"
        };
        String[] fingerprints = {
            "c19af50b5118168637d7c85bc6d34bce6739086cbc4510d416bbd2d7799539b1",
            "439ffeba8570abe0145127f915372c9568eec1c5852b388b4939f640ad692455",
            "836734e9bd0f8ed3b6cc49d37c1283e5d0155a3a6de62b44e42674269a2e7a97",
            "a887fbe6999f733b5d7194a63e4081a63c2a6faa5d16e7adb1fdf5a0212fcac9"
        };
        CallRecord[] calls = fixtures();
        for (int i = 0; i < calls.length; i++) {
            assertEquals(signatures[i], CallIdentity.signature(calls[i]));
            assertEquals(fingerprints[i], CallEvent.from(calls[i], "UTC").fingerprint());
        }
    }

    @Test
    public void legacyNullableConstructorFieldsRemainHashableAndDistinctFromEmpty() {
        CallEvent absent = new CallEvent(null, null, 0, 0, null, null);
        assertNull(absent.title);
        assertNull(absent.description);
        assertNull(absent.timezone);
        assertNull(absent.marker);
        assertNotEquals(absent.fingerprint(), new CallEvent("", "", 0, 0, "", "").fingerprint());
        assertThrows(NullPointerException.class, () -> CallEvent.from(fixtures()[0], null));
    }

    @Test
    public void javaAsciiTrimmingIsPreservedRatherThanUnicodeTrimming() {
        assertEquals("Outgoing: \u2003", CallEvent.from(fixtures()[1], "UTC").title);
        assertEquals("Unknown: Unknown number", CallEvent.from(fixtures()[3], "UTC").title);
    }

    @Test
    public void javaPublicFieldsConstructorsAndStaticMethodsRemainAvailable() throws Exception {
        assertTrue(Modifier.isFinal(CallEvent.class.getField("title").getModifiers()));
        assertEquals(long.class, CallEvent.class.getField("start").getType());
        assertTrue(
                Modifier.isStatic(
                        CallEvent.class
                                .getMethod("from", CallRecord.class, String.class)
                                .getModifiers()));
        assertTrue(
                Modifier.isStatic(
                        CallIdentity.class
                                .getMethod("signature", CallRecord.class)
                                .getModifiers()));
        assertTrue(
                Modifier.isStatic(
                        CallSelection.class
                                .getMethod("inRange", CallRecord.class, long.class, long.class)
                                .getModifiers()));
        CallSelection selection = new CallSelection(true, false, true);
        assertTrue(selection.incoming);
        assertFalse(selection.outgoing);
        assertTrue(selection.missed);
    }

    @Test
    public void exceptionCategoriesAndRangeBoundariesRemainStable() {
        assertThrows(NullPointerException.class, () -> CallIdentity.signature(null));
        assertThrows(
                NullPointerException.class,
                () -> new CallSelection(true, true, true).accepts(null));
        assertThrows(IllegalArgumentException.class, () -> CallSelection.inRange(null, -1, 1));
        assertThrows(
                ArithmeticException.class,
                () ->
                        CallEvent.from(
                                new CallRecord(
                                        "n", "x", Long.MAX_VALUE, 1, 1, null, null, true, null),
                                "UTC"));
        assertTrue(CallSelection.inRange(fixtures()[0], 1234, 1235));
        assertFalse(CallSelection.inRange(fixtures()[0], 0, 1234));
    }
}
