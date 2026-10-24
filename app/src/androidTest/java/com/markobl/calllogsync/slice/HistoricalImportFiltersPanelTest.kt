package com.markobl.calllogsync.slice

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistoricalImportFiltersPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun dateAndCallTypeFiltersEmitOnlyExplicitUserChanges() {
        var state by mutableStateOf(HistoricalImportFiltersUiState())
        var changes = 0

        compose.setContent {
            HistoricalImportFiltersPanel(
                state = state,
                fromLabel = "From",
                toLabel = "To",
                incomingLabel = "Incoming",
                outgoingLabel = "Outgoing",
                missedLabel = "Missed",
                onStateChange = {
                    state = it
                    changes++
                },
            )
        }

        compose.runOnIdle {
            assertEquals(0, changes)
            assertTrueFilters(state)
        }
        compose.onNodeWithTag("import_from").performTextInput("2026-10-01")
        compose.onNodeWithTag("import_outgoing").performClick()
        compose.runOnIdle {
            assertEquals("2026-10-01", state.fromDate)
            assertFalse(state.outgoing)
            assertEquals(2, changes)
        }
    }

    @Test
    fun controlsRemainAccessibleWithLargeFontAndRightToLeftLayout() {
        compose.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(density = 1f, fontScale = 2f),
            ) {
                HistoricalImportFiltersPanel(
                    state = HistoricalImportFiltersUiState(),
                    fromLabel = "From date",
                    toLabel = "To date",
                    incomingLabel = "Incoming calls",
                    outgoingLabel = "Outgoing calls",
                    missedLabel = "Missed calls",
                    onStateChange = {},
                )
            }
        }

        compose.onNodeWithContentDescription("From date").assertIsDisplayed()
        compose.onNodeWithContentDescription("To date").assertIsDisplayed()
        val outgoing = compose.onNodeWithTag("import_outgoing")
        outgoing.assertIsDisplayed().assertIsOn()
        val semantics = outgoing.fetchSemanticsNode()
        assertEquals(Role.Checkbox, semantics.config[SemanticsProperties.Role])
        assertEquals(
            listOf("Outgoing calls"),
            semantics.config[SemanticsProperties.ContentDescription],
        )
        assertTrue(semantics.boundsInRoot.height >= 48f)
    }

    private fun assertTrueFilters(state: HistoricalImportFiltersUiState) {
        assertEquals(true, state.incoming)
        assertEquals(true, state.outgoing)
        assertEquals(true, state.missed)
    }
}
