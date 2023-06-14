package com.browntowndev.liftlab.ui.viewmodels.states.screens

import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest

abstract class BaseScreen(override val subtitle: String = ""): Screen {
    abstract fun copySetOverflowIconVisibility(isVisible: Boolean): Screen
    abstract fun copySetOverflowMenuVisibility(isVisible: Boolean): Screen
    abstract fun copySetNavigationIconVisibility(isVisible: Boolean): Screen
    abstract fun copyTitleMutation(newTitle: String): Screen
    open fun copySubtitleMutation(newSubtitle: String): Screen { return this }

    override fun setControlVisibility(controlName: String, isVisible: Boolean): Screen {
        return when (controlName) {
            Screen.OVERFLOW_MENU -> copySetOverflowMenuVisibility(isVisible = isVisible)
            Screen.OVERFLOW_MENU_ICON -> copySetOverflowIconVisibility(isVisible = isVisible)
            Screen.NAVIGATION_ICON -> copySetNavigationIconVisibility(isVisible = isVisible)
            else -> { this }
        }
    }

    override fun <T> mutateControlValue(request: AppBarMutateControlRequest<T>): Screen {
        return when (request.controlName) {
            Screen.TITLE -> copyTitleMutation(request.payload as String)
            Screen.SUBTITLE -> copySubtitleMutation(request.payload as String)
            else -> { this  }
        }
    }
}