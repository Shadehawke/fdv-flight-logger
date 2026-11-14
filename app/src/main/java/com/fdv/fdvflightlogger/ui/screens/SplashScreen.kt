package com.fdv.fdvflightlogger.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fdv.fdvflightlogger.R
import com.fdv.fdvflightlogger.ui.nav.Routes
import kotlinx.coroutines.delay

private val PlaneSize = 44.dp
private val RevealFeather = 56.dp      // how soft the logo's leading edge is while it's revealed
private const val FLIGHT_MS = 1100     // plane crossing the screen
private const val TRAIL_FADE_MS = 500  // contrail dissolving afterwards
private const val HOLD_MS = 300L       // a beat on the finished logo before moving on

/** Plane's centre x: starts just off the left edge (progress 0) and ends just off the right (1). */
private fun planeX(progress: Float, width: Float, planePx: Float): Float =
    -planePx + (width + 2 * planePx) * progress

/** The plane flies along the logo's centre line, or mid-screen until the logo has been measured. */
private fun flightY(logo: Rect, height: Float): Float =
    if (logo.isEmpty) height / 2 else logo.center.y

@Composable
fun SplashScreen(
    isSetupComplete: Boolean,
    onNavigateNext: (String) -> Unit
) {
    // Always the latest values, without restarting the animation when they change
    val setupComplete by rememberUpdatedState(isSetupComplete)
    val navigateNext by rememberUpdatedState(onNavigateNext)

    val flight = remember { Animatable(0f) }
    val trail = remember { Animatable(1f) }
    var screenWidth by remember { mutableFloatStateOf(0f) }
    var logoBounds by remember { mutableStateOf(Rect.Zero) }
    var finished by remember { mutableStateOf(false) }
    val plane = rememberVectorPainter(Icons.Filled.Flight)

    // Runs once, whether the animation completes or the pilot taps to skip
    fun finish() {
        if (finished) return
        finished = true
        navigateNext(if (setupComplete) Routes.LANDING else Routes.SETUP)
    }

    LaunchedEffect(Unit) {
        flight.animateTo(1f, tween(FLIGHT_MS, easing = LinearEasing))   // steady cruise, no easing
        trail.animateTo(0f, tween(TRAIL_FADE_MS))
        delay(HOLD_MS)
        finish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .onSizeChanged { screenWidth = it.width.toFloat() }
            // Tap anywhere to skip; no ripple, since it isn't a button
            .clickable(interactionSource = null, indication = null) { finish() },
        contentAlignment = Alignment.Center
    ) {
        // 1) Contrails, behind the logo
        Canvas(modifier = Modifier.fillMaxSize()) {
            val planePx = PlaneSize.toPx()
            val x = planeX(flight.value, size.width, planePx)
            val y = flightY(logoBounds, size.height)
            val tail = x - planePx * 0.45f
            if (tail <= 0f || trail.value <= 0f) return@Canvas

            // Bright at the engines, fading out over 60% of the screen behind them
            val trailBrush = Brush.horizontalGradient(
                0f to Color.Transparent,
                1f to Color.White.copy(alpha = 0.8f * trail.value),
                startX = tail - size.width * 0.6f,
                endX = tail
            )
            val engineGap = planePx * 0.18f
            for (dy in floatArrayOf(-engineGap, engineGap)) {
                drawLine(
                    brush = trailBrush,
                    start = Offset(0f, y + dy),
                    end = Offset(tail, y + dy),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // 2) The logo, revealed behind the plane
        Image(
            painter = painterResource(id = R.drawable.ic_fdv_branding),
            contentDescription = "FDV",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
                .heightIn(min = 80.dp, max = 140.dp)
                .onGloballyPositioned { logoBounds = it.boundsInParent() }
                .graphicsLayer {
                    // Offscreen, so the mask below fades only the logo, not the blue behind it
                    compositingStrategy = CompositingStrategy.Offscreen
                    // Settles from 96% to full size as the contrail fades
                    val scale = 1f - 0.04f * trail.value
                    scaleX = scale
                    scaleY = scale
                }
                .drawWithContent {
                    drawContent()
                    // The plane's tail in this image's own coordinates: visible to its left, hidden to its right
                    val planePx = PlaneSize.toPx()
                    val edge = planeX(flight.value, screenWidth, planePx) - planePx / 2 - logoBounds.left
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Black,
                            1f to Color.Transparent,
                            startX = edge - RevealFeather.toPx(),
                            endX = edge
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        )

        // 3) The plane, in front of everything
        Canvas(modifier = Modifier.fillMaxSize()) {
            val planePx = PlaneSize.toPx()
            val x = planeX(flight.value, size.width, planePx)
            val y = flightY(logoBounds, size.height)
            translate(left = x - planePx / 2, top = y - planePx / 2) {
                // The Flight icon points up; turn it to face right
                rotate(degrees = 90f, pivot = Offset(planePx / 2, planePx / 2)) {
                    with(plane) {
                        draw(size = Size(planePx, planePx), colorFilter = ColorFilter.tint(Color.White))
                    }
                }
            }
        }
    }
}