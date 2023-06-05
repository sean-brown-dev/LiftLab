package com.browntowndev.liftlab.ui.viewmodels.states.topAppBar

import androidx.compose.ui.graphics.vector.ImageVector
import com.browntowndev.liftlab.ui.models.ActionMenuItem
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest

sealed interface Screen {
    val route: String
    val isOverflowMenuExpanded: Boolean
    val isAppBarVisible: Boolean
    val navigationIcon: ImageVector?
    val navigationIconContentDescription: String?
    val navigationIconVisible: Boolean
    val onNavigationIconClick: (() -> Unit)?
    val title: String
    val subtitle: String
    val actions: List<ActionMenuItem>
    fun toggleControlVisibility(controlName: String): Screen
    fun <T> mutateControlValue(request: AppBarMutateControlRequest<T>): Screen

    companion object {
        const val OVERFLOW_MENU = "overflowMenuIcon"
        const val NAVIGATION_ICON = "navigationIcon"

        enum class AppBarActions {
            OnNavigatedBack
        }
    }
}