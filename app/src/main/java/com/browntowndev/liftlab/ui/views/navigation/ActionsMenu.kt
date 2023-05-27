package com.browntowndev.liftlab.ui.views.navigation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.ui.models.ActionMenuItem

@Composable
fun ActionsMenu(
    items: MutableList<ActionMenuItem>,
    isOpen: Boolean,
    onToggleOverflow: () -> Unit,
    maxVisibleItems: Int,
) {
    val menuItems = remember(
        key1 = items,
        key2 = maxVisibleItems,
    ) {
        splitMenuItems(items, maxVisibleItems)
    }

    menuItems.alwaysShownItems.filterIsInstance<ActionMenuItem.IconMenuItem>().forEach { item ->
        if (item.isVisible) {
            IconButton(onClick = item.onClick) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = stringResource(id = item.contentDescriptionResourceId),
                )
            }
        }
    }

    menuItems.alwaysShownItems.filterIsInstance<ActionMenuItem.TextInputMenuItem>().forEach { item ->
        if (item.isVisible) {
            RememberedOutlinedTextField(item)
        }
    }

    if (menuItems.overflowItems.isNotEmpty()) {
        IconButton(onClick = onToggleOverflow) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(id = R.string.accessibility_overflow),
            )
        }
        DropdownMenu(
            expanded = isOpen,
            onDismissRequest = onToggleOverflow,
        ) {
            menuItems.overflowItems.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(item.title)
                    },
                    onClick = item.onClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RememberedOutlinedTextField(item: ActionMenuItem.TextInputMenuItem) {
    val rememberedValue = remember { mutableStateOf(item.value) }
    val focusRequester = remember { FocusRequester() }

    OutlinedTextField(
        modifier = Modifier.focusRequester(focusRequester),
        value = rememberedValue.value,
        leadingIcon = { Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )},
        trailingIcon = {IconButton(onClick = item.onClickTrailingIcon) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )}},
        onValueChange = { newValue ->
            rememberedValue.value = newValue
            item.onValueChange(newValue)
        },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
            focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            focusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
            unfocusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        shape = RoundedCornerShape(45.dp)
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

private data class MenuItems(
    val alwaysShownItems: MutableList<ActionMenuItem>,
    val overflowItems: List<ActionMenuItem.IconMenuItem>,
)

private fun splitMenuItems(
    items: List<ActionMenuItem>,
    maxVisibleItems: Int,
): MenuItems {
    val alwaysShownItems: MutableList<ActionMenuItem> =
        items.filterIsInstance<ActionMenuItem.IconMenuItem.AlwaysShown>().filter{ i -> i.isVisible}.toMutableList()

    alwaysShownItems.addAll(items.filterIsInstance<ActionMenuItem.TextInputMenuItem>())

    val ifRoomItems: MutableList<ActionMenuItem.IconMenuItem> =
        items.filterIsInstance<ActionMenuItem.IconMenuItem.ShownIfRoom>().filter{ i -> i.isVisible}.toMutableList()

    val overflowItems: List<ActionMenuItem.IconMenuItem> =
        items.filterIsInstance<ActionMenuItem.IconMenuItem.NeverShown>().filter{ i -> i.isVisible}

    val hasOverflow = overflowItems.isNotEmpty() ||
            (alwaysShownItems.size + ifRoomItems.size - 1) > maxVisibleItems

    val usedSlots = alwaysShownItems.size + (if (hasOverflow) 1 else 0)

    val availableSlots = maxVisibleItems - usedSlots

    if (availableSlots > 0 && ifRoomItems.isNotEmpty()) {
        val visible = ifRoomItems.subList(0, availableSlots.coerceAtMost(ifRoomItems.size))
        alwaysShownItems.addAll(visible)
        ifRoomItems.removeAll(visible)
    }

    return MenuItems(
        alwaysShownItems = alwaysShownItems,
        overflowItems = ifRoomItems + overflowItems,
    )
}