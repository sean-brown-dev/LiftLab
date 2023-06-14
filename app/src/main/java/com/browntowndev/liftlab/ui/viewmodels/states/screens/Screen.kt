package com.browntowndev.liftlab.ui.viewmodels.states.screens

import androidx.compose.ui.graphics.vector.ImageVector
import com.browntowndev.liftlab.ui.models.ActionMenuItem
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest

sealed interface Screen {
    val route: String
    val isOverflowMenuExpanded: Boolean
    val isOverflowMenuIconVisible: Boolean
    val isAppBarVisible: Boolean
    val navigationIcon: ImageVector?
    val navigationIconContentDescription: String?
    val navigationIconVisible: Boolean
    val onNavigationIconClick: (() -> Unit)?
    val title: String
    val subtitle: String
    val actions: List<ActionMenuItem>
    fun setControlVisibility(controlName: String, isVisible: Boolean): Screen
    fun <T> mutateControlValue(request: AppBarMutateControlRequest<T>): Screen

    companion object {
        const val OVERFLOW_MENU_ICON = "overflowMenuIcon"
        const val OVERFLOW_MENU = "overflowMenu"
        const val NAVIGATION_ICON = "navigationIcon"
        const val TITLE = "title"
        const val SUBTITLE = "subtitle"
    }
}