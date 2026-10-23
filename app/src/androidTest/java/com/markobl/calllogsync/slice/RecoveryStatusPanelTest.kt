package com.markobl.calllogsync.slice

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class RecoveryStatusPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun feedbackIsRenderedAndUpdatedFromActivityState() {
        val feedback = mutableStateOf("Checking saved calendar effects.")

        compose.setContent { RecoveryStatusPanel(feedback.value) }

        compose.onNodeWithTag("recovery_feedback_text").assertIsDisplayed()
        compose.onNodeWithText("Checking saved calendar effects.").assertIsDisplayed()

        compose.runOnIdle { feedback.value = "Event verified without a write." }

        compose.onNodeWithText("Event verified without a write.").assertIsDisplayed()
    }
}
