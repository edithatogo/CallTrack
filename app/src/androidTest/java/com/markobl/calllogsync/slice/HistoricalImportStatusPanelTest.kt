package com.markobl.calllogsync.slice

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class HistoricalImportStatusPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun statusAndFeedbackAreRenderedFromActivityState() {
        val state =
            mutableStateOf(
                HistoricalImportStatusUiState(
                    summary = "No import session selected.",
                    feedback = "",
                )
            )

        compose.setContent { HistoricalImportStatusPanel(state = state.value) }

        compose.onNodeWithTag("import_summary_text").assertIsDisplayed()
        compose.onNodeWithText("No import session selected.").assertIsDisplayed()
        compose.runOnIdle {
            state.value =
                state.value.copy(
                    summary = "Preview ready; awaiting confirmation.",
                    feedback = "Delivery paused safely.",
                )
        }

        compose.onNodeWithText("Preview ready; awaiting confirmation.").assertIsDisplayed()
        compose.onNodeWithTag("import_feedback_text").assertIsDisplayed()
        compose.onNodeWithText("Delivery paused safely.").assertIsDisplayed()
    }
}
