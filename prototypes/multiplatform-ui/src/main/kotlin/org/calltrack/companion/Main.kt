package org.calltrack.companion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

/** Sample UI only: no provider, import parser, ledger, files or credentials. */
@Composable
fun CompanionScreen() {
    var destinationSelected by remember { mutableStateOf(false) }
    var previewVisible by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("CallTrack companion experiment", style = MaterialTheme.typography.h5)
        Text("Sample data only. No calendar connection or file import.")
        Text("Automatic cellular capture: unavailable in this experiment.")
        Text(if (destinationSelected) "Destination: sample calendar" else "Choose a destination")
        Button(
            onClick = {
                destinationSelected = !destinationSelected
                // A preview belongs to its destination; never carry it into another selection.
                previewVisible = false
            }
        ) {
            Text(if (destinationSelected) "Clear selection" else "Use sample calendar")
        }
        Button(onClick = { previewVisible = true }, enabled = destinationSelected) {
            Text("Preview sample")
        }
        if (previewVisible) {
            Text("2 synthetic entries ready for review; nothing saved.")
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "CallTrack companion experiment") {
        MaterialTheme { CompanionScreen() }
    }
}
