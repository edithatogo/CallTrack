package com.markobl.calllogsync.slice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class HistoricalImportIntroPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun explanatoryTextRendersWithoutStartingImportWork() {
        compose.setContent {
            HistoricalImportIntroPanel("Import calls from the selected date range")
        }

        compose.onNodeWithTag("import_intro_text").assertIsDisplayed()
        compose.onNodeWithText("Import calls from the selected date range").assertIsDisplayed()
    }
}
