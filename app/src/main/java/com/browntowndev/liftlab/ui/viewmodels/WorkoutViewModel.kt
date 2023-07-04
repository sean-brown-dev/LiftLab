package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkoutViewModel(
    programsRepository: ProgramsRepository,
): ViewModel() {
    private var _state = MutableStateFlow(WorkoutState())
    val state = _state.asStateFlow()

    init {
        Log.d(Log.DEBUG.toString(), "Getting workout state")
        viewModelScope.launch {
            val activeProgram = programsRepository.getActive()
            if (activeProgram != null) {
                _state.update { currentState ->
                    currentState.copy(
                        program = activeProgram,
                        workout = activeProgram.workouts.find { workout ->
                            workout.position == activeProgram.currentMicrocyclePosition
                        }
                    )
                }
            }
        }
    }

    fun setInProgress(inProgress: Boolean) {
        _state.update {
            it.copy(
                inProgress = inProgress,
                workoutLogVisible = inProgress,
            )
        }
    }

    fun setWorkoutLogVisibility(visible: Boolean) {
        _state.update {
            it.copy(
                workoutLogVisible = visible
            )
        }
    }
}