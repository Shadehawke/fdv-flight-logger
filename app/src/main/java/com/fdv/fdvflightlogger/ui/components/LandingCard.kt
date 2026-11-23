package com.fdv.fdvflightlogger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import com.fdv.fdvflightlogger.data.stats.LandingRating
import com.fdv.fdvflightlogger.data.stats.LandingStats
import com.fdv.fdvflightlogger.data.stats.LandingStatsCalculator
import com.fdv.fdvflightlogger.ui.theme.raised
import com.fdv.fdvflightlogger.ui.theme.raisedColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val BestDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

// Within this many fpm of the average, the last 10 count as "about your average"
private const val TREND_TOLERANCE = 5

/** A colour per rating, so it reads at a glance in any theme. Floated is cool and muted: soft, but not the goal. */
fun landingColor(rating: LandingRating): Color = when (rating) {
    LandingRating.FLOATED -> Color(0xFF8FA3B8)
    LandingRating.BUTTER -> Color(0xFFF2B632)
    LandingRating.SMOOTH -> Color(0xFF3FA66B)
    LandingRating.FIRM -> Color(0xFF4F8DDB)
    LandingRating.HARD -> Color(0xFFEA8A1A)
    LandingRating.VERY_HARD -> Color(0xFFE5484D)
}

/** A small pill: "Floated", "Butter", "Smooth", "Firm", "Hard", "Very hard". */
@Composable
fun LandingRatingBadge(rating: LandingRating, modifier: Modifier = Modifier) {
    val color = landingColor(rating)
    Text(
        text = rating.label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/** The dashboard's Landings card. Shows nothing until a flight has a landing rate. */
@Composable
fun LandingCard(stats: LandingStats, onOpenFlight: (Long) -> Unit, modifier: Modifier = Modifier) {
    val average = stats.averageFpm ?: return
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .raised(raisedColors())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Landings", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                text = if (stats.landings == 1) "1 landing" else "%,d landings".format(stats.landings),
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }

        // The headline: average touchdown and its rating
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "$average fpm",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
                LandingRating.of(average)?.let { LandingRatingBadge(it) }
            }
            Text("average touchdown", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }

        // Only worth saying once there are more landings than the last 10
        val recent = stats.recentAverageFpm
        if (recent != null && stats.landings > LandingStatsCalculator.RECENT) {
            val diff = recent - average
            Text(
                text = when {
                    diff < -TREND_TOLERANCE -> "Last 10: $recent fpm, softer than your average"
                    diff > TREND_TOLERANCE -> "Last 10: $recent fpm, firmer than your average"
                    else -> "Last 10: $recent fpm, about your average"
                },
                style = MaterialTheme.typography.bodyMedium,
                // Coloured by what the last 10 rate, not by softer/firmer: softer isn't better if it's floating
                color = LandingRating.of(recent)?.let(::landingColor) ?: cs.onSurfaceVariant
            )
        }

        SpreadBar(stats.bands)

        stats.best?.let { best -> BestLanding(best, onClick = { onOpenFlight(best.id) }) }

        if (stats.byAircraft.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Average by aircraft", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                stats.byAircraft.forEach { type ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(type.aircraft, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            text = "${type.averageFpm} fpm",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LandingRating.of(type.averageFpm)?.let(::landingColor) ?: cs.onSurface
                        )
                        Text(
                            text = "  (${type.landings})",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** One bar split by rating, sized by how many landings each has, with a legend underneath. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpreadBar(bands: Map<LandingRating, Int>) {
    val cs = MaterialTheme.colorScheme
    val present = LandingRating.entries.filter { (bands[it] ?: 0) > 0 }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
        ) {
            present.forEach { rating ->
                // weight() shares the width in proportion to the counts, so the bar is the spread
                Box(
                    modifier = Modifier
                        .weight(bands.getValue(rating).toFloat())
                        .fillMaxHeight()
                        .background(landingColor(rating))
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            present.forEach { rating ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(landingColor(rating)))
                    Text(
                        text = "${rating.label} ${bands.getValue(rating)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BestLanding(flight: FlightLogEntity, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val fpm = LandingStatsCalculator.softness(flight.landingRateFpm) ?: return
    val date = Instant.ofEpochMilli(flight.createdAtEpochMs).atZone(ZoneId.systemDefault()).format(BestDate)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = landingColor(LandingRating.BUTTER))
        Column(modifier = Modifier.weight(1f)) {
            Text("Best landing: $fpm fpm", style = MaterialTheme.typography.titleSmall)
            Text(
                text = listOfNotNull("${flight.dep} → ${flight.arr}", flight.aircraft, date).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = "Open flight", tint = cs.onSurfaceVariant)
    }
}