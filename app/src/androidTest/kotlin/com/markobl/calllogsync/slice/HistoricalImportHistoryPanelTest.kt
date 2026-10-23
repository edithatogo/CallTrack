package com.markobl.calllogsync.slice

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HistoricalImportHistoryPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun emptyHistoryShowsLocalizedEmptyStateWithoutOpeningASession() {
        val opened = mutableListOf<String>()

        compose.setContent {
            HistoricalImportHistoryPanel(
                state = HistoricalImportHistoryUiState(),
                title = "Import history",
                emptyMessage = "No previous imports",
                onOpen = { opened.add(it) },
            )
        }

        compose.onNodeWithText("Import history").assertIsDisplayed()
        compose.onNodeWithTag("import_history_empty").assertIsDisplayed()
        compose.onNodeWithText("No previous imports").assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<String>(), opened) }
    }

    @Test
    fun historyRowsRenderInOrderAndOnlyExplicitTapOpensSessionToken() {
        val opened = mutableListOf<String>()
        val state =
            HistoricalImportHistoryUiState(
                items =
                    listOf(
                        HistoricalImportHistoryItem("12", "Import 12 — complete"),
                        HistoricalImportHistoryItem("13", "Import 13 — needs review"),
                    )
            )

        compose.setContent {
            HistoricalImportHistoryPanel(
                state = state,
                title = "Import history",
                emptyMessage = "No previous imports",
                onOpen = { opened.add(it) },
            )
        }

        compose.onNodeWithText("Import history").assertIsDisplayed()
        compose.onNodeWithText("Import 12 — complete").assertIsDisplayed()
        compose.onNodeWithText("Import 13 — needs review").assertIsDisplayed()
        compose.onAllNodesWithText("No previous imports").assertCountEquals(0)
        compose.runOnIdle { assertEquals(emptyList<String>(), opened) }

        compose.onNodeWithTag("import_history_item_1").performClick()
        compose.runOnIdle { assertEquals(listOf("13"), opened) }
    }
}
