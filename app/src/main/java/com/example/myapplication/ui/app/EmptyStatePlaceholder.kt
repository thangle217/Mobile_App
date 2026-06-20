package com.example.myapplication.ui.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.Spacing

/**
 * Reusable empty-state placeholder displayed when a list has no items.
 *
 * @param icon Emoji or character icon to display.
 * @param title Primary message (e.g. "No rooms found").
 * @param subtitle Optional descriptive text for guidance.
 * @param actionLabel Optional label for a CTA button.
 * @param onAction Callback when CTA is tapped.
 */
@Composable
fun EmptyStatePlaceholder(
    icon: String,
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        // Icon
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Text(
                text = icon,
                fontSize = 48.sp,
                modifier = Modifier.padding(20.dp)
            )
        }

        Spacer(Modifier.height(Spacing.small))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        // Subtitle
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        // CTA
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.small))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(50.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}
