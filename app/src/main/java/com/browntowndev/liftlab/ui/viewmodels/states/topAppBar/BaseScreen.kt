package com.browntowndev.liftlab.ui.viewmodels.states.topAppBar

import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest

abstract class BaseScreen: Screen {
    abstract fun copyOverflowMenuToggle(): Screen
    open fun toggleVisibility(controlName: String): Screen { return this }

    override fun toggleControlVisibility(controlName: String): Screen {
        return when (controlName) {
            Screen.OVERFLOW_MENU -> copyOverflowMenuToggle()
            else -> toggleVisibility(controlName)
        }
    }

    override fun <T> mutateControlValue(request: AppBarMutateControlRequest<T>): Screen {
        return this
    }
}