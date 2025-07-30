package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.ui.models.ReorderableListItem
import com.browntowndev.liftlab.core.common.Utils.StepSize.Companion.getAllLiftsWithRecalculatedStepSize
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.workout.Program
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
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
    private val workoutLiftsRepository: WorkoutLiftsRepository,
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
            programsRepository.getAllFlow()
                .collect { allPrograms ->
                    val activeProgram = allPrograms
                        .firstOrNull { program -> program.isActive }
                        ?.let { program ->
                            program.copy(
                                workouts = program.workouts.sortedBy { it.position }
                            )
                        }
                    _state.update {
                        it.copy(
                            program = activeProgram,
                            allPrograms = allPrograms,
                        )
                    }
                }
        }
    }

    @Subscribe
    fun handleTopAppBarActionEvent(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.CreateNewProgram -> toggleCreateProgramModal()
            TopAppBarAction.CreateNewWorkout -> createNewWorkout()
            TopAppBarAction.DeleteProgram -> beginDeleteProgram(_state.value.program?.id)
            TopAppBarAction.EditDeloadWeek -> toggleEditDeloadWeek()
            TopAppBarAction.RenameProgram -> showEditProgramNameModal()
            TopAppBarAction.ReorderWorkouts -> toggleReorderingScreen()
            TopAppBarAction.ManagePrograms -> toggleManageProgramsScreen()
            TopAppBarAction.NavigatedBack -> toggleOffReorderingAndProgramManagement()
            else -> { }
        }
    }

    fun toggleEditDeloadWeek() {
        _state.update {
            it.copy(isEditingDeloadWeek = !_state.value.isEditingDeloadWeek)
        }
    }

    fun updateDeloadWeek(deloadWeek: Int) = executeWithErrorHandling("Error updating deload week") {
        executeInTransactionScope {
            programsRepository.updateDeloadWeek(_state.value.program!!.id, deloadWeek)
            val liftsWithNewStepSizes: Map<Long, StandardWorkoutLift> = if (_state.value.program != null) {
                getAllLiftsWithRecalculatedStepSize(
                    workouts = _state.value.program!!.workouts,
                    deloadToUseInsteadOfLiftLevel = deloadWeek,
                )
            } else mapOf()

            if (liftsWithNewStepSizes.isNotEmpty()) {
                workoutLiftsRepository.updateMany(liftsWithNewStepSizes.values.toList())
            }
            _state.update {
                it.copy(
                    program = _state.value.program!!.let { program ->
                        program.copy(
                            deloadWeek = deloadWeek,
                            workouts = program.workouts.fastMap { workout ->
                                workout.copy(
                                    lifts = workout.lifts.fastMap { lift ->
                                        if(liftsWithNewStepSizes.containsKey(lift.id)) {
                                            liftsWithNewStepSizes[lift.id]!!
                                        } else lift
                                    }
                                )
                            }
                        )
                    }
                )
            }
        }
    }

    fun toggleCreateProgramModal() {
        _state.update {
            it.copy(isCreatingProgram = !_state.value.isCreatingProgram)
        }
    }

    fun createProgram(name: String) = executeWithErrorHandling("Error creating program") {
        executeInTransactionScope {
            var newProgram = Program(name = name, isActive = !_state.value.isManagingPrograms)
            if (_state.value.program != null && !_state.value.isManagingPrograms) {
                val programToArchive = _state.value.program!!.copy(isActive = false)
                programsRepository.update(programToArchive)
            }
            val newProgramId = programsRepository.insert(newProgram)
            newProgram = newProgram.copy(id = newProgramId)
            _state.update { currState ->
                currState.copy(
                    program = newProgram,
                    allPrograms = currState.allPrograms
                        .toMutableList()
                        .apply { add(newProgram) },
                    isCreatingProgram = false
                )
            }
        }
    }

    private fun createNewWorkout() = executeWithErrorHandling("Error creating workout") {
        executeInTransactionScope {
            val newWorkoutEntity = Workout(
                programId = _state.value.program!!.id,
                name = "New Workout",
                position = _state.value.program!!.workouts.count(),
                lifts = listOf()
            )
            val newWorkoutId = workoutsRepository.insert(newWorkoutEntity)
            _state.update { currentState ->
                currentState.copy(
                    workoutIdToRename = newWorkoutId,
                    originalWorkoutName = newWorkoutEntity.name,
                    program = currentState.program!!.copy(
                        workouts = currentState.program.workouts.toMutableList().apply {
                            add(
                                newWorkoutEntity.copy(
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

    fun updateWorkoutName(workoutId: Long, newName: String) = executeWithErrorHandling("Error updating workout name") {
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

    fun updateProgramName(newName: String) = executeWithErrorHandling("Error updating program name") {
        val program = _state.value.program
        if (program != null && _state.value.originalProgramName != newName) {
            executeInTransactionScope {
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

    fun deleteWorkout(workout: Workout) {
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

    fun beginDeleteWorkout(workout: Workout) {
        _state.update {
            it.copy(workoutToDelete = workout)
        }
    }

    fun cancelDeleteWorkout() {
        _state.update {
            it.copy(workoutToDelete = null)
        }
    }

    fun beginDeleteProgram(programId: Long?) {
        if (programId != null) {
            _state.update {
                it.copy(
                    isDeletingProgram = true,
                    idOfProgramToDelete = programId,
                )
            }
        }
    }

    fun cancelDeleteProgram() {
        _state.update {
            it.copy(
                isDeletingProgram = false,
                idOfProgramToDelete = null,
            )
        }
    }

    fun deleteProgram(programId: Long) = executeWithErrorHandling("Error deleting program") {
        executeInTransactionScope {
            if (_state.value.isManagingPrograms) {
                val programToDelete = _state.value.allPrograms.find { it.id == programId }!!
                programsRepository.delete(programToDelete)
                val programs = programsRepository.getAll()

                _state.update {
                    it.copy(
                        program = programs.firstOrNull { it.isActive },
                        idOfProgramToDelete = null,
                        isDeletingProgram = false,
                        allPrograms = programs,
                    )
                }
            } else {
                programsRepository.delete(_state.value.program!!)
                val activeProgram = programsRepository.getActive()
                _state.update {
                    it.copy(
                        program = activeProgram,
                        isDeletingProgram = false,
                    )
                }
            }
        }
    }

    fun saveReorder(newOrder: List<ReorderableListItem>) = executeWithErrorHandling("Error saving reorder") {
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

    private fun toggleOffReorderingAndProgramManagement() {
        _state.update {
            it.copy(
                isReordering = false,
                isManagingPrograms = false
            )
        }
    }

    fun toggleReorderingScreen() {
        _state.update {
            it.copy(isReordering = !it.isReordering)
        }
    }

    fun toggleManageProgramsScreen() {
        _state.update {
            it.copy(
                isManagingPrograms = !it.isManagingPrograms,
            )
        }
    }

    fun setProgramAsActive(programId: Long) = executeWithErrorHandling("Error setting program as active") {
        // ProgramEntity is already active
        if (_state.value.program?.id == programId) return@executeWithErrorHandling

        executeInTransactionScope {
            val programsToUpdate = mutableListOf<Program>()
            val newActiveProgram = _state.value.allPrograms
                .find { it.id == programId }
                ?.copy(isActive = true)

            if (newActiveProgram != null) {
                programsToUpdate.add(newActiveProgram)

                // Theoretically, this should never be null. You can only open programEntity management
                // if a programEntity exists. Just in case though!
                val programToArchive = _state.value.program?.copy(isActive = false)?.let { programToArchive ->
                    programsToUpdate.add(programToArchive)
                    programToArchive
                }

                programsRepository.updateMany(programsToUpdate)

                // State updated via observer collect
            }
        }
    }
}