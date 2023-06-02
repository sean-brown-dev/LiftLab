package com.browntowndev.liftlab.ui.viewmodels.states.topAppBar

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import com.browntowndev.liftlab.ui.models.ActionMenuItem

@Stable
data class LiftLabTopAppBarState(
    val currentScreen: Screen? = null
) {
    val isOverflowMenuExpanded: Boolean
        get() = currentScreen?.isOverflowMenuExpanded == true

    val isVisible: Boolean
        get() = currentScreen?.isAppBarVisible == true

    val navigationIcon: ImageVector?
        get() = currentScreen?.navigationIcon

    val navigationIconVisible: Boolean?
        get() = currentScreen?.navigationIconVisible

    val navigationIconContentDescription: String?
        get() = currentScreen?.navigationIconContentDescription

    val onNavigationIconClick: (() -> Unit)?
        get() = currentScreen?.onNavigationIconClick

    val title: String
        get() = currentScreen?.title.orEmpty()

    val actions: List<ActionMenuItem>
        get() = currentScreen?.actions ?: listOf()
}