package com.markobl.calllogsync.slice;

import java.time.LocalDate;
import java.time.ZoneId;

/** Calendar dates interpreted in one captured device timezone, with an exclusive end date. */
public final class ImportRange {
    public final long from, to;
    public final String timezone;

    private ImportRange(long from, long to, String timezone) {
        this.from = from;
        this.to = to;
        this.timezone = timezone;
    }

    public static ImportRange parse(String start, String end, String timezone) {
        ZoneId zone = ZoneId.of(timezone);
        LocalDate first = LocalDate.parse(start), last = LocalDate.parse(end);
        java.time.ZonedDateTime firstInstant = first.atStartOfDay(zone),
                lastInstant = last.atStartOfDay(zone);
        if (!firstInstant.toLocalDate().equals(first) || !lastInstant.toLocalDate().equals(last))
            throw new IllegalArgumentException("Date does not exist in selected timezone");
        long from = firstInstant.toInstant().toEpochMilli();
        long to = lastInstant.toInstant().toEpochMilli();
        if (from < 0 || to <= from) throw new IllegalArgumentException("Invalid import range");
        return new ImportRange(from, to, timezone);
    }
}
