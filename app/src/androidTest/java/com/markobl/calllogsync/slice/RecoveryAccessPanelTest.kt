package com.markobl.calllogsync.slice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RecoveryAccessPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun accessRequestRunsOnlyAfterExplicitAction() {
        var requests = 0
        compose.setContent { RecoveryAccessPanel("Allow calendar read access") { requests++ } }

        compose.onNodeWithTag("recovery_access_action").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, requests) }
        compose.onNodeWithTag("recovery_access_action").performClick()
        compose.runOnIdle { assertEquals(1, requests) }
    }
}
