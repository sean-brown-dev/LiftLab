package com.browntowndev.liftlab.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.LiftLabBottomNavBarState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BottomNavBarViewModel: ViewModel() {
    private var _state = MutableStateFlow(LiftLabBottomNavBarState())
    val state = _state.asStateFlow()

    fun hide() {
        if (_state.value.isVisible) {
            _state.update {
                it.copy(isVisible = false)
            }
        }
    }

    fun show() {
        if (!_state.value.isVisible) {
            _state.update {
                it.copy(isVisible = true)
            }
        }
    }
}