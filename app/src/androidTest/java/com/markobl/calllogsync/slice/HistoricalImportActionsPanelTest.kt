package com.markobl.calllogsync.slice

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HistoricalImportActionsPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun createActionRequiresActivityEnabledStateAndAnExplicitTap() {
        val state = mutableStateOf(HistoricalImportActionState(createEnabled = false))
        var actions = 0

        compose.setContent {
            HistoricalImportCreateActionPanel(
                state = state.value,
                label = "Create preview",
                onCreate = { actions++ },
            )
        }

        compose.onNodeWithTag("import_create").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(0, actions)
            state.value = state.value.copy(createEnabled = true)
        }
        compose.onNodeWithTag("import_create").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, actions) }
    }
}
