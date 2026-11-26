package com.markobl.calllogsync.slice

import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.platform.app.InstrumentationRegistry
import com.markobl.calllogsync.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistoricalImportDestinationPanelTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

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
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val resources = instrumentation.targetContext.resources
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

        picker.assertIsDisplayed()
        val ui = RenderedUi(instrumentation)
        focusPicker(ui, picker)
        picker.assertIsFocused().assert(collapsed)
        ui.pressKeyboardKey(KeyEvent.KEYCODE_ENTER)
        picker.assert(expanded)
        ui.pressKeyboardKey(KeyEvent.KEYCODE_TAB)
        first.assertIsFocused()
        ui.pressKeyboardKey(KeyEvent.KEYCODE_TAB)
        second.assertIsFocused()
        ui.pressKeyboardKey(KeyEvent.KEYCODE_ENTER)
        picker.assertIsNotEnabled().assert(collapsed)
        second.assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(listOf("opaque-2"), selected)
            destination = destination.copy(selectedToken = selected.single(), enabled = true)
        }
        picker.assertTextEquals("Work calendar").assertIsFocused()
    }

    private fun focusPicker(ui: RenderedUi, picker: SemanticsNodeInteraction) {
        ui.await(
            { compose.runOnUiThread { compose.activity.hasWindowFocus() } },
            "The picker test window must receive keyboard focus",
        )
        ui.pressKeyboardKey(KeyEvent.KEYCODE_TAB)
        picker.performSemanticsAction(SemanticsActions.RequestFocus) { requestFocus ->
            assertTrue("The picker must accept keyboard focus", requestFocus())
        }
        assertTrue(
            "Android must be in keyboard mode",
            compose.runOnUiThread { !compose.activity.window.decorView.isInTouchMode },
        )
    }

    private fun RenderedUi.pressKeyboardKey(keyCode: Int) {
        val now = SystemClock.uptimeMillis()
        val down =
            KeyEvent(
                now,
                now,
                KeyEvent.ACTION_DOWN,
                keyCode,
                0,
                0,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                0,
                InputDevice.SOURCE_KEYBOARD,
            )
        assertTrue(
            "Android must accept keyboard down for $keyCode",
            automation.injectInputEvent(down, true),
        )
        val up = KeyEvent.changeAction(down, KeyEvent.ACTION_UP)
        assertTrue(
            "Android must accept keyboard up for $keyCode",
            automation.injectInputEvent(up, true),
        )
    }
}
