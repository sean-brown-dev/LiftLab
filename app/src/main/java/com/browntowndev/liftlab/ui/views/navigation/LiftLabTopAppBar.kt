package com.browntowndev.liftlab.ui.views.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.ui.composables.component.ProgressCountdownTimer
import com.browntowndev.liftlab.ui.models.controls.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.appBar.CountdownTimerState
import com.browntowndev.liftlab.ui.viewmodels.appBar.LiftLabTopAppBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLabTopAppBar(
    state: LiftLabTopAppBarState,
    // ⚡ Bolt: Using a provider (() -> CountdownTimerState) instead of the raw state object
    // prevents this parent composable from recomposing every second when the timer ticks.
    timerStateProvider: () -> CountdownTimerState,
    allowExpansion: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    onCancelRestTimer: () -> Unit,
    onSetControlVisibility: (String, Boolean) -> Unit,
    onMutateControlValue: (AppBarMutateControlRequest<String>) -> Unit
) {
    if (allowExpansion) {
        LargeTopAppBar(
            colors = topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            navigationIcon = {
                NavigationIcon(state = state)
            },
            title = {
                Title(
                    state = state,
                    timerStateProvider = timerStateProvider,
                    titleFontSize = 32.sp ,
                    subtitleFontSize = 18.sp,
                    onCancelRestTimer = onCancelRestTimer,
                )
            },
            actions = {
                ActionsMenu(
                    topAppBarState = state,
                    maxVisibleItems = 3,
                    onSetControlVisibility = onSetControlVisibility,
                    onMutateControlValue = onMutateControlValue
                )
            },
            scrollBehavior = scrollBehavior
        )
    } else {
        TopAppBar(
            colors = topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            navigationIcon = {
                NavigationIcon(state = state)
            },
            title = {
                Title(
                    state = state,
                    timerStateProvider = timerStateProvider,
                    titleFontSize = 25.sp,
                    subtitleFontSize = 14.sp,
                    onCancelRestTimer = onCancelRestTimer,
                )
            },
            actions = {
                ActionsMenu(
                    topAppBarState = state,
                    maxVisibleItems = 3,
                    onSetControlVisibility = onSetControlVisibility,
                    onMutateControlValue = onMutateControlValue
                )
            },
            scrollBehavior = scrollBehavior
        )
    }

    BackHandler(state.navigationIconVisible == true) {
        state.onNavigationIconClick?.invoke()
    }
}

@Composable
private fun NavigationIcon(state: LiftLabTopAppBarState) {
    val navIconVisible = (state.navigationIconVisible == true)
    val icon = state.navigationIconImageVector
    val iconResource = state.navigationIconResourceId
    val callback = state.onNavigationIconClick
    if (icon != null && navIconVisible) {
        IconButton(onClick = { callback?.invoke() }) {
            Icon(
                imageVector = icon,
                contentDescription = state.navigationIconContentDescription
            )
        }
    } else if (iconResource != null && navIconVisible) {
        IconButton(onClick = { callback?.invoke() }) {
            Icon(
                painter = painterResource(id = iconResource),
                contentDescription = state.navigationIconContentDescription
            )
        }
    }
}

@Composable
private fun Title(
    state: LiftLabTopAppBarState,
    timerStateProvider: () -> CountdownTimerState,
    titleFontSize: TextUnit = 25.sp,
    subtitleFontSize: TextUnit = 14.sp,
    onCancelRestTimer: () -> Unit,
) {
    if (state.title.isNotEmpty()) {
        Column {
            Text(
                text = state.title,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = titleFontSize
            )
            if (state.subtitle.isNotEmpty()) {
                Text(
                    text = state.subtitle,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = subtitleFontSize,
                )
            }
        }
    } else if (state.screenHasRestTimer) {
        Box (
            contentAlignment = Alignment.CenterStart,
        ) {
            ProgressCountdownTimer(
                runningProvider = { timerStateProvider().running },
                progressProvider = { timerStateProvider().progress },
                timeRemainingProvider = { timerStateProvider().timeRemaining },
                onCancel = {
                    onCancelRestTimer()
                }
            )
            Icon(
                modifier = Modifier.size(25.dp),
                painter = painterResource(id = R.drawable.stopwatch_icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}