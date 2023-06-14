package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutsRepository
import com.browntowndev.liftlab.ui.viewmodels.states.LabState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class LabViewModel(
    private val programsRepository: ProgramsRepository,
    private val workoutsRepository: WorkoutsRepository,
    private val eventBus: EventBus,
): ViewModel() {
    private var _state = MutableStateFlow(LabState())
    val state = _state.asStateFlow()

    init {
        getActiveProgram()
    }

    fun registerEventBus() {
        if (!eventBus.isRegistered(this)) {
            eventBus.register(this)
            Log.d(Log.DEBUG.toString(), "Registered event bus for ${this::class.simpleName}")
        }
    }

    @Subscribe
    fun handleTopAppBarActionEvent(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.ReorderWorkouts,
            TopAppBarAction.NavigatedBack -> toggleReorderingScreen()
            TopAppBarAction.RenameProgram -> showEditProgramNameModal()
            TopAppBarAction.DeleteProgram -> beginDeleteProgram()
            TopAppBarAction.CreateNewWorkout -> { /*TODO*/ }
            else -> { }
        }
    }

    private fun getActiveProgram() {
        viewModelScope.launch {
            val program: ProgramDto? = programsRepository.getActive()
            _state.update {
                it.copy(program = program, isReordering = false, isDeletingProgram = false)
            }
        }
    }

    fun showEditWorkoutNameModal(originalWorkoutName: String) {
        _state.update {
            it.copy(originalWorkoutName = originalWorkoutName)
        }
    }

    fun collapseEditWorkoutNameModal() {
        if (_state.value.originalWorkoutName != null) {
            _state.update {
                it.copy(originalWorkoutName = null)
            }
        }
    }

    private fun showEditProgramNameModal() {
        _state.update {
            it.copy(isEditingProgramName = true)
        }
    }

    fun collapseEditProgramNameModal() {
        _state.update {
            it.copy(isEditingProgramName = false)
        }
    }

    fun updateWorkoutName(workoutId: Long, newName: String) {
        if (_state.value.originalWorkoutName != newName) {
            viewModelScope.launch {
                workoutsRepository.updateName(
                    id = workoutId,
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

    fun deleteWorkout(workout: WorkoutDto) {
        viewModelScope.launch {
            workoutsRepository.delete(workout)
            getActiveProgram()
        }
    }

    fun beginDeleteWorkout(workout: WorkoutDto) {
        _state.update {
            it.copy(workoutToDelete = workout)
        }
    }

    fun cancelDeleteWorkout() {
        _state.update {
            it.copy(workoutToDelete = null)
        }
    }

    private fun beginDeleteProgram() {
        _state.update {
            it.copy(isDeletingProgram = true)
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
            _state.update {
                it.program!!.let { program ->
                    it.copy(
                        program = program.copy(
                            workouts = program.workouts.toMutableList()
                                .apply { add(to, removeAt(from)) }
                                .mapIndexed { index, workoutDto -> workoutDto.copy(position = index) }
                        )
                    )
                }
            }
        }
    }
}