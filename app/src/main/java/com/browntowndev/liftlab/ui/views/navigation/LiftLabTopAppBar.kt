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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLabTopAppBar(
    state: LiftLabTopAppBarState,
    timerState: CountdownTimerState,
    allowCollapse: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    onCancelRestTimer: () -> Unit,
    onSetControlVisibility: (String, Boolean) -> Unit,
    onMutateControlValue: (AppBarMutateControlRequest<String>) -> Unit
) {
    LiftLabLargeTopAppBar(
        scrollBehavior = scrollBehavior,
        state = state,
        allowCollapse = allowCollapse,
        timerState = timerState,
        onCancelRestTimer = onCancelRestTimer,
        onSetControlVisibility = onSetControlVisibility,
        onMutateControlValue = onMutateControlValue,
    )

    BackHandler(state.navigationIconVisible == true) {
        state.onNavigationIconClick?.invoke()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiftLabLargeTopAppBar(
    state: LiftLabTopAppBarState,
    timerState: CountdownTimerState,
    allowCollapse: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    onCancelRestTimer: () -> Unit,
    onSetControlVisibility: (String, Boolean) -> Unit,
    onMutateControlValue: (AppBarMutateControlRequest<String>) -> Unit
) {
    // Tiny non-zero range when not collapsible so Title renders
    val expandedHeight  = if (allowCollapse)
        TopAppBarDefaults.LargeAppBarExpandedHeight
    else
        TopAppBarDefaults.LargeAppBarCollapsedHeight + 1.dp

    // After first measure, pin to fully-collapsed when not collapsible
    LaunchedEffect(allowCollapse) {
        val scrollBehaviorState = scrollBehavior.state
        if (!allowCollapse) {
            awaitFrame()                 // wait for heightOffsetLimit to be set
            scrollBehaviorState.heightOffset = scrollBehaviorState.heightOffsetLimit   // -> collapsedFraction == 1f
            scrollBehaviorState.contentOffset = 0f
        } else {
            scrollBehaviorState.heightOffset = 0f
            scrollBehaviorState.contentOffset = 0f
        }
    }

    // Clamp to avoid measurement errors when switching collapsible/non-collapsible
    LaunchedEffect(scrollBehavior) {
        val scrollBehaviorState = scrollBehavior.state
        snapshotFlow { scrollBehaviorState.heightOffsetLimit }.collectLatest { limit ->
            val clamped = scrollBehaviorState.heightOffset.coerceIn(limit, 0f)
            if (clamped != scrollBehaviorState.heightOffset) scrollBehaviorState.heightOffset = clamped
        }
    }

    LargeTopAppBar(
        expandedHeight = expandedHeight,
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
                titleFontSize = if (allowCollapse) 32.sp else 25.sp,
                subtitleFontSize = if (allowCollapse) 18.sp else 14.sp,
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