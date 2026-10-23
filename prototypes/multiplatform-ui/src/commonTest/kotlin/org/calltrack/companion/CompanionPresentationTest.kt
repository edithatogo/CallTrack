package org.calltrack.companion

import kotlin.test.Test
import kotlin.test.assertEquals

class CompanionPresentationTest {
    @Test
    fun previewRequiresSelectedDestination() {
        assertEquals(
            CompanionUiState(),
            reduceCompanionUi(CompanionUiState(), CompanionUiAction.REQUEST_PREVIEW),
        )
    }

    @Test
    fun selectionEnablesPreviewAndClearingResetsIt() {
        val selected =
            reduceCompanionUi(CompanionUiState(), CompanionUiAction.SELECT_SAMPLE_DESTINATION)
        val preview = reduceCompanionUi(selected, CompanionUiAction.REQUEST_PREVIEW)
        assertEquals(CompanionUiState(destinationSelected = true, previewVisible = true), preview)
        assertEquals(
            CompanionUiState(),
            reduceCompanionUi(preview, CompanionUiAction.CLEAR_DESTINATION),
        )
    }

    @Test
    fun selectingAgainNeverRestoresPreviousPreview() {
        val selected = CompanionUiState(destinationSelected = true, previewVisible = true)
        assertEquals(
            CompanionUiState(destinationSelected = true, previewVisible = false),
            reduceCompanionUi(selected, CompanionUiAction.SELECT_SAMPLE_DESTINATION),
        )
    }
}
