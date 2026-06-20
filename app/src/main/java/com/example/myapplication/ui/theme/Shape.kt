package com.example.myapplication.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Design system shape/corner radius tokens for consistent rounded corners.
 */
object Shape {
    /** Slightly rounded for inputs, chips */
    val cornerSmall = 8.dp
    /** Standard card and surface rounding */
    val cornerMedium = 12.dp
    /** Prominent cards, dialogs */
    val cornerLarge = 16.dp
    /** Pill-shaped buttons and badges */
    val cornerFull = 50.dp
    /** Bottom sheet top corners */
    val cornerSheet = 24.dp
}
