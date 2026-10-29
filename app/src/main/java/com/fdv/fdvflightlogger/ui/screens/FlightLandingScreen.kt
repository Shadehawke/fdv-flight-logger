package com.fdv.fdvflightlogger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fdv.fdvflightlogger.data.Airlines
import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import com.fdv.fdvflightlogger.data.stats.FlightStats
import com.fdv.fdvflightlogger.data.stats.RankedItem
import com.fdv.fdvflightlogger.ui.AppViewModel
import com.fdv.fdvflightlogger.ui.components.ArcGauge
import com.fdv.fdvflightlogger.ui.nav.Routes
import com.fdv.fdvflightlogger.ui.theme.DeltaBlue
import com.fdv.fdvflightlogger.ui.theme.GradientButton
import com.fdv.fdvflightlogger.ui.theme.raised
import com.fdv.fdvflightlogger.ui.theme.raisedColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val DashboardDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightLandingScreen(
    appViewModel: AppViewModel,
    navController: NavController
) {
    val state by appViewModel.state.collectAsStateWithLifecycle()
    val stats by appViewModel.stats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FDV Flight Logger") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeltaBlue,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { navController.navigate(Routes.HISTORY) }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Flight History")
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Breakpoints come from the space this screen actually gets, not the device type
            val wide = maxWidth >= 600.dp
            val gaugeSize = if (maxWidth < 420.dp) 128.dp else 150.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Greeting(
                    name = state.profile.name,
                    hub = state.profile.hub,
                    lastLanded = state.lastLanded
                )

                DashboardActions(
                    onNewFlight = { navController.navigate(Routes.FLIGHT_LOG) },
                    onOpenSaved = { navController.navigate(Routes.HISTORY) }
                )

                GaugeGrid(stats = stats, hub = state.profile.hub, gaugeSize = gaugeSize)

                if (stats.totalFlights == 0) {
                    Text(
                        text = "Log your first flight to start filling these gauges.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    SummaryRow(stats)

                    if (wide) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            RankedCard("Top aircraft", stats.topAircraft, Modifier.weight(1f).fillMaxHeight())
                            RankedCard("Top routes", stats.topRoutes, Modifier.weight(1f).fillMaxHeight())
                        }
                    } else {
                        RankedCard("Top aircraft", stats.topAircraft, Modifier.fillMaxWidth())
                        RankedCard("Top routes", stats.topRoutes, Modifier.fillMaxWidth())
                    }

                    stats.lastFlight?.let { flight ->
                        LastFlightCard(
                            flight = flight,
                            onClick = { navController.navigate("detail/${flight.id}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Greeting(name: String, hub: String, lastLanded: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = if (name.isBlank()) "Welcome back" else "Welcome back, $name",
            style = MaterialTheme.typography.headlineSmall
        )

        val details = listOfNotNull(
            hub.takeIf { it.isNotBlank() }?.let { "Hub $it" },
            lastLanded.takeIf { it.isNotBlank() }?.let { "Last landed $it" }
        ).joinToString(" • ")

        if (details.isNotEmpty()) {
            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DashboardActions(onNewFlight: () -> Unit, onOpenSaved: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GradientButton(
            text = "Start new flight",
            enabled = true,
            onClick = onNewFlight,
            modifier = Modifier.weight(1f)
        )
        RaisedButton(
            text = "Open saved flight",
            onClick = onOpenSaved,
            modifier = Modifier.weight(1f)
        )
    }
}

// Secondary action: same pill shape as GradientButton, but raised instead of filled
@Composable
private fun RaisedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .raised(raisedColors(), cornerRadius = 50.dp, elevation = 3.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun GaugeGrid(stats: FlightStats, hub: String, gaugeSize: Dp) {
    val total = stats.totalFlights
    val onlinePct = (stats.onlineShare * 100).roundToInt()
    val hubPct = (stats.hubShare * 100).roundToInt()

    // FlowRow wraps onto new lines when a row runs out of room: 4 across on a tablet, 2×2 on a phone
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GaugeCard {
            ArcGauge(
                progress = stats.flightsMilestone.progress,
                value = "%,d".format(total),
                label = "flights",
                caption = "of %,d".format(stats.flightsMilestone.target),
                size = gaugeSize
            )
        }
        GaugeCard {
            ArcGauge(
                progress = stats.hoursMilestone.progress,
                value = "%.1f".format(stats.blockHours),
                label = "block hrs",
                caption = "of %,d".format(stats.hoursMilestone.target),
                size = gaugeSize
            )
        }
        GaugeCard {
            ArcGauge(
                progress = stats.onlineShare,
                value = "$onlinePct%",
                label = "online",
                caption = "${stats.onlineFlights} of $total",
                size = gaugeSize
            )
        }
        GaugeCard {
            ArcGauge(
                progress = stats.hubShare,
                value = "$hubPct%",
                label = if (hub.isBlank()) "hub" else "via $hub",
                caption = "${stats.hubFlights} of $total",
                size = gaugeSize
            )
        }
    }
}

@Composable
private fun GaugeCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .raised(raisedColors())
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun SummaryRow(stats: FlightStats) {
    val longest = stats.longestFlight

    // IntrinsicSize.Min makes every tile as tall as the tallest one, even though only "Longest" has a detail line
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StatTile(
            label = "Distance",
            value = "%,d nm".format(stats.distanceNm.roundToInt()),
            modifier = Modifier.weight(1f)
        )
        StatTile(
            label = "Airports",
            value = "%,d".format(stats.airportsVisited),
            modifier = Modifier.weight(1f)
        )
        StatTile(
            label = "Longest",
            value = if (longest == null) "—" else "${longest.dep}–${longest.arr}",
            detail = if (longest == null) null else "%,d nm".format(stats.longestFlightNm.roundToInt()),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    detail: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .raised(raisedColors())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RankedCard(title: String, items: List<RankedItem>, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val barBrush = Brush.horizontalGradient(listOf(cs.primary, cs.secondary))
    val max = items.maxOfOrNull { it.count } ?: 1

    Column(
        modifier = modifier
            .raised(raisedColors())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)

        items.forEach { item ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${item.count}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )
                }
                // Bars are relative to the top entry, so the leader always fills the row
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.count.toFloat() / max)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(barBrush)
                )
            }
        }
    }
}

@Composable
private fun LastFlightCard(flight: FlightLogEntity, onClick: () -> Unit) {
    val date = Instant.ofEpochMilli(flight.createdAtEpochMs)
        .atZone(ZoneId.systemDefault())
        .format(DashboardDate)
    val info = Airlines.formatFlightInfo(flight.airline, flight.flightNumber, flight.aircraft)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .raised(raisedColors())
            // Same radius as raised()'s default, so the ripple matches the card's corners
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Last flight",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = "${flight.dep} → ${flight.arr}", style = MaterialTheme.typography.titleLarge)
            Text(
                text = listOf(date, info).filter { it.isNotEmpty() }.joinToString(" • "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Open last flight",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}