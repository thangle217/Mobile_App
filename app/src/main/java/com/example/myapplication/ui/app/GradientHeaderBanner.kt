package com.example.myapplication.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.Spacing

/**
 * A gradient header banner for detail/profile screens.
 * Displays the property/room name, address, and a status badge.
 *
 * @param title Primary title (e.g. property name).
 * @param subtitle Subtitle text (e.g. address or description).
 * @param statusLabel Label for the status badge.
 * @param statusColor Background color for the badge.
 * @param gradientStart Start color of the header gradient.
 * @param gradientEnd End color of the header gradient.
 */
@Composable
fun GradientHeaderBanner(
    title: String,
    subtitle: String,
    statusLabel: String = "",
    statusColor: Color = Color.Transparent,
    gradientStart: Color = Color(0xFF0D9488),
    gradientEnd: Color = Color(0xFF0891B2),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(Brush.linearGradient(listOf(gradientStart, gradientEnd)))
            .padding(horizontal = Spacing.large, vertical = Spacing.large)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            if (statusLabel.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
