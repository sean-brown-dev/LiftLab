package com.browntowndev.liftlab.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun CustomAnchorDropdown(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit,
    anchor: @Composable (BoxScope.(Modifier) -> Unit),
    menuItems: @Composable (ColumnScope.() -> Unit),
) {
    Box(
        modifier = modifier.then(Modifier.wrapContentSize(Alignment.Center))
    ) {
        anchor(Modifier.clickable { onToggleExpansion() })
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = onToggleExpansion,
            content = menuItems
        )
    }
}