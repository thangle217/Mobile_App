package com.example.myapplication.ui.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A circular avatar displaying the tenant's initials.
 * Falls back to a colored placeholder when no photo is available.
 *
 * @param name Full name of the tenant; initials are extracted from this.
 * @param size Diameter of the avatar circle (default 40.dp).
 */
@Composable
fun TenantAvatar(
    name: String,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val initials = name
        .trim()
        .split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }

    // Deterministic color based on name hash
    val hue = (name.hashCode().and(0xFF)) / 255f * 360f
    val bgColor = androidx.compose.ui.graphics.Color.hsl(hue, 0.55f, 0.55f)
    val textColor = androidx.compose.ui.graphics.Color.White

    Surface(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        color = bgColor,
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = initials,
                color = textColor,
                fontSize = (size.value * 0.38f).sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
