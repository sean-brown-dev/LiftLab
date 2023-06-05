package com.browntowndev.liftlab.ui.viewmodels.states.topAppBar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.ui.models.ActionMenuItem
import com.browntowndev.liftlab.ui.models.NavItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class LabScreen(
    override val isOverflowMenuExpanded: Boolean = false,
    override val navigationIconVisible: Boolean = false,
) : BaseScreen() {
    companion object {
        val navigation = NavItem("Lab", "", R.drawable.lab_flask, "lab")

        const val REORDER_WORKOUTS_ICON = "reorderWorkoutsIcon"
        const val RENAME_PROGRAM_ICON = "renameProgramIcon"
        const val DELETE_PROGRAM_ICON = "deleteProgramIcon"
        const val CREATE_NEW_WORKOUT_ICON = "createNewWorkoutIcon"

        enum class AppBarActions {
            ReorderWorkouts,
            CreateNewWorkout,
            RenameProgram,
            DeleteProgram,
            NavigatedBack,
        }
    }

    private val _simpleActionButtons = MutableSharedFlow<AppBarActions>(extraBufferCapacity = 1)
    val simpleActionButtons: Flow<AppBarActions> = _simpleActionButtons.asSharedFlow()

    override fun copyOverflowMenuToggle(): Screen {
        return copy(isOverflowMenuExpanded = !this.isOverflowMenuExpanded)
    }

    override fun copyNavigationIconToggle(): Screen {
        return copy(navigationIconVisible = !this.navigationIconVisible)
    }

    override val route: String
        get() = navigation.route
    override val title: String
        get() = navigation.title
    override val subtitle: String
        get() = navigation.subtitle
    override val isAppBarVisible: Boolean
        get() = true
    override val navigationIcon: ImageVector?
        get() = Icons.Filled.ArrowBack
    override val navigationIconContentDescription: String?
        get() = null
    override val onNavigationIconClick: (() -> Unit)?
        get() = { _simpleActionButtons.tryEmit(AppBarActions.NavigatedBack) }
    override val actions: List<ActionMenuItem> by derivedStateOf {
        listOf(
            ActionMenuItem.IconMenuItem.NeverShown(
                controlName = REORDER_WORKOUTS_ICON,
                title = "Reorder Workouts",
                icon = Icons.Filled.Refresh,
                isVisible = true,
                onClick = { _simpleActionButtons.tryEmit(AppBarActions.ReorderWorkouts) }
            )
        )
    }
}