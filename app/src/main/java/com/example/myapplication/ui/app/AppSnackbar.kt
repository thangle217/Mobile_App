package com.example.myapplication.ui.app

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

enum class SnackbarType { SUCCESS, ERROR, WARNING, INFO }

data class AppSnackbarState(
    val message: String,
    val type: SnackbarType = SnackbarType.INFO,
    val durationMs: Long = 3000L
)

/**
 * Animated in-app snackbar that shows above the bottom navigation bar.
 * Supports SUCCESS, ERROR, WARNING, INFO variants with distinct colors and icons.
 */
@Composable
fun AppSnackbar(
    state: AppSnackbarState?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        if (state == null) return@AnimatedVisibility

        LaunchedEffect(state) {
            delay(state.durationMs)
            onDismiss()
        }

        val (backgroundColor, contentColor, icon) = when (state.type) {
            SnackbarType.SUCCESS -> Triple(Color(0xFF16A34A), Color.White, Icons.Default.CheckCircle)
            SnackbarType.ERROR   -> Triple(Color(0xFFDC2626), Color.White, Icons.Default.Error)
            SnackbarType.WARNING -> Triple(Color(0xFFF59E0B), Color(0xFF1C1917), Icons.Default.Warning)
            SnackbarType.INFO    -> Triple(Color(0xFF0891B2), Color.White, Icons.Default.Info)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(backgroundColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor)
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
