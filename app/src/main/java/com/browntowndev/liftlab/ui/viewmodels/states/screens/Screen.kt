package com.browntowndev.liftlab.ui.viewmodels.states.screens

import androidx.compose.ui.graphics.vector.ImageVector
import arrow.core.Either
import com.browntowndev.liftlab.ui.models.ActionMenuItem
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import org.koin.core.component.KoinComponent

sealed interface Screen: KoinComponent {
    val route: String
    val isOverflowMenuExpanded: Boolean
    val isOverflowMenuIconVisible: Boolean
    val isAppBarVisible: Boolean
    val navigationIcon: Either<ImageVector, Int>?
    val navigationIconContentDescription: String?
    val navigationIconVisible: Boolean
    val onNavigationIconClick: (() -> List<Pair<String, Boolean>>)?
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