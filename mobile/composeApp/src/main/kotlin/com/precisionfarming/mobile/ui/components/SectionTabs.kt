package com.precisionfarming.mobile.ui.components

import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun SectionTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    if (labels.isEmpty()) return
    ScrollableTabRow(
        selectedTabIndex = selectedIndex.coerceIn(0, labels.lastIndex),
        edgePadding = 8.dp,
    ) {
        labels.forEachIndexed { index, label ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                text = { Text(label) },
            )
        }
    }
}
