package com.browntowndev.liftlab.ui.viewmodels.appBar

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import com.browntowndev.liftlab.ui.models.controls.ActionMenuItem
import com.browntowndev.liftlab.ui.viewmodels.appBar.screen.Screen

@Stable
data class LiftLabTopAppBarState(
    val currentScreen: Screen? = null,
    val isCollapsed: Boolean = false,
    val timeStartedInMillis: Long? = null,
    val totalRestTime: Long? = null,
) {
    val isOverflowMenuIconVisible: Boolean
        get() = currentScreen?.isOverflowMenuIconVisible == true

    val isOverflowMenuExpanded: Boolean
        get() = currentScreen?.isOverflowMenuExpanded == true

    val isVisible: Boolean
        get() = currentScreen?.isAppBarVisible == true

    val navigationIconImageVector: ImageVector?
        get() = currentScreen?.navigationIcon?.leftOrNull()

    val navigationIconResourceId: Int?
        get() = currentScreen?.navigationIcon?.getOrNull()

    val navigationIconVisible: Boolean?
        get() = currentScreen?.navigationIconVisible

    val navigationIconContentDescription: String?
        get() = currentScreen?.navigationIconContentDescription

    val onNavigationIconClick: (() -> List<Pair<String, Boolean>>)?
        get() = currentScreen?.onNavigationIconClick

    val title: String
        get() = currentScreen?.title.orEmpty()

    val subtitle: String
        get() = currentScreen?.subtitle.orEmpty()

    val actions: List<ActionMenuItem>
        get() = currentScreen?.actions ?: listOf()

    val screenHasRestTimer: Boolean
        get() = currentScreen?.actions?.filterIsInstance<ActionMenuItem.TimerMenuItem>()?.isNotEmpty() == true
}