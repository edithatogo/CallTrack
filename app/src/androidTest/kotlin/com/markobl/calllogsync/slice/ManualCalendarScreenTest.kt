package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ManualCalendarScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun screenRendersWithoutEffectsAndDispatchesOnlyExplicitActions() {
        var accessRequests = 0
        var selectedToken: String? = null
        var logs = 0
        var automaticOpens = 0

        compose.setContent {
            ManualCalendarScreen(
                intro = "Manual logging explanation",
                calendarState =
                    CalendarDestinationUiState(
                        options =
                            listOf(
                                CalendarDestinationOption("opaque-one", "Personal"),
                                CalendarDestinationOption("opaque-two", "Work"),
                            ),
                        selectedToken = "opaque-one",
                        enabled = true,
                    ),
                logState = ManualLogUiState("Log latest call", "Ready", actionEnabled = true),
                chooseLabel = "Choose calendar",
                accessLabel = "Calendar access",
                unavailableLabel = "No calendars available",
                automaticLabel = "Automatic logging settings",
                onRequestAccess = { accessRequests++ },
                onSelect = { selectedToken = it },
                onLogLatest = { logs++ },
                onOpenAutomatic = { automaticOpens++ },
            )
        }

        compose.onNodeWithTag("slice_intro").assertTextEquals("Manual logging explanation")
        compose.onNodeWithTag("slice_calendar_controls_panel").assertIsDisplayed()
        compose.onNodeWithTag("slice_manual_panel").assertIsDisplayed()
        compose.onNodeWithTag("slice_status").assertTextEquals("Ready")
        compose.onNodeWithTag("slice_calendar_picker").assertIsDisplayed()
        compose.onNodeWithTag("open_automatic").assertIsDisplayed()
        assertEquals(0, accessRequests)
        assertEquals(null, selectedToken)
        assertEquals(0, logs)
        assertEquals(0, automaticOpens)

        compose.onNodeWithTag("slice_calendar_access").performClick()
        compose.onNodeWithTag("slice_calendar_picker").performClick()
        compose.onNodeWithTag("slice_calendar_option_1").performClick()
        compose.onNodeWithTag("slice_log_latest").performClick()
        compose.onNodeWithTag("open_automatic").performClick()

        assertEquals(1, accessRequests)
        assertEquals("opaque-two", selectedToken)
        assertEquals(1, logs)
        assertEquals(1, automaticOpens)
    }

    @Test
    fun largeTextAndRtlKeepManualActionReachable() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var actions = 0
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(density, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    ManualCalendarScreen(
                        intro = "Manual logging explanation",
                        calendarState =
                            CalendarDestinationUiState(
                                options = listOf(CalendarDestinationOption("work", "Work fixture")),
                                selectedToken = "work",
                                enabled = true,
                            ),
                        logState = ManualLogUiState("Log latest call", "Ready", true),
                        chooseLabel = "Choose a calendar",
                        accessLabel = "Calendar access",
                        unavailableLabel = "No calendars",
                        automaticLabel = "Automatic logging settings",
                        onRequestAccess = { error("Access requires its own explicit action") },
                        onSelect = { error("Selection requires its own explicit action") },
                        onLogLatest = { actions++ },
                        onOpenAutomatic = { error("Navigation requires its own explicit action") },
                    )
                }
            }
        }
        val actionTag = "slice_log_latest"
        compose.onNodeWithTag(actionTag).performScrollTo().assertIsDisplayed().assertIsEnabled()
        val ui = RenderedUi(instrumentation)
        ui.await({ ui.find(actionTag, true) != null }, "Manual action must be in the foreground")
        val root = checkNotNull(ui.automation.rootInActiveWindow)
        assertEquals(instrumentation.targetContext.packageName, root.packageName.toString())
        val controls = intArrayOf(0)
        PositiveUiTest.assertRenderedAccessibilityControls(
            root,
            controls,
            instrumentation.targetContext.resources.displayMetrics.density,
        )
        assertTrue("Expected accessible controls in the launcher's foreground", controls[0] > 0)
        compose.runOnIdle { assertEquals(0, actions) }
        ui.tapId(actionTag)
        compose.runOnIdle { assertEquals("Only the explicit tap delivers", 1, actions) }
        ui.capture("manual-large-text-rtl")
    }
}
