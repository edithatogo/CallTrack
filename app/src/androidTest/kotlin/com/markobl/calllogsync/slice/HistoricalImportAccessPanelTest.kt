package com.markobl.calllogsync.slice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HistoricalImportAccessPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun accessRequestIsDispatchedOnlyAfterAnExplicitTap() {
        var requests = 0

        compose.setContent { HistoricalImportAccessPanel(label = "Grant access") { requests++ } }

        compose.onNodeWithTag("import_access_action").assertIsDisplayed()
        compose.onNodeWithText("Grant access").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, requests) }

        compose.onNodeWithTag("import_access_action").performClick()
        compose.runOnIdle { assertEquals(1, requests) }
    }
}
