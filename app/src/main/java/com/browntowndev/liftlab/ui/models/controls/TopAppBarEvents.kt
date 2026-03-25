package com.browntowndev.liftlab.ui.models.controls

import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction

sealed interface TopAppBarEvent {
    val action: TopAppBarAction

    data class ActionEvent(override val action: TopAppBarAction) : TopAppBarEvent
    data class PayloadActionEvent<T>(override val action: TopAppBarAction, val payload: T) : TopAppBarEvent
}