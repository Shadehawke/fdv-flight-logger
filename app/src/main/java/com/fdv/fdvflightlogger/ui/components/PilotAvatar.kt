package com.fdv.fdvflightlogger.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The pilot's photo in a glowing ring, or their initials when there's no photo. */
@Composable
fun PilotAvatar(
    photo: Bitmap?,
    name: String?,
    callsign: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    onClick: (() -> Unit)? = null
) {
    // Converted once per photo, not on every recomposition
    val image = remember(photo) { photo?.asImageBitmap() }

    GlowDisc(modifier = modifier, size = size, onClick = onClick) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "Your photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initialsOf(name, callsign),
                style = if (size >= 56.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                color = Color.White
            )
        }
    }
}

/** A glowing gradient disc with a glossy top, matching GradientButton and the update badge. */
@Composable
internal fun GlowDisc(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .shadow(14.dp, CircleShape, ambientColor = cs.primary, spotColor = cs.primary)
            .clip(CircleShape)
            // After the clip, so the touch ripple is round too
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(Brush.linearGradient(listOf(cs.primary, cs.secondary)))
            .drawBehind {
                drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.30f), 0.5f to Color.Transparent))
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// "Shawn Spitaleri" → "SS"; falls back to the callsign's letters ("DAL1703" → "DA")
internal fun initialsOf(name: String?, callsign: String?): String {
    val fromName = name.orEmpty()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
    if (fromName.isNotEmpty()) return fromName
    return callsign.orEmpty().filter { it.isLetter() }.take(2).uppercase().ifEmpty { "FD" }
}