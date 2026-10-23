package org.calltrack.companion

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

/** Desktop host only; the sample screen is in commonMain. */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "CallTrack companion experiment") {
        var state by remember { mutableStateOf(CompanionUiState()) }
        MaterialTheme {
            CompanionScreen(state) { action -> state = reduceCompanionUi(state, action) }
        }
    }
}
