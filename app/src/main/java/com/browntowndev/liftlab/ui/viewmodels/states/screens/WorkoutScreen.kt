package com.browntowndev.liftlab.ui.viewmodels.states.screens

import androidx.compose.ui.graphics.vector.ImageVector
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.ui.models.ActionMenuItem
import com.browntowndev.liftlab.ui.models.BottomNavItem

data class WorkoutScreen(
    override val isOverflowMenuExpanded: Boolean = false,
    override val isOverflowMenuIconVisible: Boolean = false,
    override val navigationIconVisible: Boolean = false,
    override val title: String = navigation.title,
) : BaseScreen() {
    companion object {
        val navigation = BottomNavItem("Workout", "", R.drawable.dumbbell_icon, "workout")
    }

    override fun copySetOverflowIconVisibility(isVisible: Boolean): Screen {
        return if (isVisible != this.isOverflowMenuIconVisible) copy(isOverflowMenuIconVisible = isVisible) else this
    }

    override fun copySetOverflowMenuVisibility(isVisible: Boolean): Screen {
        return copy(isOverflowMenuExpanded = !this.isOverflowMenuExpanded)
    }

    override fun copySetNavigationIconVisibility(isVisible: Boolean): Screen {
        return copy(navigationIconVisible = !this.navigationIconVisible)
    }

    override fun copyTitleMutation(newTitle: String): Screen {
        return copy(title = newTitle)
    }

    override val route: String
        get() = navigation.route
    override val isAppBarVisible: Boolean
        get() = true
    override val navigationIcon: ImageVector?
        get() = null
    override val navigationIconContentDescription: String?
        get() = null
    override val onNavigationIconClick: (() -> Unit)?
        get() = null
    override val actions: MutableList<ActionMenuItem>
        get() = mutableListOf()
}