package com.browntowndev.liftlab.ui.views.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MoreVertDropdown(
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit,
    iconSize: Dp = 24.dp,
    iconTint: Color = MaterialTheme.colorScheme.onBackground,
    menuItems: @Composable() (ColumnScope.() -> Unit),
) {
    Box(
        modifier = Modifier.wrapContentSize(Alignment.TopStart)
    ) {
        IconButton(onClick = onToggleExpansion) {
            Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(iconSize), tint = iconTint)
        }
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = onToggleExpansion,
            content = menuItems
        )
    }
}