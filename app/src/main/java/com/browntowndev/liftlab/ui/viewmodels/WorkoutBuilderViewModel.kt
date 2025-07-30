package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.ui.models.ReorderableListItem
import com.browntowndev.liftlab.core.common.Utils.StepSize.Companion.getRecalculatedStepSizeForLift
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.extensions.getRecalculatedWorkoutLiftStepSizeOptions
import com.browntowndev.liftlab.core.domain.extensions.transformToType
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.DropSet
import com.browntowndev.liftlab.core.domain.models.workout.MyoRepSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.domain.useCase.shared.UpdateRestTimeUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.AddSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.ConvertWorkoutLiftTypeUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.DeleteCustomLiftSetByPositionUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.DeleteWorkoutLiftUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.GetWorkoutConfigurationStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.ReorderWorkoutBuilderLiftsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateCustomLiftSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateLiftIncrementOverrideUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateWorkoutLiftUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateWorkoutNameUseCase
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
    getWorkoutConfigurationStateFlowUseCase: GetWorkoutConfigurationStateFlowUseCase,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {
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
                workout = workoutConfigurationState.workout,
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
        val workoutLiftToDelete = getWorkoutLiftAndLogIfNull<GenericWorkoutLift>(workoutLiftId) ?: return@executeWithErrorHandling
        deleteWorkoutLiftUseCase(workoutLiftToDelete)
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
        val workoutLiftToConvert = getWorkoutLiftAndLogIfNull<GenericWorkoutLift>(workoutLiftId)
            ?: return@executeWithErrorHandling
        convertWorkoutLiftTypeUseCase(workoutLiftToConvert, enableCustomSets)
    }

    fun setRestTime(workoutLiftId: Long, newRestTime: Duration, enabled: Boolean) = executeWithErrorHandling("Failed to update rest time") {
        val workoutLift = getWorkoutLiftAndLogIfNull<GenericWorkoutLift>(workoutLiftId)
            ?: return@executeWithErrorHandling
        updateRestTimeUseCase(
            liftId = workoutLift.liftId,
            enabled = enabled,
            restTime = newRestTime
        )
    }

    fun setIncrementOverride(workoutLiftId: Long, newIncrement: Float) = executeWithErrorHandling("Failed to update increment override") {
        val workoutLift = getWorkoutLiftAndLogIfNull<GenericWorkoutLift>(workoutLiftId)
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
            workoutLifts = _state.value.workout!!.lifts,
            newWorkoutLiftIndices = newWorkoutLiftIndices)
    }

    private fun updateLiftProperty(
        workoutLiftId: Long, 
        copyLift: (GenericWorkoutLift) -> GenericWorkoutLift
    ): Workout {
        val workout = getCurrentWorkoutAndLogIfNull() ?: throw Exception("Workout not found")
        val workoutLift = getWorkoutLiftAndLogIfNull<GenericWorkoutLift>(workoutLiftId) ?: return workout
        val indexOfWorkoutLift = workout.lifts.indexOf(workoutLift)
        if (indexOfWorkoutLift == -1) throw Exception("Workout lift not found")
        return workout.copy(
            lifts = workout.lifts.toMutableList().apply {
                set(indexOfWorkoutLift, copyLift(workoutLift))
            }
        )
    }

    fun updateDeloadWeek(workoutLiftId: Long, newDeloadWeek: Int?) = executeWithErrorHandling("Failed to update deload week") {
        val workoutLift = getWorkoutLiftAndLogIfNull<GenericWorkoutLift>(workoutLiftId) ?: return@executeWithErrorHandling
        val updatedWorkoutLift = when (workoutLift) {
            is StandardWorkoutLift -> workoutLift.copy(deloadWeek = newDeloadWeek)
            is CustomWorkoutLift -> workoutLift.copy(deloadWeek = newDeloadWeek)
            else -> throw Exception("${workoutLift::class.simpleName} not recognized.")
        }

        updateWorkoutLiftUseCase(updatedWorkoutLift)
    }

    fun setLiftSetCount(workoutLiftId: Long, newSetCount: Int) = executeWithErrorHandling("Failed to update set count") {
        val workoutLift = getWorkoutLiftAndLogIfNull<GenericWorkoutLift>(workoutLiftId) ?: return@executeWithErrorHandling
        val updatedWorkoutLift = when (workoutLift) {
            is StandardWorkoutLift -> workoutLift.copy(setCount = newSetCount)
            is CustomWorkoutLift -> workoutLift.copy(setCount = newSetCount)
            else -> throw Exception("${workoutLift::class.simpleName} not recognized.")
        }
        updateWorkoutLiftUseCase(updatedWorkoutLift)
    }

    fun setLiftRpeTarget(workoutLiftId: Long, newRpeTarget: Float) = executeWithErrorHandling("Failed to update RPE target") {
        val workoutLift = getWorkoutLiftAndLogIfNull<StandardWorkoutLift>(workoutLiftId) ?: return@executeWithErrorHandling
        val updatedWorkoutLift = workoutLift.copy(rpeTarget = newRpeTarget)
        updateWorkoutLiftUseCase(updatedWorkoutLift)
    }

    fun setLiftProgressionScheme(workoutLiftId: Long, newProgressionScheme: ProgressionScheme) = executeWithErrorHandling("Failed to update progression scheme") {
        val workoutLift = getWorkoutLiftAndLogIfNull<GenericWorkoutLift>(workoutLiftId) ?: return@executeWithErrorHandling
        val updatedWorkoutLift = when (workoutLift) {
            is StandardWorkoutLift -> workoutLift.copy(progressionScheme = newProgressionScheme)
            is CustomWorkoutLift -> workoutLift.copy(progressionScheme = newProgressionScheme)
            else -> throw Exception("${workoutLift::class.simpleName} not recognized.")
        }
        updateWorkoutLiftUseCase(updatedWorkoutLift)
    }

    fun updateStepSize(workoutLiftId: Long, newStepSize: Int) = executeWithErrorHandling("Failed to update step size") {
        val updatedWorkoutLift = getWorkoutLiftAndLogIfNull<StandardWorkoutLift>(workoutLiftId)?.copy(stepSize = newStepSize)
            ?: return@executeWithErrorHandling
        updateWorkoutLiftUseCase(updatedWorkoutLift)
    }

    fun addSet(workoutLiftId: Long) = executeWithErrorHandling("Failed to add set") {
        addSetUseCase(
            workoutLifts = _state.value.workout!!.lifts,
            workoutLiftId = workoutLiftId,
        )
    }

    fun deleteSet(workoutLiftId: Long, position: Int) = executeWithErrorHandling("Failed to delete set") {
        deleteCustomLiftSetByPositionUseCase(workoutLiftId, position)
    }

    fun setCustomSetRpeTarget(workoutLiftId: Long, position: Int, newRpeTarget: Float) = executeWithErrorHandling("Failed to update RPE target") {
        val currentSet = getCustomLiftSetAndLogIfNull<GenericLiftSet>(workoutLiftId, position) ?: return@executeWithErrorHandling
        val updatedSet = when (currentSet) {
            is StandardSet -> currentSet.copy(rpeTarget = newRpeTarget)
            is DropSet -> currentSet.copy(rpeTarget = newRpeTarget)
            else -> throw Exception("${currentSet::class.simpleName} cannot have an rpe target.")
        }

        updateCustomLiftSetUseCase(updatedSet)
    }

    fun setCustomSetRepFloor(workoutLiftId: Long, position: Int, newRepFloor: Int) = executeWithErrorHandling("Failed to update rep floor") {
        val updatedSet = getCustomLiftSetAndLogIfNull<MyoRepSet>(workoutLiftId, position)?.copy(repFloor = newRepFloor)
            ?: return@executeWithErrorHandling
        updateCustomLiftSetUseCase(updatedSet)
    }

    fun setCustomSetUseSetMatching(workoutLiftId: Long, position: Int, setMatching: Boolean) = executeWithErrorHandling("Failed to toggle set matching") {
        val updatedSet = getCustomLiftSetAndLogIfNull<MyoRepSet>(workoutLiftId, position)?.copy(setMatching = setMatching)
            ?: return@executeWithErrorHandling
        updateCustomLiftSetUseCase(updatedSet)
    }

    fun setCustomSetMatchSetGoal(workoutLiftId: Long, position: Int, newMatchSetGoal: Int) = executeWithErrorHandling("Failed to update set match goal") {
        val updatedSet = getCustomLiftSetAndLogIfNull<MyoRepSet>(workoutLiftId, position)?.copy(setGoal = newMatchSetGoal)
            ?: return@executeWithErrorHandling
        updateCustomLiftSetUseCase(updatedSet)
    }

    fun setCustomSetMaxSets(workoutLiftId: Long, position: Int, newMaxSets: Int?) = executeWithErrorHandling("Failed to update max sets") {
        val updatedSet = getCustomLiftSetAndLogIfNull<MyoRepSet>(workoutLiftId, position)?.copy(maxSets = newMaxSets)
            ?: return@executeWithErrorHandling
        updateCustomLiftSetUseCase(updatedSet)
    }

    fun setCustomSetDropPercentage(workoutLiftId: Long, position: Int, newDropPercentage: Float) = executeWithErrorHandling("Failed to update drop percentage") {
        val updatedSet = getCustomLiftSetAndLogIfNull<DropSet>(workoutLiftId, position)?.copy(dropPercentage = newDropPercentage)
            ?: return@executeWithErrorHandling
        updateCustomLiftSetUseCase(updatedSet)
    }

    fun changeCustomSetType(workoutLiftId: Long, position: Int, newSetType: SetType) = executeWithErrorHandling("Failed to change set type") {
        val transformedSet = getCustomLiftSetAndLogIfNull<GenericLiftSet>(workoutLiftId, position)?.transformToType(newSetType)
            ?: return@executeWithErrorHandling
        updateCustomLiftSetUseCase(transformedSet)
    }

    private fun updateCustomSetProperty(
        workoutLiftId: Long,
        setPosition: Int,
        copyAll: Boolean = false,
        copySet: (GenericLiftSet) -> GenericLiftSet
    ): Workout {
        val currentWorkout = getCurrentWorkoutAndLogIfNull() ?: throw Exception("Workout not found")
        val currentWorkoutLift = getWorkoutLiftAndLogIfNull<CustomWorkoutLift>(workoutLiftId) ?: throw Exception("Workout lift not found")
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
                is StandardWorkoutLift -> lift.copy(
                    repRangeBottom = newRepRangeBottom,
                    stepSize = getRecalculatedStepSizeForLift(
                        currStepSize = lift.stepSize,
                        repRangeTop = lift.repRangeTop,
                        repRangeBottom = newRepRangeBottom,
                        deloadWeek = lift.deloadWeek ?: getProgramDeloadWeekAndLogIfNull(),
                        progressionScheme = lift.progressionScheme,
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
                is StandardWorkoutLift -> lift.copy(
                    repRangeTop = newRepRangeTop,
                    stepSize = getRecalculatedStepSizeForLift(
                        currStepSize = lift.stepSize,
                        repRangeTop = newRepRangeTop,
                        repRangeBottom = lift.repRangeBottom,
                        deloadWeek = lift.deloadWeek ?: getProgramDeloadWeekAndLogIfNull(),
                        progressionScheme = lift.progressionScheme,
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
        val originalWorkoutLift = getWorkoutLiftAndLogIfNull<StandardWorkoutLift>(workoutLiftId) ?: return@executeWithErrorHandling
        val validatedRepRangeBottom = getValidatedRepRangeBottom(
            newRepRangeBottom = originalWorkoutLift.repRangeBottom, 
            repRangeTop = originalWorkoutLift.repRangeTop)

        Log.d(TAG, "confirmStandardSetRepRangeBottom - validatedRepRangeBottom: $validatedRepRangeBottom")

        val workoutLift = originalWorkoutLift.copy(
            repRangeBottom = validatedRepRangeBottom,
            stepSize = getRecalculatedStepSizeForLift(
                repRangeTop = originalWorkoutLift.repRangeTop,
                repRangeBottom = validatedRepRangeBottom,
                currStepSize = originalWorkoutLift.stepSize,
                progressionScheme = originalWorkoutLift.progressionScheme,
                deloadWeek = originalWorkoutLift.deloadWeek ?: getProgramDeloadWeekAndLogIfNull()
            )
        )

        updateWorkoutLiftUseCase(workoutLift)
    }

    fun confirmStandardSetRepRangeTop(workoutLiftId: Long) = executeWithErrorHandling("Failed to update rep range top") {
        val originalWorkoutLift = getWorkoutLiftAndLogIfNull<StandardWorkoutLift>(workoutLiftId) ?: return@executeWithErrorHandling
        val validatedRepRangeTop = getValidatedRepRangeTop(
            newRepRangeTop = originalWorkoutLift.repRangeTop, 
            repRangeBottom = originalWorkoutLift.repRangeBottom)

        Log.d(TAG, "confirmStandardSetRepRangeBottom - validatedRepRangeBottom: $validatedRepRangeTop")

        val workoutLift = originalWorkoutLift.copy(
            repRangeTop = validatedRepRangeTop,
            stepSize = getRecalculatedStepSizeForLift(
                repRangeTop = validatedRepRangeTop,
                repRangeBottom = originalWorkoutLift.repRangeBottom,
                currStepSize = originalWorkoutLift.stepSize,
                progressionScheme = originalWorkoutLift.progressionScheme,
                deloadWeek = originalWorkoutLift.deloadWeek ?: getProgramDeloadWeekAndLogIfNull()
            )
        )

        updateWorkoutLiftUseCase(workoutLift)
    }

    fun setCustomSetRepRangeBottom(workoutLiftId: Long, position: Int, newRepRangeBottom: Int) = executeWithErrorHandling("Failed to update rep range bottom") {
        _state.update { currentState ->
            currentState.copy(
                workout = updateCustomSetProperty(workoutLiftId, position) { set ->
                    when (set) {
                        is StandardSet -> set.copy(repRangeBottom = newRepRangeBottom)
                        is DropSet -> set.copy(repRangeBottom = newRepRangeBottom)
                        is MyoRepSet -> set.copy(repRangeBottom = newRepRangeBottom)
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
                        is StandardSet -> set.copy(repRangeTop = newRepRangeTop)
                        is DropSet -> set.copy(repRangeTop = newRepRangeTop)
                        is MyoRepSet -> set.copy(repRangeTop = newRepRangeTop)
                        else -> throw Exception("${set::class.simpleName} cannot have a top rep range.")
                    }
                }
            )
        }
    }

    fun confirmCustomSetRepRangeBottom(workoutLiftId: Long, position: Int) = executeWithErrorHandling("Failed to update rep range bottom") {
        val customSet = getCustomLiftSetAndLogIfNull<GenericLiftSet>(workoutLiftId, position) ?: return@executeWithErrorHandling
        val validatedRepRangeBottom = getValidatedRepRangeBottom(
            newRepRangeBottom = customSet.repRangeBottom,
            repRangeTop = customSet.repRangeTop)

        val updatedSet = when (customSet) {
            is StandardSet -> customSet.copy(repRangeBottom = validatedRepRangeBottom)
            is MyoRepSet -> customSet.copy(repRangeBottom = validatedRepRangeBottom)
            is DropSet -> customSet.copy(repRangeBottom = validatedRepRangeBottom)
            else -> throw Exception("${customSet::class.simpleName} is not defined")
        }
        updateCustomLiftSetUseCase(updatedSet)
    }

    fun confirmCustomSetRepRangeTop(workoutLiftId: Long, position: Int) = executeWithErrorHandling("Failed to update rep range top") {
        val customSet = getCustomLiftSetAndLogIfNull<GenericLiftSet>(workoutLiftId, position) ?: return@executeWithErrorHandling
        val validatedRepRangeTop = getValidatedRepRangeTop(
            newRepRangeTop = customSet.repRangeTop,
            repRangeBottom = customSet.repRangeBottom)

        val updatedSet = when (customSet) {
            is StandardSet -> customSet.copy(repRangeTop = validatedRepRangeTop)
            is MyoRepSet -> customSet.copy(repRangeTop = validatedRepRangeTop)
            is DropSet -> customSet.copy(repRangeTop = validatedRepRangeTop)
            else -> throw Exception("${customSet::class.simpleName} is not defined")
        }
        updateCustomLiftSetUseCase(updatedSet)
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
    
    private fun getCurrentWorkoutAndLogIfNull(): Workout? {
        val workout = _state.value.workout
        if (workout == null) {
            emitUserMessage("Workout not initialized!")
            val exception = Exception("Workout not initialized!")
            Log.e(TAG, "Workout was not initialized", exception)
            FirebaseCrashlytics.getInstance().recordException(exception)
        }
        
        return workout
    }
    
    private inline fun<reified T: GenericWorkoutLift> getWorkoutLiftAndLogIfNull(workoutLiftId: Long): T? {
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

    private inline fun<reified T: GenericLiftSet> getCustomLiftSetAndLogIfNull(workoutLiftId: Long, position: Int): T? {
        val workoutLift = getWorkoutLiftAndLogIfNull<CustomWorkoutLift>(workoutLiftId) ?: return null
        val customSet = safeGetCustomSetAtPositionAndLogIfNull<T>(workoutLift.customLiftSets, position) ?: return null
        return customSet
    }

    private inline fun<reified T: GenericLiftSet> safeGetCustomSetAtPositionAndLogIfNull(customLiftSets: List<GenericLiftSet>, position: Int): T? {
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