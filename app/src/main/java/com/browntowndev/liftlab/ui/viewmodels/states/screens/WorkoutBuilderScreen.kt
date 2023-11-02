package com.browntowndev.liftlab.ui.viewmodels.states.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.graphics.vector.ImageVector
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.ui.models.ActionMenuItem
import com.browntowndev.liftlab.ui.models.NavItem
import org.greenrobot.eventbus.EventBus
import org.koin.core.component.inject

data class WorkoutBuilderScreen(
    override val isOverflowMenuExpanded: Boolean = false,
    override val isOverflowMenuIconVisible: Boolean = false,
    override val navigationIconVisible: Boolean = false,
    override val title: String = navigation.title,
    override val subtitle: String = navigation.subtitle,
) : BaseScreen() {
    companion object {
        val navigation = NavItem("", "", "workoutBuilder/{id}")

        const val RENAME_WORKOUT_ICON = "renameWorkoutIcon"
        const val REORDER_LIFTS = "reorderLifts"
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

    override val route: String
        get() = navigation.route
    override val isAppBarVisible: Boolean
        get() = true
    override val navigationIcon: Either<ImageVector, Int>
        get() = Icons.Filled.ArrowBack.left()
    override val navigationIconContentDescription: String?
        get() = null
    override val onNavigationIconClick: (() -> Unit)?
        get() = { _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.NavigatedBack)) }
    override val actions: List<ActionMenuItem>
        get() = listOf(
            ActionMenuItem.IconMenuItem.NeverShown(
                controlName = RENAME_WORKOUT_ICON,
                title = "Rename Workout",
                icon = Icons.Filled.Edit.left(),
                isVisible = true,
                onClick = { _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.RenameWorkout)) },
            ),
            ActionMenuItem.IconMenuItem.NeverShown(
                controlName = REORDER_LIFTS,
                title = "Reorder Lifts",
                icon = R.drawable.reorder_icon.right(),
                isVisible = true,
                onClick = { _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.ReorderLifts)) },
            ),
        )
}