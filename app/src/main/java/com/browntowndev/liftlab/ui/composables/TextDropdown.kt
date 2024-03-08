package com.browntowndev.liftlab.ui.composables

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit

@Composable
fun TextDropdown(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit,
    text: String,
    fontSize: TextUnit,
    menuItems: @Composable (ColumnScope.() -> Unit),
) {
    Box(
        modifier = modifier.then(Modifier.wrapContentSize(Alignment.Center))
    ) {
        TextDropdownTextAnchor(
            text = text,
            fontSize = fontSize,
            modifier = Modifier.clickable { onToggleExpansion() },
        )
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = onToggleExpansion,
            content = menuItems
        )
    }
}

@Composable
fun TextDropdownTextAnchor(
    modifier: Modifier = Modifier,
    text: String,
    fontSize: TextUnit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            fontSize = fontSize,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier,
        )
    }
}