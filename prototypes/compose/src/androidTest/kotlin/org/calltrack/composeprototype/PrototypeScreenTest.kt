package org.calltrack.composeprototype

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PrototypeScreenTest {
    @get:Rule val rule = createAndroidComposeRule<PrototypeActivity>()

    @Before
    fun checkForegroundWindow() {
        rule.waitForIdle()
        requirePrototypeWindow()
    }

    @Test
    fun launchAccessAndSelectionNeverEmitLogRequest() {
        rule.onNodeWithText("Log latest call").assertIsNotEnabled()
        rule.onNodeWithText("Synthetic requests: 0").assertExists()
        rule.onNodeWithText("Simulate calendar access").performClick()
        rule.onNodeWithText("Log latest call").assertIsNotEnabled()
        rule.onNodeWithText("Work fixture").performClick()
        rule.onNodeWithText("Log latest call").assertIsEnabled()
        rule.onNodeWithText("Synthetic requests: 0").assertExists()
        rule.waitForIdle()
        requirePrototypeWindow()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        File(instrumentation.targetContext.filesDir, "compose-prototype.png").outputStream().use {
            check(
                instrumentation.uiAutomation
                    .takeScreenshot()
                    .compress(Bitmap.CompressFormat.PNG, 100, it)
            )
        }
    }

    @Test
    fun explicitActionAndRecreationPreserveSelectionWithoutRepeatingEffect() {
        rule.onNodeWithText("Simulate calendar access").performClick()
        rule.onNodeWithText("Work fixture").performClick()
        rule.onNodeWithText("Log latest call").performClick()
        rule.onNodeWithText("Synthetic requests: 1").assertExists()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("Work fixture").assertIsSelected()
        rule.onNodeWithText("Synthetic requests: 1").assertExists()
    }

    @Test
    fun returningToPlaceholderDisablesAction() {
        rule.onNodeWithText("Simulate calendar access").performClick()
        rule.onNodeWithText("Personal fixture").performClick()
        rule.onNodeWithText("Choose a calendar").performClick()
        rule.onNodeWithText("Log latest call").assertIsNotEnabled()
        rule.onNodeWithText("Synthetic requests: 0").assertExists()
    }
}
