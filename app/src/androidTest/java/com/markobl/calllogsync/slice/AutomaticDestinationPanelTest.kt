package com.markobl.calllogsync.slice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AutomaticDestinationPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun providerChoiceIsDeliveredOnlyAfterExplicitSelection() {
        val opaqueToken = "load-token-1"
        var state by
            mutableStateOf(
                AutomaticDestinationUiState(
                    options = listOf(AutomaticDestinationOption(opaqueToken, "Work calendar")),
                    enabled = true,
                )
            )
        var selectedToken: String? = null

        compose.setContent {
            AutomaticDestinationPanel(state, "Choose calendar") { token ->
                selectedToken = token
                state = state.copy(selectedToken = token)
            }
        }

        compose.runOnIdle {
            assertEquals(null, selectedToken)
            assertTrue(state.selectedToken == null)
        }
        compose.onNodeWithTag("auto_calendar_picker").assertIsDisplayed().performClick()
        compose.onNodeWithTag("auto_calendar_option_0").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(opaqueToken, selectedToken)
            assertEquals(opaqueToken, state.selectedToken)
        }
    }
}
