package com.fdv.fdvflightlogger.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fdv.fdvflightlogger.data.auth.FdvSession
import com.fdv.fdvflightlogger.data.fdv.FdvPilot
import com.fdv.fdvflightlogger.data.prefs.FdvImportMode
import com.fdv.fdvflightlogger.data.prefs.FdvSyncState

/**
 * FDV sign-in state and logbook sync for Settings. Stateless: it shows what it's given and
 * reports taps, so it can be previewed and reused without a ViewModel.
 */
@Composable
fun FdvAccountCard(
    session: FdvSession?,
    pilot: FdvPilot?,
    refreshing: Boolean,
    syncState: FdvSyncState,
    historyProgress: String?,
    onSignIn: () -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    onImportHistory: () -> Unit,
    onSyncNew: () -> Unit,
    onRemoveImported: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("FlyDeltaVirtual account", style = MaterialTheme.typography.titleMedium)

            if (session == null) {
                Text(
                    "Sign in to fill your Pilot ID, name and hub from your FDV profile.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign in with FDV")
                }
                return@Column
            }

            val callsign = pilot?.callsign ?: session.callsign ?: "Signed in"
            val name = pilot?.name?.takeIf { it.isNotBlank() }
            Text(
                listOfNotNull(name, callsign).joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge
            )

            if (pilot == null) {
                Text("Loading your profile…", style = MaterialTheme.typography.bodyMedium)
            } else {
                pilot.rank?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                pilot.hubIcao?.let { icao ->
                    Text(
                        "Hub: " + listOfNotNull(icao, pilot.hubName).joinToString(" — "),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                val stats = listOfNotNull(
                    pilot.hours?.let { "%,.1f hrs".format(it) },
                    pilot.flights?.let { "$it flights" },
                    pilot.streakCurrent?.let { current ->
                        "streak $current" + (pilot.streakBest?.let { " (best $it)" } ?: "")
                    }
                )
                if (stats.isNotEmpty()) {
                    Text(stats.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (refreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
                TextButton(onClick = onRefresh, enabled = !refreshing) { Text("Refresh") }
                OutlinedButton(onClick = onSignOut) { Text("Sign out") }
            }

            HorizontalDivider()
            Text("Logbook sync", style = MaterialTheme.typography.titleSmall)

            when {
                historyProgress != null -> Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(historyProgress, style = MaterialTheme.typography.bodyMedium)
                }

                syncState.mode == null -> {
                    Text(
                        "Bring your filed PIREPs into this logbook. You'll see what will change before anything is saved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onImportHistory, modifier = Modifier.fillMaxWidth()) {
                        Text("Import FDV history")
                    }
                }

                else -> {
                    Text(
                        when (syncState.mode) {
                            FdvImportMode.ALL -> "Every FDV flight is added to this logbook."
                            FdvImportMode.MATCH_ONLY -> "Flights you log are filled in from FDV."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        if (syncState.mode == FdvImportMode.ALL) {
                            TextButton(onClick = onRemoveImported) { Text("Remove imported") }
                        }
                        OutlinedButton(onClick = onSyncNew) { Text("Sync new flights") }
                    }
                }
            }
        }
    }
}