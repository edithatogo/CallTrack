package org.calltrack.composeprototype

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PrototypeLayoutTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun accessibleControlsEmitOneExplicitRequest() {
        var requests = 0
        rule.setContent {
            MaterialTheme {
                PreviewScreen(
                    PreviewState(calendarAccess = true, selected = 7),
                    {},
                    {},
                    { requests++ },
                )
            }
        }
        rule.waitForIdle()
        requirePrototypeWindow()
        rule.enableAccessibilityChecks()
        rule.onRoot().tryPerformAccessibilityChecks()
        rule.onNodeWithText("Log latest call").performClick()
        rule.runOnIdle { assertEquals(1, requests) }
    }

    @Test
    fun largeFontAndRtlKeepActionReachable() {
        var requests = 0
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(density, 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                MaterialTheme {
                    PreviewScreen(
                        PreviewState(calendarAccess = true, selected = 9),
                        {},
                        {},
                        { requests++ },
                    )
                }
            }
        }
        rule.waitForIdle()
        requirePrototypeWindow()
        rule.enableAccessibilityChecks()
        rule.onNodeWithText("Log latest call").performScrollTo().performClick()
        rule.onRoot().tryPerformAccessibilityChecks()
        rule.runOnIdle { assertEquals(1, requests) }
    }
}
