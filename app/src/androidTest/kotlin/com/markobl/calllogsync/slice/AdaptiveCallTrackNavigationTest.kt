// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import android.accessibilityservice.AccessibilityService
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AdaptiveCallTrackNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<CalendarSliceActivity>()

    @Test
    fun expandedSettingsShowsOneActiveDetailWithAccessibleNavigation() {
        compose.enableAccessibilityChecks()
        val ui = RenderedUi(InstrumentationRegistry.getInstrumentation())
        compose.onNodeWithTag("open_automatic").performScrollTo().performClick()
        compose.onNodeWithTag("settings_navigation").assertDoesNotExist()
        compose.onNodeWithTag("open_imports").performScrollTo().performClick()

        val imports = compose.onNodeWithTag("section_imports")
        imports.assertIsDisplayed().assertIsSelected().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("section_automatic").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        val importForm = compose.onNodeWithTag("import_from").performScrollTo().assertIsDisplayed()
        assertTrue(
            "Settings navigation and the active import form must appear side by side",
            imports.getUnclippedBoundsInRoot().right < importForm.getUnclippedBoundsInRoot().left,
        )
        compose.onNodeWithTag("auto_enable").assertDoesNotExist()
        assertFalse(
            "Opening Imports displayed the keyboard before a date tap",
            ui.keyboardVisible(),
        )
        ui.capture("expanded-settings-import")
        importForm.performTouchInput { click() }.assertIsFocused()
        ui.await(ui::keyboardVisible, "Tapping a date field did not open the keyboard")
        ui.capture("expanded-settings-import-keyboard")

        val recovery = compose.onNodeWithTag("section_recovery")
        recovery.assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        recovery.assertIsSelected()
        ui.await({ !ui.keyboardVisible() }, "Leaving Imports did not close the keyboard")
        compose.onNodeWithTag("recovery_refresh_action").assertIsDisplayed()
        compose.onNodeWithTag("import_from").assertDoesNotExist()
        compose.onNodeWithTag("auto_enable").assertDoesNotExist()
        ui.capture("expanded-settings-recovery")

        assertTrue(
            "Android must accept the Back action",
            ui.automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK),
        )
        compose.onNodeWithTag("open_recovery").assertIsDisplayed()
        compose.onNodeWithTag("settings_navigation").assertDoesNotExist()
        compose.onNodeWithTag("recovery_refresh_action").assertDoesNotExist()
        assertFalse(SettingsTestFixture.snapshot(compose.activity).hasAutomatic())
        SqliteImportStore(compose.activity).use { assertTrue(it.sessions(-1, 10).isEmpty()) }
    }
}
