package com.browntowndev.liftlab.ui.viewmodels.states.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import arrow.core.Either
import arrow.core.right
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.ui.models.ActionMenuItem
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.models.BottomNavItem
import org.greenrobot.eventbus.EventBus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

data class WorkoutScreen(
    override val isOverflowMenuExpanded: Boolean = false,
    override val isOverflowMenuIconVisible: Boolean = false,
    override val navigationIconVisible: Boolean = false,
    override val title: String = navigation.title,
    override val subtitle: String = navigation.subtitle,
    private val restTimerControlVisible: Boolean = false,
    private val restTimerRunning: Boolean = false,
    private val restTimerSpanInMillis: Long = 0L,
    private val timerRequestId: String = "",
) : BaseScreen(), KoinComponent {
    companion object {
        val navigation = BottomNavItem("Workout", "", R.drawable.dumbbell_icon, "workout")
        const val REST_TIMER = "restTimer"
        const val FINISH_BUTTON = "finishButton"
    }

    private val _eventBus: EventBus by inject()

    override fun copySetOverflowIconVisibility(isVisible: Boolean): Screen {
        return if (isVisible != this.isOverflowMenuIconVisible) copy(isOverflowMenuIconVisible = isVisible) else this
    }

    override fun copySetOverflowMenuVisibility(isVisible: Boolean): Screen {
        return if (isVisible != this.isOverflowMenuExpanded) copy(isOverflowMenuExpanded = !this.isOverflowMenuExpanded) else this
    }

    override fun copySetNavigationIconVisibility(isVisible: Boolean): Screen {
        return if (isVisible != navigationIconVisible) copy(navigationIconVisible = !this.navigationIconVisible) else this
    }

    override fun copyTitleMutation(newTitle: String): Screen {
        return if (title != newTitle) copy(title = newTitle) else this
    }

    override fun copySubtitleMutation(newSubtitle: String): Screen {
        return if (newSubtitle != subtitle) copy(subtitle = newSubtitle) else this
    }

    override fun <T> mutateControlValue(request: AppBarMutateControlRequest<T>): Screen {
        return when (request.controlName) {
            REST_TIMER -> {
                when (request.payload) {
                    is Boolean -> {
                        copy(
                            restTimerRunning = request.payload
                        )
                    }
                    is Long -> {
                        val restTime: Long = request.payload
                        if (restTime > 0L) {
                            copy(
                                timerRequestId = UUID.randomUUID().toString(),
                                restTimerSpanInMillis = restTime
                            )
                        } else if (restTime == 0L) {
                            copy(
                                restTimerSpanInMillis = restTime
                            )
                        } else this
                    }
                    else -> throw Exception("Invalid type of ${
                        if (request.payload !=  null) {
                            request.payload!!::class.simpleName
                        } else "NULL"
                    }")
                }
            }
            else -> super.mutateControlValue(request)
        }
    }

    override fun setControlVisibility(controlName: String, isVisible: Boolean): Screen {
        return when (controlName) {
            REST_TIMER -> {
                copy(restTimerControlVisible = isVisible)
            }
            else -> super.setControlVisibility(controlName, isVisible)
        }
    }

    override val route: String
        get() = navigation.route
    override val isAppBarVisible: Boolean
        get() = true
    override val navigationIcon: Either<ImageVector, Int>
        get() = R.drawable.down_carrot.right()
    override val navigationIconContentDescription: String?
        get() = null
    override val onNavigationIconClick: (() -> Unit)
        get() = { _eventBus.post(TopAppBarEvent.ActionEvent(action = TopAppBarAction.NavigatedBack)) }
    override val actions: List<ActionMenuItem> by derivedStateOf {
        listOf(
            ActionMenuItem.TimerMenuItem.AlwaysShown(
                isVisible = restTimerControlVisible,
                controlName = REST_TIMER,
                started = restTimerRunning,
                startTimeInMillis = restTimerSpanInMillis,
                timerRequestId = timerRequestId,
                icon = R.drawable.stopwatch_icon.right(),
            ),
            ActionMenuItem.ButtonMenuItem.AlwaysShown(
                isVisible = restTimerControlVisible,
                controlName = FINISH_BUTTON,
                buttonContent = {
                    Text("Finish")
                },
                onClick = {
                    _eventBus.post(TopAppBarEvent.ActionEvent(action = TopAppBarAction.FinishWorkout))
                }
            )
        )
    }
}