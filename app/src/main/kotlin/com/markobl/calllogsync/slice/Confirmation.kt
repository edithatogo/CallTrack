// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

internal data class Confirmation(
    val title: String,
    val message: String,
    val actionLabel: String,
    val onConfirm: () -> Unit,
)

@Composable
internal fun ConfirmationDialog(confirmation: Confirmation, onDismiss: () -> Unit) {
    ConfirmationDialog(
        title = confirmation.title,
        message = confirmation.message,
        actionLabel = confirmation.actionLabel,
        onConfirm = {
            onDismiss()
            confirmation.onConfirm()
        },
        onDismiss = onDismiss,
    )
}

@Composable
internal fun ConfirmationDialog(
    title: String,
    message: String,
    actionLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Text(
                message,
                Modifier.testTag("calltrack_dialog_message")
                    .semantics { testTagsAsResourceId = true }
                    .verticalScroll(rememberScrollState()),
            )
        },
        dismissButton = {
            ActionButton(
                label = stringResource(android.R.string.cancel),
                tag = "calltrack_dialog_cancel",
                enabled = true,
                onClick = onDismiss,
                style = ButtonStyle.Text,
            )
        },
        confirmButton = {
            ActionButton(
                label = actionLabel,
                tag = "calltrack_dialog_confirm",
                enabled = true,
                onClick = onConfirm,
                style = ButtonStyle.Text,
            )
        },
    )
}
