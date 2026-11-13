package com.fdv.fdvflightlogger.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fdv.fdvflightlogger.data.auth.FdvSession
import com.fdv.fdvflightlogger.data.fdv.FdvPilot
import com.fdv.fdvflightlogger.data.prefs.FdvImportMode
import com.fdv.fdvflightlogger.data.prefs.FdvSyncState
import com.fdv.fdvflightlogger.ui.theme.GradientButton
import com.fdv.fdvflightlogger.ui.theme.inset
import com.fdv.fdvflightlogger.ui.theme.raised
import com.fdv.fdvflightlogger.ui.theme.raisedColors

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
    airTimeFill: String?,
    avatar: Bitmap?,
    onSignIn: () -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    onImportHistory: () -> Unit,
    onSyncNew: () -> Unit,
    onRemoveImported: () -> Unit,
    onFillAirTimes: () -> Unit,
    onCancelAirTimes: () -> Unit,
    onChoosePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .raised(raisedColors())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (session == null) {
            SignedOutContent(onSignIn)
            return@Column
        }

        // ---- Header: avatar, name, callsign and hub, refresh ----
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EditableAvatar(
                photo = avatar,
                name = pilot?.name,
                callsign = pilot?.callsign ?: session.callsign,
                onChoosePhoto = onChoosePhoto,
                onRemovePhoto = onRemovePhoto
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = pilot?.name?.takeIf { it.isNotBlank() } ?: "FlyDeltaVirtual pilot",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val hub = pilot?.let { p -> p.hubIcao?.let { icao -> listOfNotNull(icao, p.hubName).joinToString(" — ") } }
                val subtitle = listOfNotNull(pilot?.callsign ?: session.callsign, hub).joinToString(" · ")
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (refreshing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh FDV profile", tint = cs.onSurfaceVariant)
                }
            }
        }

        // ---- Rank and career numbers ----
        if (pilot == null) {
            Text("Loading your profile…", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        } else {
            pilot.rank?.let { RankChip(it) }
            RankProgress(pilot = pilot, modifier = Modifier.fillMaxWidth())

            // IntrinsicSize.Min makes all three wells as tall as the tallest (only Streak has a second line)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatWell("FDV Hours", pilot.hours?.let { "%,.1f".format(it) } ?: "—", Modifier.weight(1f))
                StatWell("Flights", pilot.flights?.let { "%,d".format(it) } ?: "—", Modifier.weight(1f))
                StatWell(
                    label = "Flight Streak",
                    value = pilot.streakCurrent?.let { "%,d".format(it) } ?: "—",
                    modifier = Modifier.weight(1f),
                    detail = pilot.streakBest?.let { "best %,d".format(it) },
                    icon = Icons.Filled.LocalFireDepartment
                )
            }
            hoursBreakdown(pilot)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
        }

        HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.5f))

        // ---- Logbook sync ----
        LogbookSyncSection(
            syncState = syncState,
            historyProgress = historyProgress,
            airTimeFill = airTimeFill,
            onImportHistory = onImportHistory,
            onSyncNew = onSyncNew,
            onRemoveImported = onRemoveImported,
            onFillAirTimes = onFillAirTimes,
            onCancelAirTimes = onCancelAirTimes
        )

        // Out of the way of the everyday buttons, and red because it ends the session
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onSignOut) { Text("Sign out", color = cs.error) }
        }
    }
}

/** The avatar with a small camera badge; tapping it offers to choose, change or remove the photo. */
@Composable
private fun EditableAvatar(
    photo: Bitmap?,
    name: String?,
    callsign: String?,
    onChoosePhoto: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    // UI-only state (is the menu open?), so it lives here rather than in the ViewModel
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        PilotAvatar(photo = photo, name = name, callsign = callsign, onClick = { menuOpen = true })

        // The badge says "tap me"; it sits on the ring's edge, outside the clipped circle
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(22.dp)
                .clip(CircleShape)
                .background(cs.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.PhotoCamera,
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier.size(14.dp)
            )
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(if (photo == null) "Choose photo" else "Change photo") },
                onClick = {
                    menuOpen = false
                    onChoosePhoto()
                }
            )
            if (photo != null) {
                DropdownMenuItem(
                    text = { Text("Remove photo") },
                    onClick = {
                        menuOpen = false
                        onRemovePhoto()
                    }
                )
            }
        }
    }
}

@Composable
private fun SignedOutContent(onSignIn: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GlowDisc {
            Icon(Icons.Filled.Flight, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("FlyDeltaVirtual account", style = MaterialTheme.typography.titleLarge)
            Text(
                "Sign in to fill your Pilot ID, name and hub, start flights from your bookings and sync your PIREPs.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    GradientButton(text = "Sign in with FDV", enabled = true, onClick = onSignIn, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun LogbookSyncSection(
    syncState: FdvSyncState,
    historyProgress: String?,
    airTimeFill: String?,
    onImportHistory: () -> Unit,
    onSyncNew: () -> Unit,
    onRemoveImported: () -> Unit,
    onFillAirTimes: () -> Unit,
    onCancelAirTimes: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Logbook sync", style = MaterialTheme.typography.titleMedium)

        when {
            historyProgress != null -> ProgressLine(historyProgress)

            syncState.mode == null -> {
                Text(
                    "Bring your filed PIREPs into this logbook. You'll see what will change before anything is saved.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )
                PillButton("Import FDV history", onClick = onImportHistory, modifier = Modifier.fillMaxWidth())
            }

            else -> {
                Text(
                    when (syncState.mode) {
                        FdvImportMode.ALL -> "Every FDV flight is added to this logbook."
                        FdvImportMode.MATCH_ONLY -> "Flights you log are filled in from FDV."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PillButton("Sync new flights", onClick = onSyncNew, modifier = Modifier.weight(1f))
                    if (syncState.mode == FdvImportMode.ALL) {
                        TextButton(onClick = onRemoveImported) { Text("Remove imported") }
                    }
                }

                if (airTimeFill != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) { ProgressLine(airTimeFill) }
                        TextButton(onClick = onCancelAirTimes) { Text("Stop") }
                    }
                } else {
                    TextButton(onClick = onFillAirTimes) { Text("Fill in missing air times") }
                }
            }
        }
    }
}

@Composable
private fun ProgressLine(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** A sunken well holding one number, matching the flight log's inset fields. */
@Composable
private fun StatWell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    icon: ImageVector? = null
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxHeight()
            .inset(raisedColors())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = cs.primary, modifier = Modifier.size(20.dp))
            }
            Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        }
        detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant) }
    }
}

/** The pilot's rank in a pill with a brand-gradient outline. */
@Composable
private fun RankChip(rank: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .border(1.dp, Brush.horizontalGradient(listOf(cs.primary, cs.secondary)), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Filled.MilitaryTech, contentDescription = null, tint = cs.secondary, modifier = Modifier.size(18.dp))
        Text(rank, style = MaterialTheme.typography.labelLarge)
    }
}

/** Secondary action: a small raised pill, the same style as the dashboard's "Open saved flight". */
@Composable
private fun PillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .raised(raisedColors(), cornerRadius = 50.dp, elevation = 3.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

// "2,879.6 flown + 116.4 bonus + 111.0 transfer"; parts FDV didn't send are left out
private fun hoursBreakdown(pilot: FdvPilot): String? {
    val parts = listOfNotNull(
        pilot.hoursFlown?.let { "%,.1f flown".format(it) },
        pilot.hoursBonus?.takeIf { it > 0 }?.let { "%,.1f bonus".format(it) },
        pilot.hoursTransfer?.takeIf { it > 0 }?.let { "%,.1f transfer".format(it) }
    )
    return if (parts.isEmpty()) null else parts.joinToString(" + ")
}
