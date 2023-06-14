package com.browntowndev.liftlab.ui.viewmodels.states.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.graphics.vector.ImageVector
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.ui.models.ActionMenuItem
import com.browntowndev.liftlab.ui.models.NavItem
import org.greenrobot.eventbus.EventBus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class WorkoutBuilderScreen(
    override val isOverflowMenuExpanded: Boolean = false,
    override val isOverflowMenuIconVisible: Boolean = false,
    override val navigationIconVisible: Boolean = false,
    override val title: String = navigation.title,
    override val subtitle: String = navigation.subtitle,
) : BaseScreen(), KoinComponent {
    companion object {
        val navigation = NavItem("Lab", "", "workoutBuilder")

        const val RENAME_WORKOUT_ICON = "renameWorkoutIcon"
    }

    private val eventBus: EventBus by inject()

    override fun copySetOverflowIconVisibility(isVisible: Boolean): Screen {
        return if (isVisible != this.isOverflowMenuIconVisible) copy(isOverflowMenuIconVisible = isVisible) else this
    }

    override fun copySetOverflowMenuVisibility(isVisible: Boolean): Screen {
        return if(isVisible != isOverflowMenuExpanded) copy(isOverflowMenuExpanded = !this.isOverflowMenuExpanded) else this
    }

    override fun copySetNavigationIconVisibility(isVisible: Boolean): Screen {
        return if (isVisible != navigationIconVisible) copy(navigationIconVisible = !this.navigationIconVisible) else this
    }

    override fun copyTitleMutation(newTitle: String): Screen {
        return if (newTitle != title) copy(title = newTitle) else this
    }

    override fun copySubtitleMutation(newSubtitle: String): Screen {
        return if (newSubtitle != subtitle) copy(subtitle = newSubtitle) else this
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
        get() = { eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.NavigatedBack)) }
    override val actions: List<ActionMenuItem>
        get() = listOf(
            ActionMenuItem.IconMenuItem.NeverShown(
                controlName = RENAME_WORKOUT_ICON,
                title = "Rename Workout",
                icon = Icons.Filled.Edit,
                isVisible = true,
                onClick = { eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.RenameWorkout)) },
            ),
        )
}