package com.markobl.calllogsync.slice

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class AutomaticStatusPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun rendersOnlyTheProvidedStatusAndSummary() {
        val uiState =
            AutomaticStatusUiState(
                state = "Automatic logging is off",
                counts = "Pending 1; verified 2; review 3; suppressed 4",
                feedback = "Settings saved",
                lastRun = "Complete; failures 0",
            )

        compose.setContent { AutomaticStatusPanel(uiState) }

        compose.onNodeWithTag("auto_state").assertIsDisplayed().assertTextEquals(uiState.state)
        compose
            .onNode(
                hasTestTag("auto_state") and
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.LiveRegion,
                        LiveRegionMode.Polite,
                    )
            )
            .assertIsDisplayed()
        compose.onNodeWithText(uiState.state).assertIsDisplayed()
        compose.onNodeWithText(uiState.counts).assertIsDisplayed()
        compose.onNodeWithText(uiState.feedback).assertIsDisplayed()
        compose.onNodeWithText(uiState.lastRun).assertIsDisplayed()
    }
}
