package com.markobl.calllogsync.slice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AutomaticSettingsActionsPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun effectsDispatchOnlyFromEnabledExplicitActions() {
        var enabledRequests = 0
        var pauseRequests = 0
        var refreshRequests = 0
        compose.setContent {
            AutomaticSettingsActionsPanel(
                state =
                    AutomaticSettingsActionsUiState(
                        enableEnabled = false,
                        pauseEnabled = true,
                        refreshEnabled = true,
                    ),
                enableLabel = "Enable",
                pauseLabel = "Pause",
                refreshLabel = "Refresh",
                onEnable = { enabledRequests++ },
                onPause = { pauseRequests++ },
                onRefresh = { refreshRequests++ },
            )
        }

        compose.onNodeWithTag("auto_enable").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithTag("auto_pause").assertIsEnabled().performClick()
        compose.onNodeWithTag("auto_refresh").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(0, enabledRequests)
            assertEquals(1, pauseRequests)
            assertEquals(1, refreshRequests)
        }
    }

    @Test
    fun enabledActionDispatchesOnlyWhenClicked() {
        var enableRequests = 0
        compose.setContent {
            AutomaticSettingsActionsPanel(
                state = AutomaticSettingsActionsUiState(enableEnabled = true),
                enableLabel = "Enable",
                pauseLabel = "Pause",
                refreshLabel = "Refresh",
                onEnable = { enableRequests++ },
                onPause = {},
                onRefresh = {},
            )
        }

        compose.runOnIdle { assertEquals(0, enableRequests) }
        compose.onNodeWithTag("auto_enable").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, enableRequests) }
    }
}
