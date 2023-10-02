package com.browntowndev.liftlab.ui.viewmodels.states.screens

import androidx.compose.ui.graphics.vector.ImageVector
import arrow.core.Either
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.ui.models.ActionMenuItem
import com.browntowndev.liftlab.ui.models.BottomNavItem

data class WorkoutHistoryScreen(
    override val isOverflowMenuExpanded: Boolean = false,
    override val isOverflowMenuIconVisible: Boolean = false,
    override val navigationIconVisible: Boolean = false,
    override val title: String = navigation.title
) : BaseScreen() {
    companion object {
        val navigation = BottomNavItem("History", "", R.drawable.history_icon,"workoutHistory")
    }

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
    override val navigationIcon: Either<ImageVector, Int>?
        get() = null
    override val navigationIconContentDescription: String?
        get() = null
    override val onNavigationIconClick: (() -> Unit)?
        get() = null
    override val actions: MutableList<ActionMenuItem>
        get() = mutableListOf()
}