package com.fdv.fdvflightlogger.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.fdv.fdvflightlogger.data.fdv.FdvEvent
import com.fdv.fdvflightlogger.ui.theme.raised
import com.fdv.fdvflightlogger.ui.theme.raisedColors
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val LiveRed = Color(0xFFE5484D)

private val Clock: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val DayClock: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a")
private val Zulu: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmm'Z'").withZone(ZoneOffset.UTC)

// ---------------- Text helpers (plain functions, so they're unit-testable) ----------------

/** "Starts in 25 min", "Starts tomorrow 7:00 PM (0100Z)", or "Ends today 10:00 PM (0400Z)" while live. */
fun eventTiming(event: FdvEvent, now: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    if (event.isLiveAt(now)) return "Ends ${dayAndTime(event.endsAtMillis, now, zone)}"
    val minutes = (event.startsAtMillis - now) / 60_000
    return if (minutes in 0..59) "Starts in ${minutes.coerceAtLeast(1)} min"
    else "Starts ${dayAndTime(event.startsAtMillis, now, zone)}"
}

/** The whole window: "Sat, Nov 21 · 6:00 PM – 10:00 PM (1800Z–2200Z)". */
fun eventWindow(event: FdvEvent, zone: ZoneId = ZoneId.systemDefault()): String {
    val start = Instant.ofEpochMilli(event.startsAtMillis).atZone(zone)
    val end = Instant.ofEpochMilli(event.endsAtMillis).atZone(zone)
    val endText = end.format(if (end.toLocalDate() == start.toLocalDate()) Clock else DayClock)
    return "${start.format(DayClock)} – $endText (${Zulu.format(start)}–${Zulu.format(end)})"
}

/** "KATL → KJFK", "Fly-in to KSEA", "Fly-out from KATL", or null when FDV gave no airports. */
fun eventRoute(event: FdvEvent): String? = when {
    event.departureIcao != null && event.arrivalIcao != null -> "${event.departureIcao} → ${event.arrivalIcao}"
    event.arrivalIcao != null -> "Fly-in to ${event.arrivalIcao}"
    event.departureIcao != null -> "Fly-out from ${event.departureIcao}"
    else -> null
}

private fun dayAndTime(millis: Long, now: Long, zone: ZoneId): String {
    val at = Instant.ofEpochMilli(millis).atZone(zone)
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val label = when (ChronoUnit.DAYS.between(today, at.toLocalDate())) {
        0L -> "today ${at.format(Clock)}"
        1L -> "tomorrow ${at.format(Clock)}"
        else -> at.format(DayClock)
    }
    return "$label (${Zulu.format(at)})"
}

// ---------------- Composables ----------------

/** The current time, updated every 30 seconds so countdowns and LIVE badges stay right. */
@Composable
fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    return now
}

/** A red LIVE badge with a pulsing dot. [onImage] darkens it so it reads over a banner. */
@Composable
fun LivePill(modifier: Modifier = Modifier, onImage: Boolean = false) {
    val pulse = rememberInfiniteTransition(label = "live")
    val dotAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "liveDot"
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (onImage) Color.Black.copy(alpha = 0.6f) else LiveRed.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                // Reading the animated value here, in the draw layer, repaints only the dot
                .graphicsLayer { alpha = dotAlpha }
                .clip(CircleShape)
                .background(LiveRed)
        )
        Text("LIVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LiveRed)
    }
}

/** The event's banner, over a brand gradient that shows while it loads or when there's none. */
@Composable
fun EventBanner(imageUrl: String?, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier.background(Brush.linearGradient(listOf(cs.primary, cs.secondary))),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Event, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(32.dp))
        // Banners are under FDV's /public/ path, so no token is needed
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

/** Dashboard card: what's live now, or the next event. Shows nothing when there's neither. */
@Composable
fun DashboardEventsCard(events: List<FdvEvent>, onOpenAll: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val now = rememberNow()
    val live = events.filter { it.isLiveAt(now) }
    val shown = live.take(2).ifEmpty { listOfNotNull(events.firstOrNull { it.startsAtMillis > now }) }
    if (shown.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .raised(raisedColors())
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenAll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (live.isNotEmpty()) "Happening now" else "Next event",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text("All events  ›", style = MaterialTheme.typography.labelLarge, color = cs.secondary)
        }
        shown.forEach { event -> EventSummaryRow(event, now) }
    }
}

@Composable
private fun EventSummaryRow(event: FdvEvent, now: Long) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        EventBanner(
            imageUrl = event.imageUrl,
            modifier = Modifier
                .size(width = 96.dp, height = 60.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (event.isLiveAt(now)) LivePill()
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // fill = false: the name takes only the room it needs, but shrinks before pushing the badge off
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Text(
                text = listOfNotNull(event.category, eventRoute(event)).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(eventTiming(event, now), style = MaterialTheme.typography.bodySmall)
        }
        if (event.registered) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "You're registered", tint = cs.primary)
        }
    }
}