package com.markobl.calllogsync.slice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ManualLogPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun loggingRequiresAnExplicitTapAndASelectedDestination() {
        val state =
            mutableStateOf(
                ManualLogUiState(
                    logLabel = "Log latest call",
                    status = "Choose a calendar before logging a call.",
                    actionEnabled = false,
                )
            )
        var actions = 0

        compose.setContent { ManualLogPanel(state = state.value, onLogLatest = { actions++ }) }

        compose.onNodeWithTag("slice_log_latest").assertIsNotEnabled()
        compose.onNodeWithText("Choose a calendar before logging a call.").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals("Composition and a disabled action must not deliver", 0, actions)
            state.value = state.value.copy(actionEnabled = true, status = "Ready")
        }

        compose.onNodeWithTag("slice_log_latest").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals("Only the explicit tap delivers", 1, actions) }
    }

    @Test
    fun calendarChoiceIsExplicitAndBoundToTheCurrentLoadToken() {
        val staleToken = "prior-load-token"
        val currentToken = "current-load-token"
        var state by
            mutableStateOf(
                ManualCalendarControlsState(
                    options = listOf(ManualCalendarOption(currentToken, "Work calendar")),
                    selectedToken = staleToken,
                    enabled = true,
                )
            )
        var selectedToken: String? = null
        var accessRequests = 0

        compose.setContent {
            ManualCalendarControlsPanel(
                state = state,
                chooseLabel = "Choose calendar",
                accessLabel = "Calendar access",
                unavailableLabel = "No calendars",
                onRequestAccess = { accessRequests++ },
                onSelect = { token ->
                    selectedToken = token
                    state = state.copy(selectedToken = token)
                },
            )
        }

        compose.runOnIdle {
            assertEquals(null, selectedToken)
            assertEquals(0, accessRequests)
            assertEquals(
                "A token from a previous provider read is not a current selection",
                staleToken,
                state.selectedToken,
            )
        }
        compose.onNodeWithText("Choose calendar").assertIsDisplayed()
        compose.onNodeWithTag("slice_calendar_access").assertIsDisplayed().performClick()
        compose.onNodeWithTag("slice_calendar_picker").assertIsDisplayed().performClick()
        compose.onNodeWithTag("slice_calendar_option_0").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(currentToken, selectedToken)
            assertEquals(currentToken, state.selectedToken)
            assertEquals(1, accessRequests)
        }
    }
}
