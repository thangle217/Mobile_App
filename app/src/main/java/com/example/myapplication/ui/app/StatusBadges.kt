package com.example.myapplication.ui.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A compact badge/chip showing the occupancy status of a room.
 *
 * @param status One of: "occupied", "vacant", "maintenance".
 */
@Composable
fun RoomStatusBadge(status: String, modifier: Modifier = Modifier) {
    val (label, bgColor, textColor) = when (status.lowercase()) {
        "occupied"    -> Triple("Occupied",    Color(0xFFDCFCE7), Color(0xFF166534))
        "vacant"      -> Triple("Vacant",      Color(0xFFEFF6FF), Color(0xFF1D4ED8))
        "maintenance" -> Triple("Maintenance", Color(0xFFFEF3C7), Color(0xFF92400E))
        else          -> Triple(status,        Color(0xFFF3F4F6), Color(0xFF374151))
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50.dp),
        color = bgColor,
        tonalElevation = 0.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * A compact badge indicating the current status of a contract.
 * Statuses: "active", "expired", "pending", "terminated".
 */
@Composable
fun ContractStatusBadge(status: String, modifier: Modifier = Modifier) {
    val (label, bgColor, textColor) = when (status.lowercase()) {
        "active"     -> Triple("Active",     Color(0xFFDCFCE7), Color(0xFF166534))
        "expired"    -> Triple("Expired",    Color(0xFFFEE2E2), Color(0xFF991B1B))
        "pending"    -> Triple("Pending",    Color(0xFFFEF3C7), Color(0xFF92400E))
        "terminated" -> Triple("Terminated", Color(0xFFF3F4F6), Color(0xFF374151))
        else         -> Triple(status,       Color(0xFFF3F4F6), Color(0xFF374151))
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50.dp),
        color = bgColor,
        tonalElevation = 0.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
