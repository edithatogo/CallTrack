package com.markobl.calllogsync.slice

import java.time.LocalDate
import java.time.ZoneId

/** Calendar dates in one captured timezone, with an exclusive end date. */
class ImportRange
private constructor(
    @JvmField val from: Long,
    @JvmField val to: Long,
    @JvmField val timezone: String,
) {
    companion object {
        @JvmStatic
        fun parse(start: String?, end: String?, timezone: String?): ImportRange {
            val zone = ZoneId.of(timezone)
            val first = LocalDate.parse(start)
            val last = LocalDate.parse(end)
            val firstInstant = first.atStartOfDay(zone)
            val lastInstant = last.atStartOfDay(zone)
            require(firstInstant.toLocalDate() == first && lastInstant.toLocalDate() == last) {
                "Date does not exist in selected timezone"
            }
            val from = firstInstant.toInstant().toEpochMilli()
            val to = lastInstant.toInstant().toEpochMilli()
            require(from >= 0 && to > from) { "Invalid import range" }
            return ImportRange(from, to, timezone!!)
        }
    }
}
