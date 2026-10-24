package com.markobl.calllogsync.slice

import android.content.Context
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class AutomaticSettingsCallTypesTest {
    @get:Rule val compose = createAndroidComposeRule<AutomaticSettingsActivity>()

    @Test
    fun selectionsSurviveActivityRecreationWithoutEnablingAutomaticLogging() {
        compose.onNodeWithTag("auto_outgoing").performClick()
        compose.onNodeWithTag("auto_outgoing").assertIsOff()

        compose.activityRule.scenario.recreate()
        compose.waitForIdle()

        compose.onNodeWithTag("auto_incoming").assertIsOn()
        compose.onNodeWithTag("auto_outgoing").assertIsOff()
        compose.onNodeWithTag("auto_missed").assertIsOn()
        compose.activityRule.scenario.onActivity { activity ->
            assertFalse(
                activity
                    .getSharedPreferences("slice", Context.MODE_PRIVATE)
                    .contains("automaticActivation")
            )
        }
    }
}
