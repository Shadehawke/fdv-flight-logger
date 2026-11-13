package com.fdv.fdvflightlogger.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.fdv.fdvflightlogger.data.fdv.FdvPilot

/** Progress from the current rank to the next, with hours to go. Shows "Top rank reached" at the top. */
@Composable
fun RankProgress(pilot: FdvPilot, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val next = pilot.nextRank

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (next == null) {
            // No next rank and a known current one means the pilot is at the top of the ladder
            if (pilot.rank != null) {
                Text("Top rank reached", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            return@Column
        }

        // Sweeps in from empty, like the dashboard gauges
        val target = pilot.rankProgress ?: 0f
        val animated = remember { Animatable(0f) }
        LaunchedEffect(target) {
            animated.animateTo(target, tween(durationMillis = 900, easing = FastOutSlowInEasing))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(cs.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated.value)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(cs.primary, cs.secondary)))
            )
        }

        Text(
            text = pilot.nextRankHoursRemaining
                ?.let { "%,.1f hrs to %s".format(it, next) }
                ?: "Next rank: $next",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant
        )
    }
}