package com.browntowndev.liftlab.ui.views.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.ui.models.ActionMenuItem
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.TopAppBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.LiftLabTopAppBarState
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.views.utils.FocusedRoundTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionsMenu(
    topAppBarState: LiftLabTopAppBarState,
    topAppBarViewModel: TopAppBarViewModel,
    maxVisibleItems: Int,
) {
    if (topAppBarState.actions.isEmpty()) return;

    val menuItems = remember(
        key1 = topAppBarState.actions,
        key2 = maxVisibleItems,
    ) {
        splitMenuItems(topAppBarState.actions, maxVisibleItems)
    }

    menuItems.alwaysShownItems.filterIsInstance<ActionMenuItem.IconMenuItem>().forEach { item ->
        if (item.isVisible) {
            IconButton(onClick = item.onClick) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = if(item.contentDescriptionResourceId != null)  stringResource(id = item.contentDescriptionResourceId as Int) else null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    menuItems.alwaysShownItems.filterIsInstance<ActionMenuItem.TextInputMenuItem>().forEach { item ->
        if (item.isVisible) {
            FocusedOutlinedTextField(item, topAppBarViewModel = topAppBarViewModel)
        }
    }

    if (menuItems.overflowItems.isNotEmpty()) {
        IconButton(onClick = { topAppBarViewModel.setControlVisibility(Screen.OVERFLOW_MENU, true) }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(id = R.string.accessibility_overflow),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        DropdownMenu(
            expanded = topAppBarState.isOverflowMenuExpanded,
            onDismissRequest = { topAppBarViewModel.setControlVisibility(Screen.OVERFLOW_MENU, false) },
        ) {
            menuItems.overflowItems.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(item.title)
                    },
                    leadingIcon = {
                        Icon(imageVector = item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                    },
                    onClick = {
                        topAppBarViewModel.setControlVisibility(Screen.OVERFLOW_MENU, false)
                        item.onClick()
                    }
                )
                if (item.dividerBelow) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Divider(
                            modifier = Modifier.fillMaxWidth(.85f),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FocusedOutlinedTextField(item: ActionMenuItem.TextInputMenuItem, topAppBarViewModel: TopAppBarViewModel,) {
    FocusedRoundTextField(
        value = item.value,
        leadingIcon = {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        },
        trailingIcon = {
            IconButton(onClick = item.onClickTrailingIcon) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
       },
        onValueChange = {
            item.onValueChange(it)
            topAppBarViewModel.mutateControlValue(AppBarMutateControlRequest(item.controlName, it))
        }
    )
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