// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.markobl.calllogsync.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AdaptiveCallTrackScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun compactWindowKeepsPrimaryActionAccessible() = checkForm("compact", 360.dp)

    @Test
    fun mediumWindowKeepsPrimaryActionAccessible() = checkForm("medium", 640.dp)

    @Test
    fun expandedWindowKeepsPrimaryActionAccessible() = checkForm("expanded", 720.dp)

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    private fun checkForm(profile: String, formWidth: Dp) {
        var actions = 0
        compose.enableAccessibilityChecks()
        compose.setContent {
            MaterialExpressiveTheme {
                CallTrackScaffold(R.string.calltrack_title, onNavigateUp = null) {
                    ActionButton(
                        label = stringResource(R.string.slice_log_latest),
                        tag = "slice_log_latest",
                        enabled = true,
                        onClick = { actions++ },
                    )
                }
            }
        }
        compose.runOnIdle { assertEquals(0, actions) }
        compose.onNodeWithTag("calltrack_screen").assertWidthIsEqualTo(formWidth)
        compose
            .onNodeWithTag("slice_log_latest")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        RenderedUi(InstrumentationRegistry.getInstrumentation()).capture("form-window-$profile")
        compose.runOnIdle { assertEquals(1, actions) }
    }
}
