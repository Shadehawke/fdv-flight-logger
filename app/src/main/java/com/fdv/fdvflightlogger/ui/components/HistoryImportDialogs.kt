package com.fdv.fdvflightlogger.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdv.fdvflightlogger.data.prefs.FdvImportMode
import com.fdv.fdvflightlogger.data.sync.ImportPlan
import com.fdv.fdvflightlogger.ui.AppViewModel
import com.fdv.fdvflightlogger.ui.ImportOffer

/** What a history import would do, and the pilot's choice. Used by Settings and the sign-in offer. */
@Composable
fun ImportPlanDialog(
    plan: ImportPlan,
    onDismiss: () -> Unit,
    onImport: (FdvImportMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import your FDV history?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${plan.total} PIREPs found on FDV.")
                Text("${plan.matched.size} match flights in your logbook and will get their actuals.")
                if (plan.alreadyLinked > 0) Text("${plan.alreadyLinked} are already synced.")
                Text("${plan.unmatched.size} have no matching flight.")
                Text(
                    "Import all adds those as new flights. Only my flights leaves them out. " +
                            "Either way, new PIREPs follow the same choice when you sync.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onImport(FdvImportMode.ALL) }) { Text("Import all") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = { onImport(FdvImportMode.MATCH_ONLY) }) { Text("Only my flights") }
            }
        }
    )
}

/**
 * The import offered after signing in to a logbook that has never imported FDV history.
 * Asks first, shows progress while the logbook is read, then hands over to [ImportPlanDialog].
 * Shows nothing when there's no offer, so screens can include it unconditionally.
 */
@Composable
fun HistoryImportOffer(appViewModel: AppViewModel) {
    val offer by appViewModel.importOffer.collectAsStateWithLifecycle()
    val progress by appViewModel.historyProgress.collectAsStateWithLifecycle()

    when (val current = offer) {
        null -> Unit

        ImportOffer.Asking -> AlertDialog(
            onDismissRequest = appViewModel::dismissImportOffer,
            title = { Text("Bring in your FDV flights?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "The app can read your FDV logbook, fill in block time, fuel, landing rate and " +
                                "passengers on flights you've logged, and add the rest as new flights."
                    )
                    Text(
                        "You'll see what it found before anything changes. You can also do this later " +
                                "in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = appViewModel::acceptImportOffer) { Text("Take a look") }
            },
            dismissButton = {
                TextButton(onClick = appViewModel::dismissImportOffer) { Text("Not now") }
            }
        )

        ImportOffer.Reading -> AlertDialog(
            // A tap outside shouldn't drop the read; Hide is the deliberate way out
            onDismissRequest = {},
            title = { Text("Reading your FDV logbook") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                    Text(progress ?: "Starting…")
                }
            },
            confirmButton = {
                TextButton(onClick = appViewModel::dismissImportOffer) { Text("Hide") }
            }
        )

        is ImportOffer.Ready -> ImportPlanDialog(
            plan = current.plan,
            onDismiss = appViewModel::dismissImportOffer,
            onImport = { mode -> appViewModel.applyImportOffer(current.plan, mode) }
        )
    }
}