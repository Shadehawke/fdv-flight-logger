package com.fdv.fdvflightlogger.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fdv.fdvflightlogger.ui.theme.DeltaBlue
import com.fdv.fdvflightlogger.ui.theme.DeltaDarkBlue
import com.fdv.fdvflightlogger.ui.theme.DeltaLightBlue
import com.fdv.fdvflightlogger.ui.theme.DeltaRed

// A touch brighter than Delta blue, for the far end of the gradient
private val BarGlow = Color(0xFF0D4F91)

/**
 * The app's title bar: Delta blue with depth (gradient, gloss, a red-to-blue accent line and a
 * shadow). Same slots as TopAppBar, so screens switch over by name. Branded in every theme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FdvTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        // Transparent, so the brushes drawn behind show through
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        modifier = modifier
            .shadow(elevation = 6.dp)
            .drawWithCache {
                // Built once per size, not every frame
                val base = Brush.linearGradient(
                    colors = listOf(DeltaDarkBlue, DeltaBlue, BarGlow),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
                val gloss = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.10f),
                    0.6f to Color.Transparent
                )
                val accentHeight = 2.dp.toPx()
                val accent = Brush.horizontalGradient(
                    0f to DeltaRed.copy(alpha = 0f),
                    0.15f to DeltaRed,
                    0.65f to DeltaLightBlue,
                    1f to DeltaLightBlue.copy(alpha = 0f)
                )
                onDrawBehind {
                    drawRect(base)
                    drawRect(gloss)
                    drawRect(
                        brush = accent,
                        topLeft = Offset(0f, size.height - accentHeight),
                        size = Size(size.width, accentHeight)
                    )
                }
            }
    )
}