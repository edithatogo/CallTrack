package org.calltrack.companion

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class CompanionScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun show() = compose.setContent { MaterialTheme { CompanionScreen() } }

    @Test
    fun previewRequiresExplicitDestination() {
        show()
        compose.onNodeWithText("Preview sample").assertIsNotEnabled()
        compose.onNodeWithText("Use sample calendar").performClick()
        compose.onNodeWithText("Preview sample").assertIsEnabled().performClick()
        compose
            .onNodeWithText("2 synthetic entries ready for review; nothing saved.")
            .assertExists()
    }

    @Test
    fun clearingDestinationInvalidatesPreview() {
        show()
        compose.onNodeWithText("Use sample calendar").performClick()
        compose.onNodeWithText("Preview sample").performClick()
        compose.onNodeWithText("Clear selection").performClick()
        compose.onNodeWithText("Preview sample").assertIsNotEnabled()
        compose
            .onNodeWithText("2 synthetic entries ready for review; nothing saved.")
            .assertDoesNotExist()
    }

    @Test
    fun reselectingDestinationDoesNotRestoreStalePreview() {
        show()
        compose.onNodeWithText("Use sample calendar").performClick()
        compose.onNodeWithText("Preview sample").performClick()
        compose.onNodeWithText("Clear selection").performClick()
        compose.onNodeWithText("Use sample calendar").performClick()
        compose.onNodeWithText("Preview sample").assertIsEnabled()
        compose
            .onNodeWithText("2 synthetic entries ready for review; nothing saved.")
            .assertDoesNotExist()
    }

    @Test
    fun capabilitiesAndSyntheticBoundaryStayVisible() {
        show()
        compose
            .onNodeWithText("Sample data only. No calendar connection or file import.")
            .assertExists()
        compose
            .onNodeWithText("Automatic cellular capture: unavailable in this experiment.")
            .assertExists()
        compose.onNodeWithText("Save to calendar").assertDoesNotExist()
    }
}
