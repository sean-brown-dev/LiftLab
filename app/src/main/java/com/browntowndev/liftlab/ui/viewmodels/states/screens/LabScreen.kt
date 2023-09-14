package com.browntowndev.liftlab.ui.viewmodels.states.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import org.greenrobot.eventbus.EventBus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class LabScreen(
    override val isOverflowMenuExpanded: Boolean = false,
    override val isOverflowMenuIconVisible: Boolean = false,
    override val navigationIconVisible: Boolean = false,
    override val title: String = navigation.title,
    override val subtitle: String = navigation.subtitle,
    val trailingIconText: String = "",
    val renameProgramVisible: Boolean = true,
    val reorderWorkoutsVisible: Boolean = true,
    val deleteProgramVisible: Boolean = true,
    val deloadWeekVisible: Boolean = true,
    val createWorkoutVisible: Boolean = true,
) : BaseScreen(), KoinComponent {
    companion object {
        val navigation = BottomNavItem("Lab", "", R.drawable.lab_flask, "lab")

        const val REORDER_WORKOUTS_ICON = "reorderWorkoutsIcon"
        const val RENAME_PROGRAM_ICON = "renameProgramIcon"
        const val CREATE_NEW_PROGRAM_ICON = "createNewProgram"
        const val DELETE_PROGRAM_ICON = "deleteProgramIcon"
        const val CREATE_NEW_WORKOUT_ICON = "createNewWorkoutIcon"
        const val DELOAD_WEEK_ICON = "deloadWeekIcon"
    }

    private val _eventBus: EventBus by inject()
    private var _reorderWorkoutsTrailingText by mutableStateOf(trailingIconText)

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
        return if (newSubtitle != this.subtitle) copy(subtitle = newSubtitle, trailingIconText = _reorderWorkoutsTrailingText) else this
    }

    override fun <T> mutateControlValue(request: AppBarMutateControlRequest<T>): Screen {
        return when (request.controlName) {
            DELOAD_WEEK_ICON -> {
                _reorderWorkoutsTrailingText = (request.payload as String)
                this
            }
            else -> super.mutateControlValue(request)
        }
    }

    override fun setControlVisibility(controlName: String, isVisible: Boolean): Screen {
        return when (controlName) {
            RENAME_PROGRAM_ICON -> {
              copy(renameProgramVisible = isVisible)
            }
            DELETE_PROGRAM_ICON -> {
                copy(deleteProgramVisible = isVisible)
            }
            REORDER_WORKOUTS_ICON -> {
                copy(reorderWorkoutsVisible = isVisible)
            }
            CREATE_NEW_WORKOUT_ICON -> {
                copy(createWorkoutVisible = isVisible)
            }
            DELOAD_WEEK_ICON -> {
                copy(deloadWeekVisible = isVisible)
            }
            else -> super.setControlVisibility(controlName, isVisible)
        }
    }

    override val route: String
        get() = navigation.route
    override val isAppBarVisible: Boolean
        get() = true
    override val navigationIcon: Either<ImageVector, Int>?
        get() = Icons.Filled.ArrowBack.left()
    override val navigationIconContentDescription: String?
        get() = null
    override val onNavigationIconClick: (() -> Unit)
        get() = { _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.NavigatedBack)) }
    override val actions: List<ActionMenuItem> by derivedStateOf {
        listOf(
            ActionMenuItem.IconMenuItem.NeverShown(
                controlName = CREATE_NEW_WORKOUT_ICON,
                title = "Create Workout",
                icon = Icons.Filled.Add.left(),
                isVisible = createWorkoutVisible,
                dividerBelow = !reorderWorkoutsVisible,
                onClick = { _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.CreateNewWorkout)) },
            ),
            ActionMenuItem.IconMenuItem.NeverShown(
                controlName = REORDER_WORKOUTS_ICON,
                title = "Reorder Workouts",
                icon = R.drawable.reorder_icon.right(),
                isVisible = reorderWorkoutsVisible,
                onClick = {
                    _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.ReorderWorkouts))
                },
                dividerBelow = true,
            ),
            ActionMenuItem.IconMenuItem.NeverShown(
                controlName = CREATE_NEW_PROGRAM_ICON,
                title = "Create Program",
                icon = Icons.Filled.Add.left(),
                isVisible = true,
                onClick = { _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.CreateNewProgram)) },
            ),
            ActionMenuItem.IconMenuItem.NeverShown(
                controlName = RENAME_PROGRAM_ICON,
                title = "Rename Program",
                icon = Icons.Filled.Edit.left(),
                isVisible = renameProgramVisible,
                onClick = { _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.RenameProgram)) },
            ),
            ActionMenuItem.IconMenuItem.NeverShown(
                controlName = DELETE_PROGRAM_ICON,
                title = "Delete Program",
                icon = Icons.Filled.Delete.left(),
                isVisible = deleteProgramVisible,
                dividerBelow = true,
                onClick = { _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.DeleteProgram)) },
            ),
            ActionMenuItem.IconMenuItem.NeverShown(
                controlName = DELOAD_WEEK_ICON,
                title = "Deload Week",
                icon = Icons.Outlined.DateRange.left(),
                trailingIconText = _reorderWorkoutsTrailingText,
                isVisible = deloadWeekVisible,
                onClick = { _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.EditDeloadWeek)) },
            ),
        )
    }
}