package com.markobl.calllogsync.slice

import androidx.compose.runtime.mutableStateOf
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
}
