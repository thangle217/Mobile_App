package com.example.myapplication.ui.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A circular progress indicator showing an occupancy ratio (0f – 1f).
 * Animated on initial composition.
 *
 * @param progress Value between 0f (empty) and 1f (fully occupied).
 * @param size Diameter of the ring (default 64.dp).
 * @param strokeWidth Width of the arc stroke.
 */
@Composable
fun OccupancyRingIndicator(
    progress: Float,
    size: Dp = 64.dp,
    strokeWidth: Dp = 6.dp,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = EaseOutCubic),
        label = "occupancyProgress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier.size(size)) {
        val strokePx = strokeWidth.toPx()
        val inset = strokePx / 2f
        val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
        val topLeft = Offset(inset, inset)

        // Background track
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        // Progress arc
        drawArc(
            color = primaryColor,
            startAngle = -90f,
            sweepAngle = 360f * animatedProgress,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
    }
}
