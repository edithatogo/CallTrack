package com.markobl.calllogsync.slice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ManualCalendarScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun screenRendersWithoutEffectsAndDispatchesOnlyExplicitActions() {
        var accessRequests = 0
        var selectedToken: String? = null
        var logs = 0
        var automaticOpens = 0

        compose.setContent {
            ManualCalendarScreen(
                intro = "Manual logging explanation",
                calendarState =
                    ManualCalendarControlsState(
                        options =
                            listOf(
                                ManualCalendarOption("opaque-one", "Personal"),
                                ManualCalendarOption("opaque-two", "Work"),
                            ),
                        selectedToken = "opaque-one",
                        enabled = true,
                    ),
                logState = ManualLogUiState("Log latest call", "Ready", actionEnabled = true),
                chooseLabel = "Choose calendar",
                accessLabel = "Calendar access",
                unavailableLabel = "No calendars available",
                automaticLabel = "Automatic logging settings",
                onRequestAccess = { accessRequests++ },
                onSelect = { selectedToken = it },
                onLogLatest = { logs++ },
                onOpenAutomatic = { automaticOpens++ },
            )
        }

        compose.onNodeWithTag("slice_intro").assertTextEquals("Manual logging explanation")
        compose.onNodeWithTag("slice_calendar_controls_panel").assertIsDisplayed()
        compose.onNodeWithTag("slice_manual_panel").assertIsDisplayed()
        compose.onNodeWithTag("slice_status").assertTextEquals("Ready")
        compose.onNodeWithTag("slice_calendar_picker").assertIsDisplayed()
        compose.onNodeWithTag("open_automatic").assertIsDisplayed()
        assertEquals(0, accessRequests)
        assertEquals(null, selectedToken)
        assertEquals(0, logs)
        assertEquals(0, automaticOpens)

        compose.onNodeWithTag("slice_calendar_access").performClick()
        compose.onNodeWithTag("slice_calendar_picker").performClick()
        compose.onNodeWithTag("slice_calendar_option_1").performClick()
        compose.onNodeWithTag("slice_log_latest").performClick()
        compose.onNodeWithTag("open_automatic").performClick()

        assertEquals(1, accessRequests)
        assertEquals("opaque-two", selectedToken)
        assertEquals(1, logs)
        assertEquals(1, automaticOpens)
    }
}
