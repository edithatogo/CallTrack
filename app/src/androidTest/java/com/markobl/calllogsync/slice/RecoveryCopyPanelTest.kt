package com.markobl.calllogsync.slice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class RecoveryCopyPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun introAndRetentionCopyRenderOnlyActivitySuppliedText() {
        val intro = "Review saved delivery records without changing calendar events."
        val retention = "Pending payloads expire after seven days from storage."

        compose.setContent {
            RecoveryCopyPanel(intro, "recovery_intro_text")
            RecoveryCopyPanel(retention, "recovery_retention_copy_text")
        }

        compose.onNodeWithTag("recovery_intro_text").assertIsDisplayed()
        compose.onNodeWithText(intro).assertIsDisplayed()
        compose.onNodeWithTag("recovery_retention_copy_text").assertIsDisplayed()
        compose.onNodeWithText(retention).assertIsDisplayed()
    }
}
