package com.browntowndev.liftlab.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.data.dtos.ProgramDto
import com.browntowndev.liftlab.core.data.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.data.repositories.WorkoutsRepository
import com.browntowndev.liftlab.ui.viewmodels.states.LabState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LabViewModel(
    private val programsRepository: ProgramsRepository,
    private val workoutsRepository: WorkoutsRepository
): ViewModel() {
    private val _state = MutableStateFlow(LabState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getPrograms()
        }
    }

    private suspend fun getPrograms() {
        val programs: List<ProgramDto> = programsRepository.getAll()
        _state.update {
            it.copy(
                programs = programs,
                workoutOfEditNameModal = null,
                programOfEditNameModal = null
            )
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

    fun showEditProgramNameModal(program: ProgramDto) {
        _state.update {
            it.copy(programOfEditNameModal = program)
        }
    }

    fun collapseEditProgramNameModal() {
        if (_state.value.programOfEditNameModal != null) {
            _state.update {
                it.copy(programOfEditNameModal = null)
            }
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
                getPrograms()
            }
        }
    }

    fun updateProgramName(newName: String) {
        if (_state.value.programOfEditNameModal != null &&
            _state.value.originalProgramNameOfActiveRename != newName) {
            viewModelScope.launch {
                programsRepository.updateName(
                    id = _state.value.programOfEditNameModal?.id as Long,
                    newName = newName
                )
                collapseEditProgramNameModal()
                getPrograms()
            }
        }
    }

    fun deleteWorkout(workout: ProgramDto.WorkoutDto) {
        viewModelScope.launch {
            workoutsRepository.delete(workout)
            getPrograms()
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

    fun deleteProgram(program: ProgramDto) {
        viewModelScope.launch {
            programsRepository.delete(program)
            getPrograms()
        }
    }

    fun beginDeleteProgram(program: ProgramDto) {
        _state.update {
            it.copy(programToDelete = program)
        }
    }

    fun cancelDeleteProgram() {
        _state.update {
            it.copy(programToDelete = null)
        }
    }
}