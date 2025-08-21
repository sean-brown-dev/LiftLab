package com.browntowndev.liftlab.ui.views.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
    timerState: CountdownTimerState,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior,
    onCancelRestTimer: () -> Unit,
    onSetControlVisibility: (String, Boolean) -> Unit,
    onMutateControlValue: (AppBarMutateControlRequest<String>) -> Unit
) {
    val transition = updateTransition(targetState = state.isCollapsed, label = "appBarTransition")

    val appBarAlpha by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = 500, easing = FastOutSlowInEasing)
            } else {
                tween(durationMillis = 500, easing = FastOutSlowInEasing)
            }
        }, label = "appBarTransition"
    ) { isCollapsed -> if (isCollapsed) 1f else 0f }

    if (state.isCollapsed) {
        LiftLabSmallTopAppBar(
            modifier = modifier.alpha(appBarAlpha),
            state = state,
            timerState = timerState,
            scrollBehavior = scrollBehavior,
            onCancelRestTimer = onCancelRestTimer,
            onSetControlVisibility = onSetControlVisibility,
            onMutateControlValue = onMutateControlValue,
        )
    }
    else {
        LiftLabLargeTopAppBar(
            modifier = modifier.alpha(1f - appBarAlpha),
            scrollBehavior = scrollBehavior,
            state = state,
            timerState = timerState,
            onCancelRestTimer = onCancelRestTimer,
            onSetControlVisibility = onSetControlVisibility,
            onMutateControlValue = onMutateControlValue,
        )
    }

    BackHandler(state.navigationIconVisible == true) {
        state.onNavigationIconClick?.invoke()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiftLabLargeTopAppBar(
    state: LiftLabTopAppBarState,
    timerState: CountdownTimerState,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
    onCancelRestTimer: () -> Unit,
    onSetControlVisibility: (String, Boolean) -> Unit,
    onMutateControlValue: (AppBarMutateControlRequest<String>) -> Unit
) {
    LargeTopAppBar(
        modifier = modifier,
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
                timerState = timerState,
                titleFontSize = 32.sp,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiftLabSmallTopAppBar(
    state: LiftLabTopAppBarState,
    timerState: CountdownTimerState,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior { false },
    onCancelRestTimer: () -> Unit,
    onSetControlVisibility: (String, Boolean) -> Unit,
    onMutateControlValue: (AppBarMutateControlRequest<String>) -> Unit
) {
    TopAppBar(
        modifier = modifier,
        scrollBehavior = scrollBehavior,
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
                timerState = timerState,
                onCancelRestTimer = onCancelRestTimer,
            )
        },
        actions = {
            ActionsMenu(
                topAppBarState = state,
                maxVisibleItems = 3,
                onSetControlVisibility = onSetControlVisibility,
                onMutateControlValue = onMutateControlValue,
            )
        },
    )
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
    timerState: CountdownTimerState,
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
                running = timerState.running,
                progress = timerState.progress,
                timeRemaining = timerState.timeRemaining,
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