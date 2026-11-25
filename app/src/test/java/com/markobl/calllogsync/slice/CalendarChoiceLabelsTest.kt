// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarChoiceLabelsTest {
    @Test
    fun sameNameDoesNotCollapseDistinctCalendarDestinations() {
        val choices =
            listOf(
                AndroidCalendarSink.Choice(1, "Work", "provider", "account-a"),
                AndroidCalendarSink.Choice(2, "Work", "provider", "account-b"),
                AndroidCalendarSink.Choice(3, "Work", "provider", "account-a"),
                AndroidCalendarSink.Choice(4, "Personal", "provider", "account-a"),
            )
        assertEquals(
            mapOf(
                1L to "Work (account-a, 1)",
                2L to "Work (account-b)",
                3L to "Work (account-a, 3)",
                4L to "Personal",
            ),
            calendarChoiceLabels(choices, "Unnamed"),
        )
    }
}
