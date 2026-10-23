package com.markobl.calllogsync.slice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private fun assertTrueFilters(state: HistoricalImportFiltersUiState) {
        assertEquals(true, state.incoming)
        assertEquals(true, state.outgoing)
        assertEquals(true, state.missed)
    }
}
