package com.browntowndev.liftlab.ui.viewmodels.states.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.ui.models.ActionMenuItem
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.models.BottomNavItem
import com.browntowndev.liftlab.ui.views.navigation.Route
import org.greenrobot.eventbus.EventBus
import org.koin.core.component.inject
import java.util.UUID

data class WorkoutScreen(
    override val isOverflowMenuExpanded: Boolean = false,
    override val isOverflowMenuIconVisible: Boolean = true,
    override val navigationIconVisible: Boolean = false,
    override val title: String = "",
    override val subtitle: String = navigation.subtitle,
    private val backNavigationIconVisible: Boolean = false,
    private val restTimerControlVisible: Boolean = false,
    private val restTimerRunning: Boolean = false,
    private val restTime: Long = 0L,
    private val countDownFrom: Long = 0L,
    private val timerRequestId: String = "",
) : BaseScreen() {
    companion object {
        val navigation = BottomNavItem("Workout", "", R.drawable.dumbbell_icon, Route.Workout())
        const val REST_TIMER = "restTimer"
        const val BACK_NAVIGATION_ICON = "backNavIcon"
        const val FINISH_BUTTON = "finishButton"
        const val WORKOUT_HISTORY_BUTTON = "workoutHistoryButton"
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
                val payload = request.payload as? Triple<*, *, *>
                val restTime = payload?.first as? Long
                val restTimeRemaining = payload?.second as? Long
                val restTimerRunning = payload?.third as? Boolean

                if (restTime == null) throw Exception("restTime not provided or Triple type wrong.")
                if (restTimeRemaining == null) throw Exception("restTimeRemaining not provided or Triple type wrong.")
                if (restTimerRunning == null) throw Exception("restTimerRunning not provided or Triple type wrong.")

                copy(
                    timerRequestId = if (restTimeRemaining > 0L) UUID.randomUUID().toString() else timerRequestId,
                    restTimerRunning = restTimerRunning,
                    restTime = restTime,
                    countDownFrom = restTimeRemaining,
                )
            }
            else -> super.mutateControlValue(request)
        }
    }

    override fun setControlVisibility(controlName: String, isVisible: Boolean): Screen {
        return when (controlName) {
            REST_TIMER -> copy(restTimerControlVisible = isVisible)
            BACK_NAVIGATION_ICON -> copy(backNavigationIconVisible = isVisible)
            else -> super.setControlVisibility(controlName, isVisible)
        }
    }

    override val route: Route
        get() = navigation.route
    override val isAppBarVisible: Boolean
        get() = true
    override val navigationIcon: Either<ImageVector, Int>
        get() =
            if (!backNavigationIconVisible)
                R.drawable.down_carrot.right()
            else
                Icons.AutoMirrored.Filled.ArrowBack.left()
    override val navigationIconContentDescription: String?
        get() = null
    override val onNavigationIconClick: (() -> List<Pair<String, Boolean>>)
        get() = {
            _eventBus.post(TopAppBarEvent.ActionEvent(action = TopAppBarAction.NavigatedBack))
            listOf()
        }
    override val actions: List<ActionMenuItem> by derivedStateOf {
        listOf(
            ActionMenuItem.TimerMenuItem.AlwaysShown(
                isVisible = restTimerControlVisible,
                controlName = REST_TIMER,
                started = restTimerRunning,
                countDownStartedFrom = restTime,
                countDownFrom = countDownFrom,
                timerRequestId = timerRequestId,
                icon = R.drawable.stopwatch_icon.right(),
            ),
            ActionMenuItem.ButtonMenuItem.AlwaysShown(
                isVisible = restTimerControlVisible || backNavigationIconVisible,
                controlName = FINISH_BUTTON,
                buttonContent = {
                    if (backNavigationIconVisible) {
                        Text("Confirm")
                    } else {
                        Text("Finish")
                    }
                },
                onClick = {
                    _eventBus.post(TopAppBarEvent.ActionEvent(action = TopAppBarAction.FinishWorkout))
                }
            ),
            ActionMenuItem.IconMenuItem.AlwaysShown (
                title = "View/Edit History",
                isVisible = !restTimerControlVisible && !backNavigationIconVisible,
                controlName = WORKOUT_HISTORY_BUTTON,
                icon = R.drawable.history_icon.right(),
                onClick = {
                    _eventBus.post(TopAppBarEvent.ActionEvent(action = TopAppBarAction.OpenWorkoutHistory))
                    listOf()
                }
            ),
        )
    }
}