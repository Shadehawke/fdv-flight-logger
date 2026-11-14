package com.fdv.fdvflightlogger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fdv.fdvflightlogger.data.db.PirepStatus
import com.fdv.fdvflightlogger.ui.theme.DeltaGreen
import com.fdv.fdvflightlogger.ui.theme.DeltaOrange

/** A small coloured pill: green Approved, orange Pending, red Rejected. */
@Composable
fun PirepStatusBadge(status: PirepStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        PirepStatus.APPROVED -> DeltaGreen
        PirepStatus.PENDING -> DeltaOrange
        PirepStatus.REJECTED -> MaterialTheme.colorScheme.error
    }
    Text(
        text = status.label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            // A wash of the same colour behind the text, so it reads in light and dark themes
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}