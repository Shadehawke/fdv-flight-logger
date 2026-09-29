package com.fdv.fdvflightlogger.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.draw.drawBehind

@Immutable
data class RaisedColors(
    val base: Color,
    val highlight: Color,
    val shadow: Color,
    val rim: Color
)

/**
 * Derives raised-surface colors from the current theme's background,
 * so FDV, Light and Dark all get a matching effect without extra palette entries.
 */
@Composable
fun raisedColors(): RaisedColors {
    val bg = MaterialTheme.colorScheme.background
    return remember(bg) {
        val isDark = bg.luminance() < 0.5f
        RaisedColors(
            base = bg,
            highlight = if (isDark) lerp(bg, Color.White, 0.10f) else lerp(bg, Color.White, 0.85f),
            shadow = if (isDark) lerp(bg, Color.Black, 0.55f) else lerp(bg, Color.Black, 0.18f),
            rim = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.7f)
        )
    }
}

/**
 * Soft "extruded" surface: a light shadow top-left, a dark shadow bottom-right,
 * a faint top-to-bottom sheen, and a highlight rim along the top edge.
 * Shadows draw outside the bounds, so leave ~16dp of room around the element.
 */
fun Modifier.raised(
    colors: RaisedColors,
    cornerRadius: Dp = 18.dp,
    elevation: Dp = 5.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .drawWithCache {
            val radius = cornerRadius.toPx()
            val offset = elevation.toPx()
            val blur = offset * 2f
            val paint = Paint()
            val framework = paint.asFrameworkPaint().apply { color = colors.base.toArgb() }

            onDrawBehind {
                drawIntoCanvas { canvas ->
                    // setShadowLayer is hardware-accelerated for round rects on API 28+ (minSdk is 31)
                    framework.setShadowLayer(blur, offset, offset, colors.shadow.toArgb())
                    canvas.drawRoundRect(0f, 0f, size.width, size.height, radius, radius, paint)
                    framework.setShadowLayer(blur, -offset, -offset, colors.highlight.toArgb())
                    canvas.drawRoundRect(0f, 0f, size.width, size.height, radius, radius, paint)
                }
            }
        }
        .clip(shape)
        .background(
            Brush.verticalGradient(listOf(lerp(colors.base, colors.highlight, 0.35f), colors.base))
        )
        .border(1.dp, Brush.verticalGradient(listOf(colors.rim, Color.Transparent)), shape)
}

/**
 * Primary action button: brand gradient, colored glow, and a glossy top half.
 */
@Composable
fun GradientButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    val fill = if (enabled) {
        Brush.horizontalGradient(listOf(cs.primary, cs.secondary))
    } else {
        SolidColor(cs.onSurface.copy(alpha = 0.12f))
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = shape,
                ambientColor = cs.primary,
                spotColor = cs.primary
            )
            .clip(shape)
            .background(fill)
            .drawWithContent {
                drawContent()
                // Gloss: a white fade over the top half reads as a reflective surface
                if (enabled) {
                    drawRect(
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.22f),
                            0.5f to Color.Transparent
                        )
                    )
                }
            }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = if (enabled) Color.White else cs.onSurface.copy(alpha = 0.38f)
        )
    }
}

/** Shared shape for all inset input fields. */
val FieldShape = RoundedCornerShape(12.dp)

/**
 * Sunken "well": slightly darker than the surface, with a soft inner shadow
 * on the top and left edges and a faint light rim along the bottom.
 */
fun Modifier.inset(colors: RaisedColors, cornerRadius: Dp = 12.dp): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .clip(shape)
        .background(lerp(colors.base, colors.shadow, 0.10f))
        .drawBehind {
            val depth = 6.dp.toPx()
            drawRect(
                Brush.verticalGradient(
                    listOf(colors.shadow.copy(alpha = 0.45f), Color.Transparent),
                    startY = 0f, endY = depth
                )
            )
            drawRect(
                Brush.horizontalGradient(
                    listOf(colors.shadow.copy(alpha = 0.30f), Color.Transparent),
                    startX = 0f, endX = depth
                )
            )
        }
        .border(1.dp, Brush.verticalGradient(listOf(Color.Transparent, colors.rim)), shape)
}

/**
 * Field colors for inset wells: no fill (the well draws it), no resting underline,
 * brand-colored underline and label only while focused.
 */
@Composable
fun fdvFieldColors(): TextFieldColors {
    val cs = MaterialTheme.colorScheme
    return TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedIndicatorColor = cs.secondary,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        focusedLabelColor = cs.secondary,
        cursorColor = cs.secondary
    )
}

/**
 * Section navigation chip: gradient pill with glow when selected,
 * small raised pill when not.
 */
@Composable
fun SectionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val raised = raisedColors()
    val shape = RoundedCornerShape(50)

    val surface = if (selected) {
        Modifier
            .shadow(8.dp, shape, ambientColor = cs.primary, spotColor = cs.primary)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(cs.primary, cs.secondary)))
    } else {
        Modifier.raised(raised, cornerRadius = 50.dp, elevation = 3.dp)
    }

    Box(
        modifier = surface
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else cs.onSurface
        )
    }
}