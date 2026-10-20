package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

public class ImportRangeTest {
    @Test
    public void datesUseCapturedTimezoneAndExclusiveEndAcrossDst() {
        ImportRange ordinary = ImportRange.parse("2026-10-01", "2026-10-02", "Australia/Brisbane");
        assertEquals(86400000L, ordinary.to - ordinary.from);
        ImportRange dst = ImportRange.parse("2026-03-08", "2026-03-09", "America/New_York");
        assertEquals(23 * 3600000L, dst.to - dst.from);
    }

    @Test
    public void invalidAndReversedDatesAreRejected() {
        assertThrows(
                java.time.DateTimeException.class,
                () -> ImportRange.parse("2026-02-30", "2026-03-02", "UTC"));
        assertThrows(
                IllegalArgumentException.class,
                () -> ImportRange.parse("2026-10-02", "2026-10-01", "UTC"));
    }

    @Test
    public void skippedLocalDateCannotSilentlyBecomeDifferentStartDate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ImportRange.parse("2011-12-30", "2012-01-01", "Pacific/Apia"));
    }
}
