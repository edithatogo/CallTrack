package com.markobl.calllogsync.slice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AutomaticSettingsCopyPanelsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun introRendersAndPermissionRequestOccursOnlyAfterExplicitTap() {
        val requests = mutableListOf<String>()
        val intro = "Automatic logging needs explicit permissions."

        compose.setContent {
            AutomaticSettingsIntroPanel(intro)
            AutomaticSettingsAccessPanel(
                "Grant access",
                onRequestAccess = { requests += "request" },
            )
        }

        compose.onNodeWithTag("auto_intro_text").assertIsDisplayed()
        compose.onNodeWithText(intro).assertIsDisplayed()
        compose.onNodeWithTag("auto_access_action").assertIsDisplayed()
        assertEquals(emptyList<String>(), requests)

        compose.onNodeWithTag("auto_access_action").performClick()

        assertEquals(listOf("request"), requests)
    }
}
