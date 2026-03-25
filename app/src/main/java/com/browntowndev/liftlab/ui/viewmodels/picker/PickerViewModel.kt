package com.browntowndev.liftlab.ui.viewmodels.picker

import androidx.lifecycle.ViewModel
import com.browntowndev.liftlab.ui.viewmodels.workoutBuilder.PickerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PickerViewModel: ViewModel() {
    private var _state = MutableStateFlow(PickerState())
    val state = _state.asStateFlow()

    fun showRpePicker(workoutLiftId: Long, setPosition: Int, currentRpe: Float?, myoRepSetPosition: Int? = null) {
        _state.update {
            it.copy(
                workoutLiftId = workoutLiftId,
                setPosition = setPosition,
                myoRepSetPosition = myoRepSetPosition,
                currentRpe = currentRpe,
                type = PickerType.Rpe,
            )
        }
    }

    fun hideRpePicker() {
        _state.update {
            it.copy(
                workoutLiftId = null,
                setPosition = null,
                myoRepSetPosition = null,
                currentRpe = null,
                type = null,
            )
        }
    }
}