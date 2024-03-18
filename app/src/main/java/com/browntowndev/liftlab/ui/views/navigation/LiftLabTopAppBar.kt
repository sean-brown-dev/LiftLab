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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.ui.composables.ProgressCountdownTimer
import com.browntowndev.liftlab.ui.models.ActionMenuItem
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.TopAppBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.LiftLabTopAppBarState
import org.greenrobot.eventbus.EventBus
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLabTopAppBar(
    state: LiftLabTopAppBarState,
    modifier: Modifier = Modifier,
    topAppBarViewModel: TopAppBarViewModel = koinViewModel(),
    scrollBehavior: TopAppBarScrollBehavior,
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
            topAppBarViewModel = topAppBarViewModel,
            state = state,
        )
    }
    else {
        LiftLabLargeTopAppBar(
            modifier = modifier.alpha(1f - appBarAlpha),
            scrollBehavior = scrollBehavior,
            topAppBarViewModel = topAppBarViewModel,
            state = state,
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
    modifier: Modifier = Modifier,
    topAppBarViewModel: TopAppBarViewModel = koinViewModel(),
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
) {
    var isCollapsed by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = scrollBehavior.state.collapsedFraction) {
        isCollapsed = scrollBehavior.state.collapsedFraction == 1.0f
    }

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
                topAppBarViewModel = topAppBarViewModel,
                state = state,
                titleFontSize = 32.sp,
                subtitleFontSize = 18.sp,
            )
        },
        actions = {
            ActionsMenu(
                topAppBarState = state,
                topAppBarViewModel = topAppBarViewModel,
                maxVisibleItems = 3,
            )
        },
        scrollBehavior = scrollBehavior
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiftLabSmallTopAppBar(
    state: LiftLabTopAppBarState,
    modifier: Modifier = Modifier,
    topAppBarViewModel: TopAppBarViewModel = koinViewModel(),
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior { false },
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
                topAppBarViewModel = topAppBarViewModel,
                state = state,
            )
        },
        actions = {
            ActionsMenu(
                topAppBarState = state,
                topAppBarViewModel = topAppBarViewModel,
                maxVisibleItems = 3,
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
    topAppBarViewModel: TopAppBarViewModel,
    state: LiftLabTopAppBarState,
    titleFontSize: TextUnit = 25.sp,
    subtitleFontSize: TextUnit = 14.sp,
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
    } else {
        val context = LocalContext.current
        val restTimerAction = state.actions
            .filterIsInstance<ActionMenuItem.TimerMenuItem.AlwaysShown>()
            .firstOrNull { it.isVisible }

        if (restTimerAction != null) {
            Box (
                contentAlignment = Alignment.CenterStart,
            ) {
                ProgressCountdownTimer(
                    timerRequestId = restTimerAction.timerRequestId,
                    start = restTimerAction.started,
                    countDownStartedFrom = restTimerAction.countDownStartedFrom,
                    countDownFrom = restTimerAction.countDownFrom
                ) { ranToCompletion ->
                    topAppBarViewModel.mutateControlValue(
                        AppBarMutateControlRequest(
                            restTimerAction.controlName,
                            Triple(0L, 0L, false)
                        )
                    )
                    EventBus.getDefault().post(TopAppBarEvent.ActionEvent(TopAppBarAction.RestTimerCompleted))

                    if (ranToCompletion) {
                        topAppBarViewModel.playRestTimerCompletionSound(context)
                    }
                }
                Icon(
                    modifier = Modifier.size(25.dp),
                    painter = painterResource(id = R.drawable.stopwatch_icon),
                    contentDescription = null,
                    tint = if (restTimerAction.started) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}