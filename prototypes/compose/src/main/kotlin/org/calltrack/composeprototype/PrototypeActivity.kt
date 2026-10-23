package org.calltrack.composeprototype

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/** Instrumentation-only host. All callbacks change in-memory synthetic state. */
class PrototypeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var access by rememberSaveable { mutableStateOf(false) }
            var selected by rememberSaveable { mutableLongStateOf(-1L) }
            var requests by rememberSaveable { mutableIntStateOf(0) }
            val state =
                PreviewState(calendarAccess = access, selected = selected, requests = requests)
            MaterialTheme {
                PreviewScreen(
                    state,
                    onAccess = { access = true },
                    onSelect = { if (state.controlsEnabled) selected = it },
                    onLog = { if (state.canLog) requests++ },
                )
            }
        }
    }
}
