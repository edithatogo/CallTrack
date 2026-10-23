package com.markobl.calllogsync.slice

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HistoricalImportDestinationPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun destinationListIsPresentedAndOnlyExplicitSelectionReturnsOpaqueToken() {
        val selected = mutableListOf<String>()
        val options =
            listOf(
                HistoricalImportDestinationOption("opaque-1", "Personal calendar"),
                HistoricalImportDestinationOption("opaque-2", "Work calendar"),
            )

        compose.setContent {
            HistoricalImportDestinationPanel(
                state = HistoricalImportDestinationUiState(options, "opaque-1", enabled = true),
                chooseLabel = "Choose a destination calendar",
                unavailableLabel = "Destination unavailable",
                onSelect = selected::add,
            )
        }

        compose.onNodeWithTag("import_calendar_picker").assertIsDisplayed()
        compose
            .onNodeWithTag("import_calendar_picker")
            .assertContentDescriptionEquals("Personal calendar")
        compose.onNodeWithText("Personal calendar").assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<String>(), selected) }

        compose.onNodeWithTag("import_calendar_picker").performClick()
        compose.onNodeWithTag("import_calendar_option_1").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(listOf("opaque-2"), selected) }
    }

    @Test
    fun unavailableDestinationCannotBeOpenedOrSelected() {
        val selected = mutableListOf<String>()

        compose.setContent {
            HistoricalImportDestinationPanel(
                state = HistoricalImportDestinationUiState(),
                chooseLabel = "Choose a destination calendar",
                unavailableLabel = "Destination unavailable",
                onSelect = selected::add,
            )
        }

        compose.onNodeWithTag("import_calendar_picker").assertIsNotEnabled()
        compose.onNodeWithTag("import_calendar_unavailable").assertIsDisplayed()
        compose.onNodeWithText("Destination unavailable").assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<String>(), selected) }
    }
}
