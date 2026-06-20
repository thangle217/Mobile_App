package com.example.myapplication.ui.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A horizontal scrollable tab row for filtering list content.
 * Renders a pill-style tab for each item in [tabs].
 *
 * @param tabs List of tab labels to display.
 * @param selectedIndex Currently selected tab index.
 * @param onTabSelected Callback with the new tab index when tapped.
 */
@Composable
fun FilterTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        edgePadding = 16.dp,
        indicator = {},        // Hide default indicator; pill handles selection
        divider = {}
    ) {
        tabs.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            FilterTabItem(
                label = label,
                selected = selected,
                onClick = { onTabSelected(index) }
            )
        }
    }
}

@Composable
private fun FilterTabItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary
                         else MaterialTheme.colorScheme.surfaceVariant
    val contentColor   = if (selected) MaterialTheme.colorScheme.onPrimary
                         else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50.dp),
        color = containerColor,
        modifier = Modifier.padding(end = 8.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
