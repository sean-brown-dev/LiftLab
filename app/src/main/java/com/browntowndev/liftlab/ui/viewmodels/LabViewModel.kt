package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.ReorderableListItem
import com.browntowndev.liftlab.core.common.Utils
import com.browntowndev.liftlab.core.common.Utils.StepSize.Companion.getAllLiftsWithRecalculatedStepSize
import com.browntowndev.liftlab.core.common.Utils.StepSize.Companion.getRecalculatedStepSizeForLift
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.persistence.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutLiftsRepository
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
    private val workoutLiftsRepository: WorkoutLiftsRepository,
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

    fun updateDeloadWeek(deloadWeek: Int) {
        executeInTransactionScope {
            programsRepository.updateDeloadWeek(_state.value.program!!.id, deloadWeek)
            val liftsWithNewStepSizes: Map<Long, StandardWorkoutLiftDto> = if (_state.value.program != null) {
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

    fun createProgram(name: String) {
        executeInTransactionScope {
            var newProgram = ProgramDto(name = name, isActive = !_state.value.isManagingPrograms)
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

    fun deleteProgram(programId: Long) {
        executeInTransactionScope {
            if (_state.value.isManagingPrograms) {
                val programToDelete = _state.value.allPrograms.find { it.id == programId }!!
                val isActive = programToDelete.id == _state.value.program?.id
                var newActiveProgram: ProgramDto? = null

                if (isActive) {
                    deleteActiveProgram()
                    newActiveProgram = _state.value.allPrograms
                        .firstOrNull { it.id != programId }
                        ?.copy(isActive = true)
                        ?.also {
                            programsRepository.update(it)
                        }
                } else {
                    programsRepository.delete(programToDelete)
                }

                _state.update {
                    it.copy(
                        program = if (isActive) newActiveProgram else it.program,
                        idOfProgramToDelete = null,
                        isDeletingProgram = false,
                        allPrograms = it.allPrograms.mapNotNull { program ->
                            if (program.id == newActiveProgram?.id) {
                                newActiveProgram
                            } else if (program.id != programId) {
                                program
                            }
                            else null
                        }
                    )
                }
            } else {
                deleteActiveProgram()
                val newActiveProgram = programsRepository.getAll()
                    .firstOrNull()
                    ?.copy(isActive = true)
                    ?.also {
                        programsRepository.update(it)
                    }

                _state.update {
                    it.copy(
                        program = newActiveProgram,
                        isDeletingProgram = false,
                    )
                }
            }
        }
    }

    private suspend fun deleteActiveProgram() {
        val program = _state.value.program
        if (program != null) {
            programsRepository.delete(program)
            workoutInProgressRepository.delete()
            restTimerInProgressRepository.deleteAll()
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
        executeInTransactionScope {
            // This UI will rarely be clicked, so I think it's fine to just get this each time it's opened
            val allPrograms = programsRepository.getAll().sortedBy { it.name }
            _state.update {
                it.copy(
                    allPrograms = allPrograms,
                    isManagingPrograms = !it.isManagingPrograms,
                )
            }
        }
    }

    fun setProgramAsActive(programId: Long) {
        // Program is already active
        if (_state.value.program?.id == programId) return

        executeInTransactionScope {
            val programsToUpdate = mutableListOf<ProgramDto>()
            val newActiveProgram = _state.value.allPrograms
                .find { it.id == programId }
                ?.copy(isActive = true)

            if (newActiveProgram != null) {
                programsToUpdate.add(newActiveProgram)

                // Theoretically, this should never be null. You can only open program management
                // if a program exists. Just in case though!
                val programToArchive = _state.value.program?.copy(isActive = false)?.let { programToArchive ->
                    programsToUpdate.add(programToArchive)
                    programToArchive
                }

                programsRepository.updateMany(programsToUpdate)

                _state.update {
                    it.copy(
                        program = null, // Will get retrieved by observe in init
                        allPrograms = it.allPrograms.map { program ->
                            when (program.id) {
                                programId -> {
                                    newActiveProgram
                                }
                                programToArchive?.id -> {
                                    programToArchive
                                }
                                else -> {
                                    program
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}