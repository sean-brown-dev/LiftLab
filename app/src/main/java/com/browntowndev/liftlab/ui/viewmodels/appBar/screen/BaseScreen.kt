package com.browntowndev.liftlab.ui.viewmodels.appBar.screen

import com.browntowndev.liftlab.ui.models.controls.AppBarMutateControlRequest

abstract class BaseScreen(override val subtitle: String = ""): Screen {
    abstract fun copySetOverflowIconVisibility(isVisible: Boolean): Screen
    abstract fun copySetOverflowMenuVisibility(isVisible: Boolean): Screen
    abstract fun copySetNavigationIconVisibility(isVisible: Boolean): Screen
    abstract fun copyTitleMutation(newTitle: String): Screen
    open fun copySubtitleMutation(newSubtitle: String): Screen { return this }

    override fun setControlVisibility(controlName: String, isVisible: Boolean): Screen {
        return when (controlName) {
            Screen.Companion.OVERFLOW_MENU -> copySetOverflowMenuVisibility(isVisible = isVisible)
            Screen.Companion.OVERFLOW_MENU_ICON -> copySetOverflowIconVisibility(isVisible = isVisible)
            Screen.Companion.NAVIGATION_ICON -> copySetNavigationIconVisibility(isVisible = isVisible)
            else -> { this }
        }
    }

    override fun <T> mutateControlValue(request: AppBarMutateControlRequest<T>): Screen {
        return when (request.controlName) {
            Screen.Companion.TITLE -> copyTitleMutation(request.payload as String)
            Screen.Companion.SUBTITLE -> copySubtitleMutation(request.payload as String)
            else -> { this  }
        }
    }
}