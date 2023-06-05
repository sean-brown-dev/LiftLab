package com.browntowndev.liftlab.ui.viewmodels.states.topAppBar

import androidx.compose.ui.graphics.vector.ImageVector
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.ui.models.ActionMenuItem
import com.browntowndev.liftlab.ui.models.NavItem

data class WorkoutHistoryScreen(
    override val isOverflowMenuExpanded: Boolean = false,
    override val navigationIconVisible: Boolean = false,
) : BaseScreen() {
    companion object {
        val navigation = NavItem("History", "", R.drawable.history_icon,"workoutHistory")
    }

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