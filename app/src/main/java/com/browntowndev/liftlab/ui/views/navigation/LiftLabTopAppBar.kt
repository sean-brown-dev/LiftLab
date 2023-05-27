package com.browntowndev.liftlab.ui.views.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.browntowndev.liftlab.ui.models.TopAppBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLabTopAppBar(
    state: MutableState<TopAppBarState>,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    LargeTopAppBar(
        colors = topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        navigationIcon = {
            val icon = state.value.navigationIcon
            val callback = state.value.onNavigationIconClick
            if (icon != null && (state.value.navigationIconVisible == true)) {
                IconButton(onClick = { callback?.invoke() }) {
                    Icon(
                        imageVector = icon,
                        contentDescription = state.value.navigationIconContentDescription
                    )
                }
            }
        },
        title = {
            val title = state.value.title
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        actions = {
            val items = state.value.actions
            if (items.isNotEmpty()) {
                ActionsMenu(
                    items = items,
                    isOpen = isMenuExpanded,
                    onToggleOverflow = { isMenuExpanded = !isMenuExpanded },
                    maxVisibleItems = 3,
                )
            }
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior
    )

    val onExecuteBackNavigation = state.value.onNavigationIconClick;
    BackHandler(state.value.navigationIconVisible == true) {
        onExecuteBackNavigation?.invoke()
    }
}