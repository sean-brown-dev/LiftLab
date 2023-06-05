package com.browntowndev.liftlab.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.data.dtos.ProgramDto
import com.browntowndev.liftlab.core.data.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.data.repositories.WorkoutsRepository
import com.browntowndev.liftlab.ui.viewmodels.states.LabState
import com.browntowndev.liftlab.ui.viewmodels.states.topAppBar.LabScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LabViewModel(
    private val programsRepository: ProgramsRepository,
    private val workoutsRepository: WorkoutsRepository
): ViewModel() {
    private var _state = MutableStateFlow(LabState())
    val state = _state.asStateFlow()

    init {
        getActiveProgram()
    }

    fun watchActionBarActions(screen: LabScreen?) {
        viewModelScope.launch {
            screen?.simpleActionButtons?.collect {
                when (it) {
                    LabScreen.Companion.AppBarActions.ReorderWorkouts,
                    LabScreen.Companion.AppBarActions.NavigatedBack -> toggleReorderingScreen()
                    else -> { }
                }
            }
        }
    }

    private fun getActiveProgram() {
        viewModelScope.launch {
            val program: ProgramDto = programsRepository.getActive()
            _state.update {
                it.copy(program = program, isReordering = false)
            }
        }
    }

    fun showEditWorkoutNameModal(workout: ProgramDto.WorkoutDto) {
        _state.update {
            it.copy(workoutOfEditNameModal = workout)
        }
    }

    fun collapseEditWorkoutNameModal() {
        if (_state.value.workoutOfEditNameModal != null) {
            _state.update {
                it.copy(workoutOfEditNameModal = null)
            }
        }
    }

    fun showEditProgramNameModal() {
        _state.update {
            it.copy(isEditingProgramName = true)
        }
    }

    fun collapseEditProgramNameModal() {
        _state.update {
            it.copy(isEditingProgramName = false)
        }
    }

    fun updateWorkoutName(newName: String) {
        if (_state.value.workoutOfEditNameModal != null &&
            _state.value.originalWorkoutNameOfActiveRename != newName) {
            viewModelScope.launch {
                workoutsRepository.updateName(
                    id = _state.value.workoutOfEditNameModal?.id as Long,
                    newName = newName
                )
                collapseEditWorkoutNameModal()
                getActiveProgram()
            }
        }
    }

    fun updateProgramName(newName: String) {
        val program = _state.value.program
        if (program != null && _state.value.originalProgramName != newName) {
            viewModelScope.launch {
                programsRepository.updateName(
                    id = program.id,
                    newName = newName
                )
                collapseEditProgramNameModal()
                getActiveProgram()
            }
        }
    }

    fun deleteWorkout(workout: ProgramDto.WorkoutDto) {
        viewModelScope.launch {
            workoutsRepository.delete(workout)
            getActiveProgram()
        }
    }

    fun beginDeleteWorkout(workout: ProgramDto.WorkoutDto) {
        _state.update {
            it.copy(workoutToDelete = workout)
        }
    }

    fun cancelDeleteWorkout() {
        _state.update {
            it.copy(workoutToDelete = null)
        }
    }

    fun deleteProgram() {
        val program = _state.value.program
        if (program != null) {
            viewModelScope.launch {
                programsRepository.delete(program)
                getActiveProgram()
            }
        }
    }

    fun beginDeleteProgram() {
        _state.update {
            it.copy(isDeletingProgram = true)
        }
    }

    fun cancelDeleteProgram() {
        _state.update {
            it.copy(isDeletingProgram = false)
        }
    }

    fun saveReorder() {
        val program = _state.value.program
        if(program != null) {
            viewModelScope.launch {
                workoutsRepository.updateMany(program.workouts)
                getActiveProgram()
            }
        }
    }

    fun toggleReorderingScreen() {
        if (_state.value.isReordering) {
            viewModelScope.launch {
                getActiveProgram()
            }
        }
        else {
            _state.update {
                it.copy(isReordering = true)
            }
        }
    }

    fun changePosition(to: Int, from: Int) {
        if (to > -1 && from > -1 &&
            to < _state.value.workoutCount && from < _state.value.workoutCount
        ) {
            val program = _state.value.program?.copy()
            program?.workouts = program?.workouts
                ?.toMutableList()?.apply { add(to, removeAt(from)) }
                ?.onEachIndexed { index, workoutDto -> workoutDto.position = index }?.toList() ?: listOf()
            _state.update {
                it.copy(program = program)
            }
        }
    }
}