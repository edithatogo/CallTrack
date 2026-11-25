// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.markobl.calllogsync.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AdaptiveCallTrackScreenTest {
    @get:Rule val compose = createComposeRule()

    @OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTestApi::class)
    @Test
    fun compactMediumAndExpandedWindowsKeepPrimaryActionAccessible() {
        val compact = DpSize(360.dp, 720.dp)
        val window = mutableStateOf(compact)
        var actions = 0
        compose.enableAccessibilityChecks()
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(window.value)) {
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
        }
        val ui = RenderedUi(InstrumentationRegistry.getInstrumentation())
        val shapes =
            listOf(
                Triple("compact", compact, 360.dp),
                Triple("medium", DpSize(720.dp, 900.dp), 640.dp),
                Triple("expanded", DpSize(1200.dp, 900.dp), 720.dp),
            )
        for ((index, shape) in shapes.withIndex()) {
            val (label, size, formWidth) = shape
            compose.runOnIdle {
                assertEquals(index, actions)
                window.value = size
            }
            compose.onNodeWithTag("calltrack_screen").assertWidthIsEqualTo(formWidth)
            compose
                .onNodeWithTag("slice_log_latest")
                .assertIsDisplayed()
                .assertIsEnabled()
                .assertHeightIsAtLeast(48.dp)
                .performClick()
            ui.capture("form-window-$label")
        }
        compose.runOnIdle { assertEquals(shapes.size, actions) }
    }
}
