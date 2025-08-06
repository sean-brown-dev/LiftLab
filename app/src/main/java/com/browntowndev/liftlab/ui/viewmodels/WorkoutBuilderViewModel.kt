package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.AddSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.ConvertWorkoutLiftTypeUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.DeleteCustomLiftSetByPositionUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.DeleteWorkoutLiftUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.GetWorkoutConfigurationStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.ReorderWorkoutBuilderLiftsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateCustomLiftSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateLiftIncrementOverrideUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateRestTimeUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateWorkoutLiftDeloadWeekUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateWorkoutLiftUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateWorkoutNameUseCase
import com.browntowndev.liftlab.ui.mapping.WorkoutMappingExtensions.toDomainModel
import com.browntowndev.liftlab.ui.mapping.WorkoutMappingExtensions.toUiModel
import com.browntowndev.liftlab.ui.models.controls.ReorderableListItem
import com.browntowndev.liftlab.ui.models.controls.TopAppBarEvent
import com.browntowndev.liftlab.ui.models.workout.CustomLiftSetUiModel
import com.browntowndev.liftlab.ui.models.workout.CustomWorkoutLiftUiModel
import com.browntowndev.liftlab.ui.models.workout.DropSetUiModel
import com.browntowndev.liftlab.ui.models.workout.MyoRepSetUiModel
import com.browntowndev.liftlab.ui.models.workout.StandardSetUiModel
import com.browntowndev.liftlab.ui.models.workout.StandardWorkoutLiftUiModel
import com.browntowndev.liftlab.ui.models.workout.WorkoutLiftUiModel
import com.browntowndev.liftlab.ui.models.workout.WorkoutUiModel
import com.browntowndev.liftlab.ui.models.workout.getRecalculatedStepSizeForLift
import com.browntowndev.liftlab.ui.models.workout.getRecalculatedWorkoutLiftStepSizeOptions
import com.browntowndev.liftlab.ui.models.workout.transformToType
import com.browntowndev.liftlab.ui.viewmodels.states.PickerState
import com.browntowndev.liftlab.ui.viewmodels.states.PickerType
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutBuilderState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutBuilderViewModel(
    private val workoutId: Long,
    private val liftLevelDeloadsEnabled: Boolean,
    private val onNavigateBack: () -> Unit,
    private val convertWorkoutLiftTypeUseCase: ConvertWorkoutLiftTypeUseCase,
    private val reorderWorkoutBuilderLiftsUseCase: ReorderWorkoutBuilderLiftsUseCase,
    private val deleteWorkoutLiftUseCase: DeleteWorkoutLiftUseCase,
    private val updateWorkoutNameUseCase: UpdateWorkoutNameUseCase,
    private val updateRestTimeUseCase: UpdateRestTimeUseCase,
    private val updateLiftIncrementOverrideUseCase: UpdateLiftIncrementOverrideUseCase,
    private val updateWorkoutLiftUseCase: UpdateWorkoutLiftUseCase,
    private val deleteCustomLiftSetByPositionUseCase: DeleteCustomLiftSetByPositionUseCase,
    private val updateCustomLiftSetUseCase: UpdateCustomLiftSetUseCase,
    private val addSetUseCase: AddSetUseCase,
    private val updateWorkoutLiftDeloadWeekUseCase: UpdateWorkoutLiftDeloadWeekUseCase,
    getWorkoutConfigurationStateFlowUseCase: GetWorkoutConfigurationStateFlowUseCase,
    eventBus: EventBus,
): BaseViewModel(eventBus) {
    companion object {
        private const val TAG = "WorkoutBuilderViewModel"
        private const val DEFAULT_PROGRAM_DELOAD_WEEK = 4
    }
    
    private var _state = MutableStateFlow(WorkoutBuilderState())
    val state = _state.asStateFlow()

    init {
        getWorkoutConfigurationStateFlowUseCase(
            workoutId = workoutId,
            liftLevelDeloadsEnabled = liftLevelDeloadsEnabled
        ).map { workoutConfigurationState ->
            WorkoutBuilderState(
                workout = workoutConfigurationState.workout?.toUiModel(),
                programDeloadWeek = workoutConfigurationState.programDeloadWeek ?: DEFAULT_PROGRAM_DELOAD_WEEK,
                workoutLiftStepSizeOptions = workoutConfigurationState.workoutLiftStepSizeOptions,
            )
        }.onEach { state ->
            _state.update { currentState ->
                currentState.copy(
                    workout = state.workout,
                    programDeloadWeek = state.programDeloadWeek,
                    workoutLiftStepSizeOptions = state.workoutLiftStepSizeOptions,
                    workoutLiftIdToDelete = null,
                    isReordering = false,
                    isEditingName = false,
                )
            }
        }.catch {
            Log.e(TAG, "Error getting workout", it)
            FirebaseCrashlytics.getInstance().recordException(it)
            emitUserMessage("Failed to load workout builder")
        }.launchIn(viewModelScope)
    }

    @Subscribe
    fun handleActionBarEvents(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.NavigatedBack -> onNavigateBack()
            TopAppBarAction.RenameWorkout -> toggleWorkoutRenameModal()
            TopAppBarAction.ReorderLifts -> toggleReorderLifts()
            else -> {}
        }
    }

    fun toggleMovementPatternDeletionModal(workoutLiftId: Long? = null) {
        _state.update {
            it.copy(workoutLiftIdToDelete = workoutLiftId)
        }
    }

    fun deleteMovementPattern() = executeWithErrorHandling("Failed to delete movement pattern") {
        val workoutLiftId = _state.value.workoutLiftIdToDelete ?: -1
        val workoutLiftToDelete = getWorkoutLiftAndLogIfNull<WorkoutLiftUiModel>(workoutLiftId) ?: return@executeWithErrorHandling
        deleteWorkoutLiftUseCase(workoutLiftToDelete.toDomainModel())
    }

    fun updateWorkoutName(newName: String) = executeWithErrorHandling("Failed to update workout name") {
        val workout = getCurrentWorkoutAndLogIfNull() ?: return@executeWithErrorHandling
        updateWorkoutNameUseCase(workout.id, newName)
    }

    fun toggleWorkoutRenameModal() {
        _state.update { it.copy(isEditingName = !it.isEditingName) }
    }

    fun toggleReorderLifts() {
        _state.update { it.copy(isReordering = !_state.value.isReordering) }
    }

    fun toggleRpePicker(
        visible: Boolean,
        workoutLiftId: Long,
        position: Int? = null,
        type: PickerType, currentRpe: Float? = null,
        currentPercentage: Float? = null
    ) {
        _state.update {
            it.copy(
                pickerState = if (visible) {
                    PickerState(
                        workoutLiftId = workoutLiftId,
                        setPosition = position,
                        currentRpe = currentRpe,
                        currentPercentage = currentPercentage,
                        type = type
                    )
                } else null
            )
        }
    }

    fun toggleDetailExpansion(workoutLiftId: Long, position: Int) {
        _state.update {currentState ->
            val expansionStatesCopy = HashMap(currentState.detailExpansionStates)
            val setStatesCopy = expansionStatesCopy[workoutLiftId]?.toHashSet() ?: hashSetOf()

            if (setStatesCopy.contains(position)) {
                setStatesCopy.remove(position)
            } else {
                setStatesCopy.add(position)
            }

            expansionStatesCopy[workoutLiftId] = setStatesCopy
            currentState.copy(detailExpansionStates = expansionStatesCopy)
        }
    }

    fun toggleHasCustomLiftSets(workoutLiftId: Long, enableCustomSets: Boolean) = executeWithErrorHandling("Failed to toggle custom lift sets") {
        val workoutLiftToConvert = getWorkoutLiftAndLogIfNull<WorkoutLiftUiModel>(workoutLiftId)
            ?: return@executeWithErrorHandling
        convertWorkoutLiftTypeUseCase(workoutLiftToConvert.toDomainModel(), enableCustomSets)
    }

    fun setRestTime(workoutLiftId: Long, newRestTime: Duration, enabled: Boolean) = executeWithErrorHandling("Failed to update rest time") {
        val workoutLift = getWorkoutLiftAndLogIfNull<WorkoutLiftUiModel>(workoutLiftId)
            ?: return@executeWithErrorHandling
        updateRestTimeUseCase(
            liftId = workoutLift.liftId,
            enabled = enabled,
            restTime = newRestTime
        )
    }

    fun setIncrementOverride(workoutLiftId: Long, newIncrement: Float) = executeWithErrorHandling("Failed to update increment override") {
        val workoutLift = getWorkoutLiftAndLogIfNull<WorkoutLiftUiModel>(workoutLiftId)
            ?: return@executeWithErrorHandling
        updateLiftIncrementOverrideUseCase(
            liftId = workoutLift.liftId,
            incrementOverride = newIncrement,
        )
    }

    fun reorderLifts(newLiftOrder: List<ReorderableListItem>) = executeWithErrorHandling("Failed to reorder lifts") {
        val newWorkoutLiftIndices = newLiftOrder
            .mapIndexed { index, item -> item.key to index }
            .associate { it.first to it.second }
        reorderWorkoutBuilderLiftsUseCase(
            workoutId = workoutId,
            workoutLifts = _state.value.workout!!.lifts.fastMap { it.toDomainModel() },
            newWorkoutLiftIndices = newWorkoutLiftIndices)
    }

    private fun updateLiftProperty(
        workoutLiftId: Long, 
        copyLift: (WorkoutLiftUiModel) -> WorkoutLiftUiModel
    ): WorkoutUiModel {
        val workout = getCurrentWorkoutAndLogIfNull() ?: throw Exception("Workout not found")
        val workoutLift = getWorkoutLiftAndLogIfNull<WorkoutLiftUiModel>(workoutLiftId) ?: return workout
        val indexOfWorkoutLift = workout.lifts.indexOf(workoutLift)
        if (indexOfWorkoutLift == -1) throw Exception("Workout lift not found")
        return workout.copy(
            lifts = workout.lifts.toMutableList().apply {
                set(indexOfWorkoutLift, copyLift(workoutLift))
            }
        )
    }

    fun updateDeloadWeek(workoutLiftId: Long, newDeloadWeek: Int?) = executeWithErrorHandling("Failed to update deload week") {
        val workoutLift = getWorkoutLiftAndLogIfNull<WorkoutLiftUiModel>(workoutLiftId) ?: return@executeWithErrorHandling
        updateWorkoutLiftDeloadWeekUseCase(workoutLift.toDomainModel(), newDeloadWeek, getProgramDeloadWeekAndLogIfNull())
    }

    fun setLiftSetCount(workoutLiftId: Long, newSetCount: Int) = executeWithErrorHandling("Failed to update set count") {
        val workoutLift = getWorkoutLiftAndLogIfNull<WorkoutLiftUiModel>(workoutLiftId) ?: return@executeWithErrorHandling
        val updatedWorkoutLift = when (workoutLift) {
            is StandardWorkoutLiftUiModel -> workoutLift.copy(setCount = newSetCount)
            is CustomWorkoutLiftUiModel -> workoutLift.copy(setCount = newSetCount)
            else -> throw Exception("${workoutLift::class.simpleName} not recognized.")
        }
        updateWorkoutLiftUseCase(updatedWorkoutLift.toDomainModel())
    }

    fun setLiftRpeTarget(workoutLiftId: Long, newRpeTarget: Float) = executeWithErrorHandling("Failed to update RPE target") {
        val workoutLift = getWorkoutLiftAndLogIfNull<StandardWorkoutLiftUiModel>(workoutLiftId) ?: return@executeWithErrorHandling
        val updatedWorkoutLift = workoutLift.copy(rpeTarget = newRpeTarget)
        updateWorkoutLiftUseCase(updatedWorkoutLift.toDomainModel())
    }

    fun setLiftProgressionScheme(workoutLiftId: Long, newProgressionScheme: ProgressionScheme) = executeWithErrorHandling("Failed to update progression scheme") {
        val workoutLift = getWorkoutLiftAndLogIfNull<WorkoutLiftUiModel>(workoutLiftId) ?: return@executeWithErrorHandling
        val updatedWorkoutLift = when (workoutLift) {
            is StandardWorkoutLiftUiModel -> workoutLift.copy(progressionScheme = newProgressionScheme)
            is CustomWorkoutLiftUiModel -> workoutLift.copy(progressionScheme = newProgressionScheme)
            else -> throw Exception("${workoutLift::class.simpleName} not recognized.")
        }
        updateWorkoutLiftUseCase(updatedWorkoutLift.toDomainModel())
    }

    fun updateStepSize(workoutLiftId: Long, newStepSize: Int) = executeWithErrorHandling("Failed to update step size") {
        val updatedWorkoutLift = getWorkoutLiftAndLogIfNull<StandardWorkoutLiftUiModel>(workoutLiftId)?.copy(stepSize = newStepSize)
            ?: return@executeWithErrorHandling
        updateWorkoutLiftUseCase(updatedWorkoutLift.toDomainModel())
    }

    fun addSet(workoutLiftId: Long) = executeWithErrorHandling("Failed to add set") {
        addSetUseCase(
            workoutLifts = _state.value.workout!!.lifts.fastMap { it.toDomainModel() },
            workoutLiftId = workoutLiftId,
        )
    }

    fun deleteSet(workoutLiftId: Long, position: Int) = executeWithErrorHandling("Failed to delete set") {
        deleteCustomLiftSetByPositionUseCase(workoutLiftId, position)
    }

    fun setCustomSetRpeTarget(workoutLiftId: Long, position: Int, newRpeTarget: Float) = executeWithErrorHandling("Failed to update RPE target") {
        val currentSet = getCustomLiftSetAndLogIfNull<CustomLiftSetUiModel>(workoutLiftId, position) ?: return@executeWithErrorHandling
        val updatedSet = when (currentSet) {
            is StandardSetUiModel -> currentSet.copy(rpeTarget = newRpeTarget)
            is DropSetUiModel -> currentSet.copy(rpeTarget = newRpeTarget)
            else -> throw Exception("${currentSet::class.simpleName} cannot have an rpe target.")
        }

        updateCustomLiftSetUseCase(updatedSet.toDomainModel())
    }

    fun setCustomSetRepFloor(workoutLiftId: Long, position: Int, newRepFloor: Int) = executeWithErrorHandling("Failed to update rep floor") {
        val updatedSet = getCustomLiftSetAndLogIfNull<MyoRepSetUiModel>(workoutLiftId, position)?.copy(repFloor = newRepFloor)
            ?: return@executeWithErrorHandling
        updateCustomLiftSetUseCase(updatedSet.toDomainModel())
    }

    fun setCustomSetUseSetMatching(workoutLiftId: Long, position: Int, setMatching: Boolean) = executeWithErrorHandling("Failed to toggle set matching") {
        val updatedSet = getCustomLiftSetAndLogIfNull<MyoRepSetUiModel>(workoutLiftId, position)?.copy(setMatching = setMatching)
            ?: return@executeWithErrorHandling
        updateCustomLiftSetUseCase(updatedSet.toDomainModel())
    }

    fun setCustomSetMatchSetGoal(workoutLiftId: Long, position: Int, newMatchSetGoal: Int) = executeWithErrorHandling("Failed to update set match goal") {
        val updatedSet = getCustomLiftSetAndLogIfNull<MyoRepSetUiModel>(workoutLiftId, position)?.copy(setGoal = newMatchSetGoal)
            ?: return@executeWithErrorHandling
        updateCustomLiftSetUseCase(updatedSet.toDomainModel())
    }

    fun setCustomSetMaxSets(workoutLiftId: Long, position: Int, newMaxSets: Int?) = executeWithErrorHandling("Failed to update max sets") {
        val updatedSet = getCustomLiftSetAndLogIfNull<MyoRepSetUiModel>(workoutLiftId, position)?.copy(maxSets = newMaxSets)
            ?: return@executeWithErrorHandling
        updateCustomLiftSetUseCase(updatedSet.toDomainModel())
    }

    fun setCustomSetDropPercentage(workoutLiftId: Long, position: Int, newDropPercentage: Float) = executeWithErrorHandling("Failed to update drop percentage") {
        val updatedSet = getCustomLiftSetAndLogIfNull<DropSetUiModel>(workoutLiftId, position)?.copy(dropPercentage = newDropPercentage)
            ?: return@executeWithErrorHandling
        updateCustomLiftSetUseCase(updatedSet.toDomainModel())
    }

    fun changeCustomSetType(workoutLiftId: Long, position: Int, newSetType: SetType) = executeWithErrorHandling("Failed to change set type") {
        val transformedSet = getCustomLiftSetAndLogIfNull<CustomLiftSetUiModel>(workoutLiftId, position)?.transformToType(newSetType)
            ?: return@executeWithErrorHandling
        updateCustomLiftSetUseCase(transformedSet.toDomainModel())
    }

    private fun updateCustomSetProperty(
        workoutLiftId: Long,
        setPosition: Int,
        copyAll: Boolean = false,
        copySet: (CustomLiftSetUiModel) -> CustomLiftSetUiModel
    ): WorkoutUiModel {
        val currentWorkout = getCurrentWorkoutAndLogIfNull() ?: throw Exception("Workout not found")
        val currentWorkoutLift = getWorkoutLiftAndLogIfNull<CustomWorkoutLiftUiModel>(workoutLiftId) ?: throw Exception("Workout lift not found")
        val updatedWorkoutLift = currentWorkoutLift.copy(
            customLiftSets = currentWorkoutLift.customLiftSets.fastMap { set ->
                if (copyAll || set.position == setPosition) copySet(set) else set
            }
        )

        val indexOfLift = currentWorkout.lifts.indexOf(currentWorkoutLift)
        if (indexOfLift == -1) throw Exception("Workout lift not found") // Should be impossible
        val updatedWorkout = currentWorkout.copy(
            lifts = currentWorkout.lifts.toMutableList().apply {
                set(indexOfLift, updatedWorkoutLift)
            }
        )
        return updatedWorkout
    }

    fun setLiftRepRangeBottom(workoutLiftId: Long, newRepRangeBottom: Int) = executeWithErrorHandling("Failed to update rep range bottom") {
        val updatedWorkout = updateLiftProperty(workoutLiftId) { lift ->
            when (lift) {
                is StandardWorkoutLiftUiModel -> lift.copy(
                    repRangeBottom = newRepRangeBottom,
                    stepSize = lift.getRecalculatedStepSizeForLift(
                        deloadToUseInsteadOfLiftLevel = lift.deloadWeek ?: getProgramDeloadWeekAndLogIfNull()
                    )
                )

                else -> throw Exception("${lift::class.simpleName} cannot have a top rep range.")
            }
        }

        _state.update {
            it.copy(
                workout = updatedWorkout,
                workoutLiftStepSizeOptions = updatedWorkout.getRecalculatedWorkoutLiftStepSizeOptions(
                    programDeloadWeek = it.programDeloadWeek!!,
                    liftLevelDeloadsEnabled = liftLevelDeloadsEnabled),
            )
        }
    }

    fun setLiftRepRangeTop(workoutLiftId: Long, newRepRangeTop: Int) = executeWithErrorHandling("Failed to update rep range top") {
        val updatedWorkout = updateLiftProperty(workoutLiftId) { lift ->
            when (lift) {
                is StandardWorkoutLiftUiModel -> lift.copy(
                    repRangeTop = newRepRangeTop,
                    stepSize = lift.getRecalculatedStepSizeForLift(
                        deloadToUseInsteadOfLiftLevel = lift.deloadWeek ?: getProgramDeloadWeekAndLogIfNull(),
                    )
                )

                else -> throw Exception("${lift::class.simpleName} cannot have a top rep range.")
            }
        }

        _state.update {
            it.copy(
                workout = updatedWorkout,
                workoutLiftStepSizeOptions = updatedWorkout.getRecalculatedWorkoutLiftStepSizeOptions(
                    programDeloadWeek = it.programDeloadWeek!!,
                    liftLevelDeloadsEnabled = liftLevelDeloadsEnabled),
            )
        }
    }

    fun confirmStandardSetRepRangeBottom(workoutLiftId: Long) = executeWithErrorHandling("Failed to update rep range bottom") {
        val originalWorkoutLift = getWorkoutLiftAndLogIfNull<StandardWorkoutLiftUiModel>(workoutLiftId) ?: return@executeWithErrorHandling
        val validatedRepRangeBottom = getValidatedRepRangeBottom(
            newRepRangeBottom = originalWorkoutLift.repRangeBottom, 
            repRangeTop = originalWorkoutLift.repRangeTop)

        Log.d(TAG, "confirmStandardSetRepRangeBottom - validatedRepRangeBottom: $validatedRepRangeBottom")

        val workoutLift = originalWorkoutLift.copy(
            repRangeBottom = validatedRepRangeBottom,
            stepSize = originalWorkoutLift.getRecalculatedStepSizeForLift(
                deloadToUseInsteadOfLiftLevel = originalWorkoutLift.deloadWeek ?: getProgramDeloadWeekAndLogIfNull(),
            )
        )

        updateWorkoutLiftUseCase(workoutLift.toDomainModel())
    }

    fun confirmStandardSetRepRangeTop(workoutLiftId: Long) = executeWithErrorHandling("Failed to update rep range top") {
        val originalWorkoutLift = getWorkoutLiftAndLogIfNull<StandardWorkoutLiftUiModel>(workoutLiftId) ?: return@executeWithErrorHandling
        val validatedRepRangeTop = getValidatedRepRangeTop(
            newRepRangeTop = originalWorkoutLift.repRangeTop, 
            repRangeBottom = originalWorkoutLift.repRangeBottom)

        Log.d(TAG, "confirmStandardSetRepRangeBottom - validatedRepRangeBottom: $validatedRepRangeTop")

        val workoutLift = originalWorkoutLift.copy(
            repRangeTop = validatedRepRangeTop,
            stepSize = originalWorkoutLift.getRecalculatedStepSizeForLift(
                deloadToUseInsteadOfLiftLevel = originalWorkoutLift.deloadWeek ?: getProgramDeloadWeekAndLogIfNull()
            )
        )

        updateWorkoutLiftUseCase(workoutLift.toDomainModel())
    }

    fun setCustomSetRepRangeBottom(workoutLiftId: Long, position: Int, newRepRangeBottom: Int) = executeWithErrorHandling("Failed to update rep range bottom") {
        _state.update { currentState ->
            currentState.copy(
                workout = updateCustomSetProperty(workoutLiftId, position) { set ->
                    when (set) {
                        is StandardSetUiModel -> set.copy(repRangeBottom = newRepRangeBottom)
                        is DropSetUiModel -> set.copy(repRangeBottom = newRepRangeBottom)
                        is MyoRepSetUiModel -> set.copy(repRangeBottom = newRepRangeBottom)
                        else -> throw Exception("${set::class.simpleName} cannot have a bottom rep range.")
                    }
                }
            )
        }
    }

    fun setCustomSetRepRangeTop(workoutLiftId: Long, position: Int, newRepRangeTop: Int) = executeWithErrorHandling("Failed to update rep range top") {
        _state.update { currentState ->
            currentState.copy(
                workout = updateCustomSetProperty(workoutLiftId, position) { set ->
                    when (set) {
                        is StandardSetUiModel -> set.copy(repRangeTop = newRepRangeTop)
                        is DropSetUiModel -> set.copy(repRangeTop = newRepRangeTop)
                        is MyoRepSetUiModel -> set.copy(repRangeTop = newRepRangeTop)
                        else -> throw Exception("${set::class.simpleName} cannot have a top rep range.")
                    }
                }
            )
        }
    }

    fun confirmCustomSetRepRangeBottom(workoutLiftId: Long, position: Int) = executeWithErrorHandling("Failed to update rep range bottom") {
        val customSet = getCustomLiftSetAndLogIfNull<CustomLiftSetUiModel>(workoutLiftId, position) ?: return@executeWithErrorHandling
        val validatedRepRangeBottom = getValidatedRepRangeBottom(
            newRepRangeBottom = customSet.repRangeBottom,
            repRangeTop = customSet.repRangeTop)

        val updatedSet = when (customSet) {
            is StandardSetUiModel -> customSet.copy(repRangeBottom = validatedRepRangeBottom)
            is MyoRepSetUiModel -> customSet.copy(repRangeBottom = validatedRepRangeBottom)
            is DropSetUiModel -> customSet.copy(repRangeBottom = validatedRepRangeBottom)
            else -> throw Exception("${customSet::class.simpleName} is not defined")
        }
        updateCustomLiftSetUseCase(updatedSet.toDomainModel())
    }

    fun confirmCustomSetRepRangeTop(workoutLiftId: Long, position: Int) = executeWithErrorHandling("Failed to update rep range top") {
        val customSet = getCustomLiftSetAndLogIfNull<CustomLiftSetUiModel>(workoutLiftId, position) ?: return@executeWithErrorHandling
        val validatedRepRangeTop = getValidatedRepRangeTop(
            newRepRangeTop = customSet.repRangeTop,
            repRangeBottom = customSet.repRangeBottom)

        val updatedSet = when (customSet) {
            is StandardSetUiModel -> customSet.copy(repRangeTop = validatedRepRangeTop)
            is MyoRepSetUiModel -> customSet.copy(repRangeTop = validatedRepRangeTop)
            is DropSetUiModel -> customSet.copy(repRangeTop = validatedRepRangeTop)
            else -> throw Exception("${customSet::class.simpleName} is not defined")
        }
        updateCustomLiftSetUseCase(updatedSet.toDomainModel())
    }

    private fun getValidatedRepRangeBottom(newRepRangeBottom: Int, repRangeTop: Int): Int {
        return if (newRepRangeBottom < repRangeTop) {
            newRepRangeBottom
        } else {
            repRangeTop - 1
        }
    }

    private fun getValidatedRepRangeTop(newRepRangeTop: Int, repRangeBottom: Int): Int {
        return if (newRepRangeTop > repRangeBottom) {
            newRepRangeTop
        } else {
            repRangeBottom + 1
        }
    }
    
    private fun getCurrentWorkoutAndLogIfNull(): WorkoutUiModel? {
        val workout = _state.value.workout
        if (workout == null) {
            emitUserMessage("Workout not initialized!")
            val exception = Exception("Workout not initialized!")
            Log.e(TAG, "Workout was not initialized", exception)
            FirebaseCrashlytics.getInstance().recordException(exception)
        }
        
        return workout
    }
    
    private inline fun<reified T: WorkoutLiftUiModel> getWorkoutLiftAndLogIfNull(workoutLiftId: Long): T? {
        val workoutLift = _state.value.workout?.lifts
            ?.find { it.id == workoutLiftId } as? T

        if (workoutLift == null) {
            emitUserMessage("Must be standard workoutEntity liftEntity.")
            val exception = Exception("Must be standard workoutEntity liftEntity.")
            Log.e(TAG, "Must be standard workoutEntity liftEntity.", exception)
            FirebaseCrashlytics.getInstance().recordException(exception)
        }
        
        return workoutLift
    }

    private inline fun<reified T: CustomLiftSetUiModel> getCustomLiftSetAndLogIfNull(workoutLiftId: Long, position: Int): T? {
        val workoutLift = getWorkoutLiftAndLogIfNull<CustomWorkoutLiftUiModel>(workoutLiftId) ?: return null
        val customSet = safeGetCustomSetAtPositionAndLogIfNull<T>(workoutLift.customLiftSets, position) ?: return null
        return customSet
    }

    private inline fun<reified T: CustomLiftSetUiModel> safeGetCustomSetAtPositionAndLogIfNull(customLiftSets: List<CustomLiftSetUiModel>, position: Int): T? {
        if (customLiftSets.size > position) {
            return customLiftSets[position] as? T
        } else {
            emitUserMessage("Custom liftEntity set not found!")
            val exception = Exception("Custom liftEntity position out of bounds. set count=${customLiftSets.size}, position=$position")
            Log.e(TAG, "Custom liftEntity position out of bounds. set count=${customLiftSets.size}, position=$position", exception)
            FirebaseCrashlytics.getInstance().recordException(exception)
            return null
        }
    }
    
    private fun getProgramDeloadWeekAndLogIfNull(): Int {
        val programDeloadWeek = _state.value.programDeloadWeek
        return if (programDeloadWeek != null) {
            programDeloadWeek
        } else {
            emitUserMessage("Using default deload week of $DEFAULT_PROGRAM_DELOAD_WEEK")
            val exception = Exception("Program deload week not initialized!")
            Log.e(TAG, "Program deload week was not initialized", exception)
            FirebaseCrashlytics.getInstance().recordException(exception)
            DEFAULT_PROGRAM_DELOAD_WEEK
        }
    }
}