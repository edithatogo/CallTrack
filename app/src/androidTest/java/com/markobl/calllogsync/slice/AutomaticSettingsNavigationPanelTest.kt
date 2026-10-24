package com.markobl.calllogsync.slice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AutomaticSettingsNavigationPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun destinationsOpenOnlyAfterExplicitAction() {
        val opened = mutableListOf<String>()
        compose.setContent {
            AutomaticSettingsNavigationPanel(
                importLabel = "Import call history",
                recoveryLabel = "Review delivery records",
                onOpenImports = { opened += "imports" },
                onOpenRecovery = { opened += "recovery" },
            )
        }

        compose.onNodeWithTag("open_imports").assertIsDisplayed()
        compose.onNodeWithTag("open_recovery").assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<String>(), opened) }
        compose.onNodeWithTag("open_imports").performClick()
        compose.onNodeWithTag("open_recovery").performClick()
        compose.runOnIdle { assertEquals(listOf("imports", "recovery"), opened) }
    }
}
