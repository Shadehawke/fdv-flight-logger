package com.fdv.fdvflightlogger.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.fdv.fdvflightlogger.R
import com.fdv.fdvflightlogger.ui.nav.Routes

@Composable
fun SplashScreen(
    isSetupComplete: Boolean,
    onNavigateNext: (String) -> Unit
) {
    // 1) Kick animation immediately (first frame)
    var animateIn by remember { mutableStateOf(false) }

    // 2) Animate alpha + scale
    val alpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "splashAlpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.92f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "splashScale"
    )

    // 3) Run the animation + navigate after a short pause
    LaunchedEffect(isSetupComplete) {
        animateIn = true
        delay(650) // adjust: 450–800ms is the usual sweet spot
        onNavigateNext(if (isSetupComplete) Routes.FLIGHT_LOG else Routes.SETUP)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Use your theme primary so it matches FDV (Delta Blue)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_fdv_branding), // use your centered logo asset
            contentDescription = "FDV",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
                .heightIn(min = 80.dp, max = 140.dp)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale
                )
                .alpha(alpha)
        )
    }
}
