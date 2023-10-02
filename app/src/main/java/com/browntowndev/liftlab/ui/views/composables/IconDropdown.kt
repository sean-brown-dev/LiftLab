package com.browntowndev.liftlab.ui.views.composables

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun IconDropdown(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.MoreVert,
    painter: Painter? = null,
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit,
    iconSize: Dp = 24.dp,
    iconTint: Color = MaterialTheme.colorScheme.onBackground,
    dropdownColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    menuItems: @Composable (ColumnScope.() -> Unit),
) {
    Box(
        modifier = modifier.wrapContentSize(Alignment.TopStart)
    ) {
        IconButton(onClick = onToggleExpansion) {
            if (painter != null) {
                Icon(painter = painter, contentDescription = null, modifier = Modifier.size(iconSize), tint = iconTint)
            }
            else {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(iconSize), tint = iconTint)
            }
        }
            DropdownMenu(
                modifier = Modifier.background(color = dropdownColor),
                expanded = isExpanded,
                onDismissRequest = onToggleExpansion,
            ) {
                menuItems()
            }
    }
}