package com.browntowndev.liftlab.ui.viewmodels.states.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.browntowndev.liftlab.ui.models.BottomNavItem
import org.greenrobot.eventbus.EventBus
import org.koin.core.component.inject

data class WorkoutHistoryScreen(
    override val isOverflowMenuExpanded: Boolean = false,
    override val isOverflowMenuIconVisible: Boolean = true,
    override val navigationIconVisible: Boolean = true,
    override val title: String = navigation.title,
) : BaseScreen() {
    companion object {
        val navigation = BottomNavItem("Workout History", "", R.drawable.home_icon,"workoutHistory")
        private const val EDIT_DATE_RANGE_ICON = "editDateRangeIcon"
        private const val FILTER_PROGRAM_AND_WORKOUT_ICON = "filterProgramAndWorkoutIcon"
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

    override val route: String
        get() = navigation.route
    override val isAppBarVisible: Boolean
        get() = true
    override val navigationIcon: Either<ImageVector, Int>
        get() = Icons.AutoMirrored.Filled.ArrowBack.left()
    override val navigationIconContentDescription: String?
        get() = null
    override val onNavigationIconClick: (() -> Unit)
        get() = { _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.NavigatedBack)) }
    override val actions: List<ActionMenuItem> by derivedStateOf {
        listOf(
            ActionMenuItem.IconMenuItem.AlwaysShown(
                controlName = FILTER_PROGRAM_AND_WORKOUT_ICON,
                title = "Filter",
                isVisible = true,
                onClick = {
                    _eventBus.post(TopAppBarEvent.ActionEvent(TopAppBarAction.FilterStarted))
                },
                icon = R.drawable.filter_icon.right(),
                contentDescriptionResourceId = R.string.accessibility_filter,
            ),
            ActionMenuItem.IconMenuItem.AlwaysShown (
                title = "View/Edit History",
                isVisible = true,
                controlName = EDIT_DATE_RANGE_ICON,
                icon = R.drawable.edit_calendar.right(),
                onClick = {
                    _eventBus.post(TopAppBarEvent.ActionEvent(action = TopAppBarAction.EditDateRange))
                },
            ),
        )
    }
}