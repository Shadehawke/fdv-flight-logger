package com.fdv.fdvflightlogger.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdv.fdvflightlogger.ui.AppViewModel

/** This version's release notes, once after an update. Shows nothing otherwise. */
@Composable
fun WhatsNewDialog(appViewModel: AppViewModel) {
    val whatsNew by appViewModel.whatsNew.collectAsStateWithLifecycle()
    val notes = whatsNew ?: return

    AlertDialog(
        onDismissRequest = appViewModel::dismissWhatsNew,
        title = { Text("What's new in ${notes.version}") },
        text = {
            // Scrolls when the notes are longer than the screen, e.g. on the Fold's cover screen
            HelpBody(
                markdown = notes.body,
                terms = emptyList(),
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            TextButton(onClick = appViewModel::dismissWhatsNew) { Text("Got it") }
        }
    )
}