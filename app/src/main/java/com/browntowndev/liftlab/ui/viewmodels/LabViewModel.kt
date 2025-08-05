package com.browntowndev.liftlab.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.ui.models.ReorderableListItem
import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.ui.models.TopAppBarEvent
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.ReorderWorkoutsUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.SetProgramAsActiveUseCase
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.CreateProgramUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.CreateWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.DeleteProgramUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.DeleteWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.GetProgramConfigurationStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.UpdateProgramDeloadWeekUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.UpdateProgramNameUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateWorkoutNameUseCase
import com.browntowndev.liftlab.ui.viewmodels.states.LabState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class LabViewModel(
    private val updateProgramDeloadWeekUseCase: UpdateProgramDeloadWeekUseCase,
    private val createProgramUseCase: CreateProgramUseCase,
    private val createWorkoutUseCase: CreateWorkoutUseCase,
    private val updateWorkoutNameUseCase: UpdateWorkoutNameUseCase,
    private val updateProgramNameUseCase: UpdateProgramNameUseCase,
    private val deleteWorkoutUseCase: DeleteWorkoutUseCase,
    private val deleteProgramUseCase: DeleteProgramUseCase,
    private val reorderWorkoutsUseCase: ReorderWorkoutsUseCase,
    private val setProgramAsActiveUseCase: SetProgramAsActiveUseCase,
    getProgramConfigurationStateFlowUseCase: GetProgramConfigurationStateFlowUseCase,
    eventBus: EventBus,
): BaseViewModel(eventBus) {
    private var _state = MutableStateFlow(LabState())
    val state = _state.asStateFlow()

    init {
        getProgramConfigurationStateFlowUseCase()
            .map { programConfigurationState ->
                LabState(
                    allPrograms = programConfigurationState.allPrograms,
                    program = programConfigurationState.program,
                )
            }.onEach { state ->
                _state.update {
                    it.copy(
                        allPrograms = state.allPrograms,
                        program = state.program,
                        isCreatingProgram = false,
                        isDeletingProgram = false,
                        idOfProgramToDelete = null,
                        isEditingProgramName = false,
                        isReordering = false,
                        workoutIdToRename = null,
                        originalWorkoutName = null,
                        workoutToDelete = null,
                        isEditingDeloadWeek = false,
                    )
                }
            }.launchIn(viewModelScope)
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
        updateProgramDeloadWeekUseCase(
            program = _state.value.program!!,
            deloadWeek = deloadWeek,
            useLiftSpecificDeload = SettingsManager.getSetting(LIFT_SPECIFIC_DELOADING, DEFAULT_LIFT_SPECIFIC_DELOADING))
    }

    fun toggleCreateProgramModal() {
        _state.update {
            it.copy(isCreatingProgram = !_state.value.isCreatingProgram)
        }
    }

    fun createProgram(name: String) = executeWithErrorHandling("Error creating program") {
        createProgramUseCase(
            name = name,
            isActive = !_state.value.isManagingPrograms,
            currentActiveProgram = _state.value.program,
        )
    }

    private fun createNewWorkout() = executeWithErrorHandling("Error creating workout") {
        createWorkoutUseCase(
            program = _state.value.program!!,
            name = "New Workout")
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
            updateWorkoutNameUseCase(workoutId, newName)
        } else collapseEditWorkoutNameModal()
    }

    fun updateProgramName(newName: String) = executeWithErrorHandling("Error updating program name") {
        val program = _state.value.program
        if (program != null && _state.value.originalProgramName != newName) {
            updateProgramNameUseCase(program.id, newName)
        }
    }

    fun deleteWorkout(workout: Workout) = executeWithErrorHandling("Error deleting workout") {
        deleteWorkoutUseCase(workout)
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
        deleteProgramUseCase(programId)
    }

    fun saveReorder(newOrder: List<ReorderableListItem>) = executeWithErrorHandling("Error saving reorder") {
        val newWorkoutPositions = newOrder.mapIndexed { index, reorderableListItem ->
            reorderableListItem.key to index
        }.toMap()
        reorderWorkoutsUseCase(_state.value.program!!.workouts, newWorkoutPositions.toMap())
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
        setProgramAsActiveUseCase(programId, _state.value.allPrograms)
    }
}