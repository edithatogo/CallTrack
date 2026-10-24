package org.calltrack.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Android-only host for the synthetic shared screen; no platform data is read or written. */
class HostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var state by remember { mutableStateOf(CompanionUiState()) }
            MaterialTheme {
                CompanionScreen(state) { action ->
                    state = reduceCompanionUi(state, action)
                }
            }
        }
    }
}
