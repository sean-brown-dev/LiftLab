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
import androidx.compose.ui.Modifier
import com.browntowndev.liftlab.ui.viewmodels.TopAppBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.topAppBar.LiftLabTopAppBarState
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLabTopAppBar(
    state: LiftLabTopAppBarState,
    modifier: Modifier = Modifier,
    topAppBarViewModel: TopAppBarViewModel = getViewModel(),
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
) {
    LargeTopAppBar(
        colors = topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        navigationIcon = {
            val icon = state.navigationIcon
            val callback = state.onNavigationIconClick
            if (icon != null && (state.navigationIconVisible == true)) {
                IconButton(onClick = { callback?.invoke() }) {
                    Icon(
                        imageVector = icon,
                        contentDescription = state.navigationIconContentDescription
                    )
                }
            }
        },
        title = {
            val title = state.title
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        actions = {
            ActionsMenu(
                topAppBarState = state,
                topAppBarViewModel = topAppBarViewModel,
                maxVisibleItems = 3,
            )
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior
    )

    BackHandler(state.navigationIconVisible == true) {
        state.onNavigationIconClick?.invoke()
    }
}