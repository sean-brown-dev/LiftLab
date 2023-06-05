package com.browntowndev.liftlab.ui.viewmodels.states.topAppBar

import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest

abstract class BaseScreen(override val subtitle: String = ""): Screen {
    abstract fun copyOverflowMenuToggle(): Screen
    abstract fun copyNavigationIconToggle(): Screen
    open fun toggleVisibility(controlName: String): Screen { return this }

    override fun toggleControlVisibility(controlName: String): Screen {
        return when (controlName) {
            Screen.OVERFLOW_MENU -> copyOverflowMenuToggle()
            Screen.NAVIGATION_ICON -> copyNavigationIconToggle()
            else -> toggleVisibility(controlName)
        }
    }

    override fun <T> mutateControlValue(request: AppBarMutateControlRequest<T>): Screen {
        return this
    }
}