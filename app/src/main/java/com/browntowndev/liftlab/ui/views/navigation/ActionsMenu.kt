package com.browntowndev.liftlab.ui.views.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.ui.composables.FocusableRoundTextField
import com.browntowndev.liftlab.ui.models.ActionMenuItem
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.TopAppBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.LiftLabTopAppBarState
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen

@Composable
fun ActionsMenu(
    topAppBarState: LiftLabTopAppBarState,
    topAppBarViewModel: TopAppBarViewModel,
    maxVisibleItems: Int,
) {
    if (topAppBarState.actions.isEmpty()) return

    val menuItems = remember(
        key1 = topAppBarState.actions,
        key2 = maxVisibleItems,
    ) {
        splitMenuItems(topAppBarState.actions, maxVisibleItems)
    }

    menuItems.alwaysShownItems.filterIsInstance<ActionMenuItem.TextInputMenuItem>().fastForEach { item ->
        if (item.isVisible) {
            FocusedOutlinedTextField(item, topAppBarViewModel = topAppBarViewModel)
        }
    }

    menuItems.alwaysShownItems.filterIsInstance<ActionMenuItem.IconMenuItem>().fastForEach { item ->
        if (item.isVisible) {
            IconButton(
                onClick = {
                    item.onClick().fastForEach {
                        topAppBarViewModel.setControlVisibility(it.first, it.second)
                    }
                }
            ) {
                item.icon?.onLeft {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = it,
                        contentDescription = if(item.contentDescriptionResourceId != null)  stringResource(id = item.contentDescriptionResourceId as Int) else null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }?.onRight {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = it),
                        contentDescription = if(item.contentDescriptionResourceId != null)  stringResource(id = item.contentDescriptionResourceId as Int) else null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }

    menuItems.alwaysShownItems.filterIsInstance<ActionMenuItem.ButtonMenuItem.AlwaysShown>().fastForEach { item ->
        if (item.isVisible) {
            Button(
                modifier = Modifier.padding(end = 10.dp),
                shape = RoundedCornerShape(5.dp),
                onClick = item.onClick,
                content = item.buttonContent
            )
        }
    }

    if (menuItems.overflowItems.isNotEmpty() && topAppBarState.isOverflowMenuIconVisible) {
        IconButton(onClick = { topAppBarViewModel.setControlVisibility(Screen.OVERFLOW_MENU, true) }) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(id = R.string.accessibility_overflow),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        DropdownMenu(
            expanded = topAppBarState.isOverflowMenuExpanded,
            onDismissRequest = { topAppBarViewModel.setControlVisibility(Screen.OVERFLOW_MENU, false) },
        ) {
            menuItems.overflowItems.fastForEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(item.title)
                    },
                    leadingIcon = {
                        item.icon?.onLeft {
                            Icon(
                                imageVector = it,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }?.onRight {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(id = it),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    trailingIcon = {
                        if(item.trailingIconText != null) {
                            Text(item.trailingIconText!!, color = MaterialTheme.colorScheme.tertiary)
                        }
                    },
                    onClick = {
                        topAppBarViewModel.setControlVisibility(Screen.OVERFLOW_MENU, false)
                        item.onClick().fastForEach {
                            topAppBarViewModel.setControlVisibility(it.first, it.second)
                        }
                    }
                )
                if (item.dividerBelow) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun FocusedOutlinedTextField(
    item: ActionMenuItem.TextInputMenuItem,
    topAppBarViewModel: TopAppBarViewModel,
) {
    FocusableRoundTextField(
        modifier = Modifier.padding(end = 15.dp),
        value = item.value,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.tertiaryContainer,
            focusedBorderColor = MaterialTheme.colorScheme.tertiaryContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            focusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            focusedTextColor = MaterialTheme.colorScheme.onTertiaryContainer,
            unfocusedTextColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        leadingIcon = {
            item.icon?.onLeft {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }?.onRight {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = it),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        trailingIcon = {
            IconButton(onClick = {
                item.onClickTrailingIcon().fastForEach {
                    topAppBarViewModel.setControlVisibility(it.first, it.second)
                }
            }) {
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

    alwaysShownItems.addAll(items.filterIsInstance<ActionMenuItem.TextInputMenuItem>().filter { it.isVisible })
    alwaysShownItems.addAll(items.filterIsInstance<ActionMenuItem.ButtonMenuItem>().filter { it.isVisible })

    val ifRoomItems: MutableList<ActionMenuItem.IconMenuItem> =
        items.filterIsInstance<ActionMenuItem.IconMenuItem.ShownIfRoom>().filter{ i -> i.isVisible}.toMutableList()

    val overflowItems: List<ActionMenuItem.IconMenuItem> =
        items.filterIsInstance<ActionMenuItem.IconMenuItem.NeverShown>().filter{ i -> i.isVisible}.toMutableStateList()

    val hasOverflow = overflowItems.isNotEmpty() ||
            (alwaysShownItems.size + ifRoomItems.size - 1) > maxVisibleItems

    val usedSlots = alwaysShownItems.size + (if (hasOverflow) 1 else 0)

    val availableSlots = maxVisibleItems - usedSlots

    if (availableSlots > 0 && ifRoomItems.isNotEmpty()) {
        val visible = ifRoomItems.subList(0, availableSlots.coerceAtMost(ifRoomItems.size))
        alwaysShownItems.addAll(visible)
        ifRoomItems.removeAll(visible.toSet())
    }

    return MenuItems(
        alwaysShownItems = alwaysShownItems,
        overflowItems = ifRoomItems + overflowItems,
    )
}