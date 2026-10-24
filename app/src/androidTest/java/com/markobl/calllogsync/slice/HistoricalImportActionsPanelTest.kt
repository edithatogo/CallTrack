package com.markobl.calllogsync.slice

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertContentDescriptionEquals
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

    @Test
    fun runActionsRequireEnabledStateAndExplicitTaps() {
        val state = mutableStateOf(HistoricalImportActionState())
        val actions = IntArray(4)

        compose.setContent {
            HistoricalImportRunActionsPanel(
                state = state.value,
                continueLabel = "Continue",
                confirmLabel = "Confirm import",
                stopLabel = "Stop",
                cancelLabel = "Cancel",
                onContinue = { actions[0]++ },
                onConfirm = { actions[1]++ },
                onStop = { actions[2]++ },
                onCancel = { actions[3]++ },
            )
        }

        val tags = listOf("import_continue", "import_confirm", "import_stop", "import_cancel")
        tags.forEach { compose.onNodeWithTag(it).assertIsNotEnabled() }
        compose.onNodeWithTag("import_stop").assertContentDescriptionEquals("Stop")
        compose.runOnIdle {
            assertEquals(listOf(0, 0, 0, 0), actions.toList())
            state.value =
                HistoricalImportActionState(
                    continueEnabled = true,
                    confirmEnabled = true,
                    stopEnabled = true,
                    cancelEnabled = true,
                )
        }

        tags.forEach { compose.onNodeWithTag(it).assertIsEnabled().performClick() }
        compose.runOnIdle { assertEquals(listOf(1, 1, 1, 1), actions.toList()) }
    }
}
