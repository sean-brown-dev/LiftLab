package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.ReorderableListItem
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.persistence.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutInProgressRepository
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
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val restTimerInProgressRepository: RestTimerInProgressRepository,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {
    private var _state = MutableStateFlow(LabState())
    val state = _state.asStateFlow()

    init {
        _state.update {
            it.copy(isReordering = false, isDeletingProgram = false)
        }

        viewModelScope.launch {
            programsRepository.getActive().observeForever { activeProgram ->
                _state.update {
                    it.copy(
                        program = activeProgram
                    )
                }
            }
        }
    }

    @Subscribe
    fun handleTopAppBarActionEvent(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.ReorderWorkouts,
            TopAppBarAction.NavigatedBack -> toggleReorderingScreen()
            TopAppBarAction.RenameProgram -> showEditProgramNameModal()
            TopAppBarAction.DeleteProgram -> beginDeleteProgram()
            TopAppBarAction.CreateNewWorkout -> createNewWorkout()
            TopAppBarAction.CreateNewProgram -> toggleCreateProgramModal()
            TopAppBarAction.EditDeloadWeek -> toggleEditDeloadWeek()
            else -> { }
        }
    }

    fun toggleEditDeloadWeek() {
        _state.update {
            it.copy(isEditingDeloadWeek = !_state.value.isEditingDeloadWeek)
        }
    }

    fun updateDeloadWeek(deloadWeek: Int) {
        executeInTransactionScope {
            programsRepository.updateDeloadWeek(_state.value.program!!.id, deloadWeek)
            _state.value.program!!.workouts.fastForEach { workout ->
                workoutsRepository.setAllWorkoutLiftDeloadWeeksToNull(workout.id, deloadWeek)
            }
            _state.update {
                it.copy(
                    isEditingDeloadWeek = false,
                    program = _state.value.program!!.copy(deloadWeek = deloadWeek)
                )
            }
        }
    }

    fun toggleCreateProgramModal() {
        _state.update {
            it.copy(isCreatingProgram = !_state.value.isCreatingProgram)
        }
    }

    fun createProgram(name: String) {
        executeInTransactionScope {
            val newProgram = ProgramDto(name = name)
            if (_state.value.program != null) {
                val programToArchive = _state.value.program!!.copy(isActive = false)
                programsRepository.update(programToArchive)
            }
            val newProgramId = programsRepository.insert(newProgram)
            _state.update {
                it.copy(program = newProgram.copy(id = newProgramId), isCreatingProgram = false)
            }
        }
    }

    private fun createNewWorkout() {
        executeInTransactionScope {
            val newWorkout = WorkoutDto(
                programId = _state.value.program!!.id,
                name = "New Workout",
                position = _state.value.program!!.workouts.count(),
                lifts = listOf()
            )
            val newWorkoutId = workoutsRepository.insert(newWorkout)
            _state.update { currentState ->
                currentState.copy(
                    workoutIdToRename = newWorkoutId,
                    originalWorkoutName = newWorkout.name,
                    program = currentState.program!!.copy(
                        workouts = currentState.program.workouts.toMutableList().apply {
                            add(
                                newWorkout.copy(
                                    id = newWorkoutId
                                )
                            )
                        }
                    ),
                )
            }
        }
    }
    
    fun showEditWorkoutNameModal(workoutIdToRename: Long, originalWorkoutName: String) {
        _state.update {
            it.copy(workoutIdToRename = workoutIdToRename, originalWorkoutName = originalWorkoutName)
        }
    }

    fun collapseEditWorkoutNameModal() {
        if (_state.value.originalWorkoutName != null) {
            _state.update {
                it.copy(workoutIdToRename = null, originalWorkoutName = null)
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
            executeInTransactionScope {
                workoutsRepository.updateName(
                    id = workoutId,
                    newName = newName
                )
                collapseEditWorkoutNameModal()
                _state.update { currentState ->
                    currentState.copy(
                        program = currentState.program!!.copy(
                            workouts = currentState.program.workouts.fastMap { workout ->
                                if(workout.id == workoutId) workout.copy(name = newName)
                                else workout
                            }
                        ),
                        workoutIdToRename = null,
                    )
                }
            }
        } else collapseEditWorkoutNameModal()
    }

    fun updateProgramName(newName: String) {
        val program = _state.value.program
        if (program != null && _state.value.originalProgramName != newName) {
            executeInTransactionScope {
                Log.d(Log.DEBUG.toString(), program.id.toString())
                Log.d(Log.DEBUG.toString(), newName)
                programsRepository.updateName(
                    id = program.id,
                    newName = newName
                )
                _state.update {
                    it.copy(program = program.copy(name = newName), isEditingProgramName = false)
                }
            }
        }
    }

    fun deleteWorkout(workout: WorkoutDto) {
        viewModelScope.launch {
            workoutsRepository.delete(workout)
            _state.update {
                _state.value.copy(
                    program = _state.value.program!!.copy(
                        workouts = _state.value.program!!.workouts.filter {
                            it.id != workout.id
                        }
                    ),
                    workoutToDelete = null,
                )
            }
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
            executeInTransactionScope {
                programsRepository.delete(program)
                workoutInProgressRepository.delete()
                restTimerInProgressRepository.deleteAll()

                _state.update {
                    LabState()
                }
            }
        }
    }

    fun cancelDeleteProgram() {
        _state.update {
            it.copy(isDeletingProgram = false)
        }
    }

    fun saveReorder(newOrder: List<ReorderableListItem>) {
        executeInTransactionScope {
            val reorderedWorkouts = newOrder.mapIndexed { index, reorderableListItem ->
                val workout = _state.value.program!!.workouts.find { workout -> workout.id == reorderableListItem.key }
                workout!!.copy(position = index)
            }
            workoutsRepository.updateMany(reorderedWorkouts)
            _state.update { currentState ->
                currentState.copy(
                    program = currentState.program!!.copy(
                        workouts = reorderedWorkouts
                    ),
                    isReordering = false
                )
            }
        }
    }

    fun toggleReorderingScreen() {
        viewModelScope.launch {
            _state.update {
                it.copy(isReordering = !it.isReordering)
            }
        }
    }
}