package com.fdv.fdvflightlogger.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fdv.fdvflightlogger.data.prefs.ThemeMode
import com.fdv.fdvflightlogger.ui.theme.FDVFlightLoggerTheme
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val START_ANGLE = 135f // 0° is 3 o'clock and angles grow clockwise, so 135° is 7:30
private const val SWEEP_ANGLE = 270f // leaves a 90° gap at the bottom for the caption
private const val TICK_COUNT = 10

/**
 * Cockpit-style arc gauge. [progress] is 0–1; the arc sweeps up to it when first shown
 * and animates to any new value after that.
 */
@Composable
fun ArcGauge(
    progress: Float,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    size: Dp = 150.dp,
    strokeWidth: Dp = 12.dp
) {
    val target = progress.coerceIn(0f, 1f)

    // Start empty so the arc sweeps in, except in Android Studio previews, which show one still frame
    val inPreview = LocalInspectionMode.current
    val animated = remember { Animatable(if (inPreview) target else 0f) }
    LaunchedEffect(target) {
        animated.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = 1_200, easing = FastOutSlowInEasing)
        )
    }

    val cs = MaterialTheme.colorScheme
    val trackColor = cs.onSurface.copy(alpha = 0.10f)
    val tickColor = cs.onSurface.copy(alpha = 0.25f)

    // Stops are fractions of a full circle, measured from START_ANGLE (the canvas is rotated below).
    // The gauge covers 0–0.75; returning to primary at 1.0 hides the seam behind the round start cap.
    val gradient = Brush.sweepGradient(
        0f to cs.primary,
        0.75f to cs.secondary,
        0.875f to cs.secondary,
        1f to cs.primary
    )

    val percent = (target * 100).roundToInt()

    Box(
        modifier = modifier
            .size(size)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label: $value, $percent percent of target"
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            // Inset by the widest thing drawn (the outer glow) so nothing gets clipped
            val inset = stroke * 1.2f
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val radius = arcSize.width / 2f
            val sweep = SWEEP_ANGLE * animated.value

            rotate(degrees = START_ANGLE) {
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = SWEEP_ANGLE,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )

                if (sweep > 0f) {
                    // Glow: wider, fainter copies underneath read as light bleeding off the arc
                    listOf(2.4f to 0.12f, 1.6f to 0.22f, 1f to 1f).forEach { (widthScale, alpha) ->
                        drawArc(
                            brush = gradient,
                            startAngle = 0f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            alpha = alpha,
                            style = Stroke(width = stroke * widthScale, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // Ticks inside the arc, like an instrument dial; 0, 50% and 100% are longer
            for (i in 0..TICK_COUNT) {
                val angle = Math.toRadians((START_ANGLE + SWEEP_ANGLE * i / TICK_COUNT).toDouble())
                val outer = radius - stroke * 1.1f
                val inner = outer - if (i % 5 == 0) stroke * 0.9f else stroke * 0.5f
                drawLine(
                    color = tickColor,
                    start = center + polar(angle, inner),
                    end = center + polar(angle, outer),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // A bright dot at the needle tip
            if (sweep > 0f) {
                val tipAngle = Math.toRadians((START_ANGLE + sweep).toDouble())
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = stroke * 0.28f,
                    center = center + polar(tipAngle, radius)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, style = MaterialTheme.typography.headlineMedium, maxLines = 1)
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                maxLines = 1
            )
        }

        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
            )
        }
    }
}

// Converts an angle and distance from the center into an x/y offset
private fun polar(angleRadians: Double, radius: Float) = Offset(
    x = (cos(angleRadians) * radius).toFloat(),
    y = (sin(angleRadians) * radius).toFloat()
)

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ArcGaugePreview() {
    FDVFlightLoggerTheme(themeMode = ThemeMode.DARK, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ArcGauge(progress = 0.68f, value = "34", label = "flights", caption = "of 50")
                ArcGauge(progress = 0.552f, value = "55.2", label = "block hrs", caption = "of 100")
            }
        }
    }
}