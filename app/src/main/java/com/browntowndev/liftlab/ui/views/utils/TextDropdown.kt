package com.browntowndev.liftlab.ui.views.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit

@Composable
fun TextDropdown(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit,
    text: String,
    fontSize: TextUnit,
    menuItems: @Composable() (ColumnScope.() -> Unit),
) {
    Box(
        modifier = modifier.wrapContentSize(Alignment.TopStart)
    ) {
        TextDropdownTextAnchor(
            text = text,
            fontSize = fontSize,
            modifier = modifier.clickable { onToggleExpansion() },
        )
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = onToggleExpansion,
            content = menuItems
        )
    }
}

@Composable
fun TextDropdownTextAnchor(text: String, fontSize: TextUnit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        Text(
            text = text,
            fontSize = fontSize,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier,
        )
    }
}