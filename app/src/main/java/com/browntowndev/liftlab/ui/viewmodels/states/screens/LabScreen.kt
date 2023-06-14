package com.browntowndev.liftlab.ui.viewmodels.states.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.ui.models.ActionMenuItem
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
) : BaseScreen(), KoinComponent {
    companion object {
        val navigation = BottomNavItem("Lab", "", R.drawable.lab_flask, "lab")

        const val REORDER_WORKOUTS_ICON = "reorderWorkoutsIcon"
        const val RENAME_PROGRAM_ICON = "renameProgramIcon"
        const val CREATE_NEW_PROGRAM_ICON = "createNewProgram"
        const val DELETE_PROGRAM_ICON = "deleteProgramIcon"
        const val CREATE_NEW_WORKOUT_ICON = "createNewWorkoutIcon"
    }

    private val _eventBus: EventBus by inject()

    override fun copySetOverflowIconVisibility(isVisible: Boolean): Screen {
        return if (isVisible != this.isOverflowMenuIconVisible) copy(isOverflowMenuIconVisible = isVisible) else this
    }

    override fun copySetOverflowMenuVisibility(isVisible: Boolean): Screen {
        return if (isVisible != this.isOverflowMenuExpanded) copy(isOverflowMenuExpanded = isVisible) else this
    }

    override fun copySetNavigationIconVisibility(isVisible: Boolean): Screen {
        return if (isVisible != this.navigationIconVisible) copy(navigationIconVisible = isVisible) else this
    }

    override fun copyTitleMutation(newTitle: String): Screen {
        return if (newTitle != this.title) copy(title = newTitle) else this
    }

    override fun copySubtitleMutation(newSubtitle: String): Screen {
        return if (newSubtitle != this.subtitle) copy(subtitle = newSubtitle) else this
    }

    override val route: String
        get() = navigation.route
    override val isAppBarVisible: Boolean
        get() = true
    override val navigationIcon: ImageVector?
        get() = Icons.Filled.ArrowBack
    override val navigationIconContentDescription: String?
        get() = null
    override val onNavigationIconClick: (() -> Unit)?
        get() = { _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.NavigatedBack)) }
    override val actions: List<ActionMenuItem> by derivedStateOf {
        listOf(
            ActionMenuItem.IconMenuItem.NeverShown(
                controlName = CREATE_NEW_WORKOUT_ICON,
                title = "Add Workout",
                icon = Icons.Filled.Add,
                isVisible = true,
                onClick = { _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.CreateNewWorkout)) },
            ),
            ActionMenuItem.IconMenuItem.NeverShown(
                controlName = REORDER_WORKOUTS_ICON,
                title = "Reorder Workouts",
                icon = Icons.Filled.Refresh,
                isVisible = true,
                onClick = {
                    _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.ReorderWorkouts))
                },
                dividerBelow = true,
            ),
            ActionMenuItem.IconMenuItem.NeverShown(
                controlName = CREATE_NEW_PROGRAM_ICON,
                title = "Create Program",
                icon = Icons.Filled.Add,
                isVisible = true,
                onClick = { _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.CreateNewProgram)) },
            ),
            ActionMenuItem.IconMenuItem.NeverShown(
                controlName = RENAME_PROGRAM_ICON,
                title = "Rename Program",
                icon = Icons.Filled.Edit,
                isVisible = true,
                onClick = { _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.RenameProgram)) },
            ),
            ActionMenuItem.IconMenuItem.NeverShown(
                controlName = DELETE_PROGRAM_ICON,
                title = "Delete Program",
                icon = Icons.Filled.Delete,
                isVisible = true,
                onClick = { _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.DeleteProgram)) },
            ),
        )
    }
}