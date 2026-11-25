package com.markobl.calllogsync.slice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import com.markobl.calllogsync.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HistoricalImportDestinationPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun destinationListIsPresentedAndOnlyExplicitSelectionReturnsOpaqueToken() {
        val selected = mutableListOf<String>()
        val options =
            listOf(
                CalendarDestinationOption("opaque-1", "Personal calendar"),
                CalendarDestinationOption("opaque-2", "Work calendar"),
            )

        compose.setContent {
            CalendarDestinationPicker(
                state = CalendarDestinationUiState(options, "opaque-1", enabled = true),
                chooseLabel = "Choose a destination calendar",
                tagPrefix = "import_calendar",
                optionsEnabled = true,
                unavailableLabel = "Destination unavailable",
                onSelect = selected::add,
            )
        }

        compose.onNodeWithTag("import_calendar_picker").assertIsDisplayed()
        compose.onNodeWithTag("import_calendar_picker").assertTextEquals("Personal calendar")
        compose.onNodeWithText("Personal calendar").assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<String>(), selected) }

        compose.onNodeWithTag("import_calendar_picker").performClick()
        compose.onNodeWithTag("import_calendar_option_1").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(listOf("opaque-2"), selected) }
    }

    @Test
    fun unavailableDestinationCannotBeOpenedOrSelected() {
        val selected = mutableListOf<String>()

        compose.setContent {
            CalendarDestinationPicker(
                state = CalendarDestinationUiState(),
                chooseLabel = "Choose a destination calendar",
                tagPrefix = "import_calendar",
                optionsEnabled = true,
                unavailableLabel = "Destination unavailable",
                onSelect = selected::add,
            )
        }

        compose.onNodeWithTag("import_calendar_picker").assertIsNotEnabled()
        compose.onNodeWithTag("import_calendar_unavailable").assertIsDisplayed()
        compose.onNodeWithText("Destination unavailable").assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<String>(), selected) }
    }

    @Test
    fun keyboardSelectionRestoresPickerFocusWhenSaveFinishes() {
        val selected = mutableListOf<String>()
        val options =
            listOf(
                CalendarDestinationOption("opaque-1", "Personal calendar"),
                CalendarDestinationOption("opaque-2", "Work calendar"),
            )
        var destination by mutableStateOf(CalendarDestinationUiState(options, "opaque-1", true))
        compose.setContent {
            CalendarDestinationPicker(
                state = destination,
                chooseLabel = "Choose a destination calendar",
                tagPrefix = "import_calendar",
                onSelect = {
                    selected.add(it)
                    destination = destination.copy(enabled = false)
                },
            )
        }
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val collapsed =
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                resources.getString(R.string.calendar_picker_collapsed),
            )
        val expanded =
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                resources.getString(R.string.calendar_picker_expanded),
            )
        val picker = compose.onNodeWithTag("import_calendar_picker")
        val first = compose.onNodeWithTag("import_calendar_option_0")
        val second = compose.onNodeWithTag("import_calendar_option_1")

        compose.onRoot().performKeyInput { pressKey(Key.Tab) }
        picker.assertIsFocused().assert(collapsed).performKeyInput { pressKey(Key.Enter) }
        picker.assert(expanded).performKeyInput { pressKey(Key.Tab) }
        first.assertIsFocused().performKeyInput { pressKey(Key.Tab) }
        second.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        picker.assertIsNotEnabled().assert(collapsed)
        second.assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(listOf("opaque-2"), selected)
            destination = destination.copy(selectedToken = selected.single(), enabled = true)
        }
        picker.assertTextEquals("Work calendar").assertIsFocused()
    }
}
