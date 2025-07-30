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
import com.browntowndev.liftlab.core.domain.models.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.DropSet
import com.browntowndev.liftlab.core.domain.models.MyoRepSet
import com.browntowndev.liftlab.core.domain.models.StandardSet
import com.browntowndev.liftlab.core.domain.models.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.Workout
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import com.browntowndev.liftlab.core.domain.useCase.workoutBuilder.AddSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutBuilder.ConvertWorkoutLiftTypeUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutBuilder.ReorderWorkoutBuilderLiftsUseCase
import com.browntowndev.liftlab.ui.viewmodels.states.PickerState
import com.browntowndev.liftlab.ui.viewmodels.states.PickerType
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutBuilderState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutBuilderViewModel(
    private val workoutId: Long,
    private val onNavigateBack: () -> Unit,
    private val programsRepository: ProgramsRepository,
    private val workoutsRepository: WorkoutsRepository,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val customLiftSetsRepository: CustomLiftSetsRepository,
    private val liftsRepository: LiftsRepository,
    private val convertWorkoutLiftTypeUseCase: ConvertWorkoutLiftTypeUseCase,
    private val reorderWorkoutBuilderLiftsUseCase: ReorderWorkoutBuilderLiftsUseCase,
    private val addSetUseCase: AddSetUseCase,
    private val liftLevelDeloadsEnabled: Boolean,
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
        workoutsRepository.getFlow(workoutId)
            .distinctUntilChanged()
            .map { workout ->
                Log.d(TAG, "workoutEntity=$workout")

                WorkoutBuilderState(
                    workout = workout,
                )
            }.scan(WorkoutBuilderState()) { oldState, newState ->
                val programDeloadWeek =
                    if (newState.workout != null && newState.workout.programId != oldState.workout?.programId) {
                        programsRepository.getDeloadWeek(newState.workout.programId)
                    } else oldState.programDeloadWeek

                WorkoutBuilderState(
                    workout = newState.workout,
                    programDeloadWeek = programDeloadWeek,
                    workoutLiftStepSizeOptions = newState.workout?.getRecalculatedWorkoutLiftStepSizeOptions(
                        programDeloadWeek = programDeloadWeek!!,
                        liftLevelDeloadsEnabled = liftLevelDeloadsEnabled,
                    ) ?: mapOf()
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
            }
            .catch {
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
        val liftToDelete = _state.value.workout?.lifts?.find { it.id == _state.value.workoutLiftIdToDelete }
        if (liftToDelete != null) {
            executeInTransactionScope {
                workoutLiftsRepository.delete(liftToDelete)
            }
        }
    }

    fun updateWorkoutName(newName: String) = executeWithErrorHandling("Failed to update workout name") {
        if (_state.value.workout != null) {
            executeInTransactionScope {
                workoutsRepository.updateName(_state.value.workout!!.id, newName)
            }
        }
    }

    fun toggleWorkoutRenameModal() {
        _state.update { it.copy(isEditingName = !it.isEditingName) }
    }

    fun toggleReorderLifts() {
        _state.update { it.copy(isReordering = !_state.value.isReordering) }
    }

    fun togglePicker(
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

    fun toggleHasCustomLiftSets(workoutLiftId: Long, enableCustomSets: Boolean) =
        executeWithErrorHandling("Failed to toggle custom lift sets") {
            executeInTransactionScope {
                val workoutLiftToConvert = _state.value.workout!!.lifts.find { it.id == workoutLiftId }!!
                convertWorkoutLiftTypeUseCase(workoutLiftToConvert, enableCustomSets)
            }
        }

    fun setRestTime(workoutLiftId: Long, newRestTime: Duration, enabled: Boolean) =
        executeWithErrorHandling("Failed to update rest time") {
            executeInTransactionScope {
                val workoutLift = _state.value.workout!!.lifts.find { it.id == workoutLiftId }!!
                liftsRepository.updateRestTime(
                    id = workoutLift.liftId,
                    enabled = enabled,
                    newRestTime = newRestTime
                )
            }
        }

    fun setIncrementOverride(workoutLiftId: Long, newIncrement: Float) =
        executeWithErrorHandling("Failed to update increment override") {
            executeInTransactionScope {
                val workoutLift = _state.value.workout!!.lifts.find { it.id == workoutLiftId }!!
                liftsRepository.updateIncrementOverride(
                    id = workoutLift.liftId,
                    newIncrement = newIncrement,
                )
            }
        }

    fun reorderLifts(newLiftOrder: List<ReorderableListItem>) = executeWithErrorHandling("Failed to reorder lifts") {
        executeInTransactionScope {
            val newWorkoutLiftIndices = newLiftOrder
                .mapIndexed { index, item -> item.key to index }
                .associate { it.first to it.second }

            reorderWorkoutBuilderLiftsUseCase(
                workoutId = workoutId,
                workoutLifts = _state.value.workout!!.lifts,
                newWorkoutLiftIndices = newWorkoutLiftIndices)
        }
    }

    private fun updateLiftProperty(
        currentState: WorkoutBuilderState, 
        workoutLiftId: Long, 
        copyLift: (GenericWorkoutLift) -> GenericWorkoutLift
    ): Workout {
        return currentState.workout!!.let { workout ->
            workout.copy(lifts = workout.lifts.fastMap { currentWorkoutLift ->
                if (currentWorkoutLift.id == workoutLiftId) copyLift(currentWorkoutLift)
                else currentWorkoutLift
            })
        }
    }

    fun updateDeloadWeek(workoutLiftId: Long, newDeloadWeek: Int?) = executeWithErrorHandling("Failed to update deload week") {
        executeInTransactionScope {
            val updatedWorkoutLift = _state.value.workout!!.lifts.find { it.id == workoutLiftId }!!.let { lift ->
                when (lift) {
                    is StandardWorkoutLift -> lift.copy(deloadWeek = newDeloadWeek)
                    is CustomWorkoutLift -> lift.copy(deloadWeek = newDeloadWeek)
                    else -> throw Exception("${lift::class.simpleName} not recognized.")
                }
            }
            workoutLiftsRepository.update(updatedWorkoutLift)
        }
    }

    fun setLiftSetCount(workoutLiftId: Long, newSetCount: Int) = executeWithErrorHandling("Failed to update set count") {
        executeInTransactionScope {
            val updatedWorkoutLift = _state.value.workout!!.lifts.find { it.id == workoutLiftId }!!.let { lift ->
                when (lift) {
                    is StandardWorkoutLift -> lift.copy(setCount = newSetCount)
                    is CustomWorkoutLift -> lift.copy(setCount = newSetCount)
                    else -> throw Exception("${lift::class.simpleName} not recognized.")
                }
            }
            workoutLiftsRepository.update(updatedWorkoutLift)
        }
    }

    fun setLiftRpeTarget(workoutLiftId: Long, newRpeTarget: Float) = executeWithErrorHandling("Failed to update RPE target") {
        executeInTransactionScope {
            val updatedWorkoutLift = _state.value.workout!!.lifts.find { it.id == workoutLiftId }!!.let { lift ->
                when (lift) {
                    is StandardWorkoutLift -> lift.copy(rpeTarget = newRpeTarget)
                    else -> throw Exception("${lift::class.simpleName} cannot have RPE target.")
                }
            }
            workoutLiftsRepository.update(updatedWorkoutLift)
        }
    }

    fun setLiftProgressionScheme(workoutLiftId: Long, newProgressionScheme: ProgressionScheme) = executeWithErrorHandling("Failed to update progression scheme") {
        executeInTransactionScope {
            val updatedWorkoutLift = _state.value.workout!!.lifts.find { it.id == workoutLiftId }!!.let { lift ->
                when (lift) {
                    is StandardWorkoutLift -> lift.copy(progressionScheme = newProgressionScheme)
                    is CustomWorkoutLift -> lift.copy(progressionScheme = newProgressionScheme)
                    else -> throw Exception("${lift::class.simpleName} not recognized.")
                }
            }
            workoutLiftsRepository.update(updatedWorkoutLift)
        }
    }

    fun updateStepSize(workoutLiftId: Long, newStepSize: Int) = executeWithErrorHandling("Failed to update step size") {
        executeInTransactionScope {
            val updatedWorkoutLift = _state.value.workout!!.lifts.find { it.id == workoutLiftId }!!.let { lift ->
                when (lift) {
                    is StandardWorkoutLift -> lift.copy(stepSize = newStepSize)
                    else -> throw Exception("${lift::class.simpleName} cannot have RPE target.")
                }
            }
            workoutLiftsRepository.update(updatedWorkoutLift)
        }
    }

    fun addSet(workoutLiftId: Long) = executeWithErrorHandling("Failed to add set") {
        executeInTransactionScope {
            addSetUseCase(
                workoutLifts = _state.value.workout!!.lifts,
                workoutLiftId = workoutLiftId,
            )
        }
    }

    fun deleteSet(workoutLiftId: Long, position: Int) = executeWithErrorHandling("Failed to delete set") {
        executeInTransactionScope {
            customLiftSetsRepository.deleteByPosition(workoutLiftId, position)
        }
    }

    fun setCustomSetRpeTarget(workoutLiftId: Long, position: Int, newRpeTarget: Float) = executeWithErrorHandling("Failed to update RPE target") {
        val originalWorkout = getCurrentWorkoutAndLogIfNull() ?: return@executeWithErrorHandling
        var updatedSet: GenericLiftSet? = null
        _state.update { currentState ->
            currentState.copy(
                workout = updateCustomSetProperty(
                    _state.value,
                    workoutLiftId,
                    position
                ) { set ->
                    updatedSet = when (set) {
                        is StandardSet -> set.copy(rpeTarget = newRpeTarget)
                        is DropSet -> set.copy(rpeTarget = newRpeTarget)
                        else -> throw Exception("${set::class.simpleName} cannot have an rpe target.")
                    }
                    updatedSet
                }
            )
        }

        if (updatedSet != null) {
            safeExecute("update set", originalWorkout) {
                customLiftSetsRepository.update(updatedSet)
            }
        }
    }

    fun setCustomSetRepFloor(workoutLiftId: Long, position: Int, newRepFloor: Int) {
        val originalWorkout = getCurrentWorkoutAndLogIfNull() ?: return
        var updatedSet: GenericLiftSet? = null
        _state.update { currentState ->
            currentState.copy(
                workout = updateCustomSetProperty(
                    _state.value,
                    workoutLiftId,
                    position
                ) { set ->
                    updatedSet = when (set) {
                        is MyoRepSet -> set.copy(repFloor = newRepFloor)
                        else -> throw Exception("${set::class.simpleName} cannot have a rep floor.")
                    }
                    updatedSet
                }
            )
        }

        if (updatedSet != null) {
            safeExecute("update set", originalWorkout) {
                customLiftSetsRepository.update(updatedSet)
            }
        }
    }

    fun setCustomSetUseSetMatching(workoutLiftId: Long, position: Int, setMatching: Boolean) {
        val originalWorkout = getCurrentWorkoutAndLogIfNull() ?: return
        var updatedSet: GenericLiftSet? = null
        _state.update { currentState ->
            currentState.copy(
                workout = updateCustomSetProperty(_state.value, workoutLiftId, position) { set ->
                    updatedSet = when (set) {
                        is MyoRepSet -> set.copy(
                            setMatching = setMatching,
                            setGoal = set.setGoal,
                            maxSets = null,
                            repFloor = if (setMatching) null else 5,
                        )
                        else -> throw Exception("${set::class.simpleName} cannot have set matching.")
                    }
                    updatedSet
                }
            )
        }

        if (updatedSet != null) {
            safeExecute("update set", originalWorkout) {
                customLiftSetsRepository.update(updatedSet)
            }
        }
    }

    fun setCustomSetMatchSetGoal(workoutLiftId: Long, position: Int, newMatchSetGoal: Int) {
        val originalWorkout = getCurrentWorkoutAndLogIfNull() ?: return
        var updatedSet: GenericLiftSet? = null
        _state.update { currentState ->
            currentState.copy(
                workout = updateCustomSetProperty(_state.value, workoutLiftId, position) { set ->
                    updatedSet = when (set) {
                        is MyoRepSet -> set.copy(setGoal = newMatchSetGoal)
                        else -> throw Exception("${set::class.simpleName} cannot have a match set goal.")
                    }
                    updatedSet
                }
            )
        }

        if (updatedSet != null) {
            safeExecute("update set", originalWorkout) {
                customLiftSetsRepository.update(updatedSet)
            }
        }
    }

    fun setCustomSetMaxSets(workoutLiftId: Long, position: Int, newMaxSets: Int?) {
        val originalWorkout = getCurrentWorkoutAndLogIfNull() ?: return
        var updatedSet: GenericLiftSet? = null
        _state.update { currentState ->
            currentState.copy(
                workout = updateCustomSetProperty(_state.value, workoutLiftId, position) { set ->
                    updatedSet = when (set) {
                        is MyoRepSet -> set.copy(maxSets = newMaxSets)
                        else -> throw Exception("${set::class.simpleName} cannot have a max set limit.")
                    }
                    updatedSet
                }
            )
        }

        if (updatedSet != null) {
            safeExecute("update set", originalWorkout) {
                customLiftSetsRepository.update(updatedSet)
            }
        }
    }

    fun setCustomSetDropPercentage(workoutLiftId: Long, position: Int, newDropPercentage: Float) {
        val originalWorkout = getCurrentWorkoutAndLogIfNull() ?: return
        var updatedSet: GenericLiftSet? = null
        _state.update { currentState ->
            currentState.copy(
                workout = updateCustomSetProperty(_state.value, workoutLiftId, position) { set ->
                    updatedSet = when (set) {
                        is DropSet -> set.copy(dropPercentage = newDropPercentage)
                        else -> throw Exception("${set::class.simpleName} cannot have a drop percentage.")
                    }
                    updatedSet
                }
            )
        }

        if (updatedSet != null) {
            safeExecute("update set", originalWorkout) {
                customLiftSetsRepository.update(updatedSet)
            }
        }
    }

    fun changeCustomSetType(workoutLiftId: Long, setPosition: Int, newSetType: SetType) {
        val originalWorkout = getCurrentWorkoutAndLogIfNull() ?: return
        var updatedSet: GenericLiftSet? = null
        _state.update { currentState ->
            currentState.copy(
                workout = updateCustomSetProperty(_state.value, workoutLiftId, setPosition) { set ->
                    updatedSet = when (set) {
                        is StandardSet -> if (newSetType != SetType.STANDARD) {
                            transformCustomLiftSet(
                                set,
                                newSetType
                            )
                        } else set

                        is DropSet -> if (newSetType != SetType.DROP_SET) {
                            transformCustomLiftSet(
                                set,
                                newSetType
                            )
                        } else set

                        is MyoRepSet -> if (newSetType != SetType.MYOREP) {
                            transformCustomLiftSet(
                                set,
                                newSetType
                            )
                        } else set

                        else -> throw Exception("${set::class.simpleName} cannot have a drop percentage.")
                    }
                    updatedSet
                },
            )
        }

        if (updatedSet != null) {
            safeExecute("update set", originalWorkout) {
                customLiftSetsRepository.update(updatedSet)
            }
        }
    }
    private fun transformCustomLiftSet(set: GenericLiftSet, newSetType: SetType): GenericLiftSet {
        return when (set) {
            is StandardSet ->
                when (newSetType) {
                    SetType.DROP_SET -> DropSet(
                        id = set.id,
                        workoutLiftId = set.workoutLiftId,
                        position = set.position,
                        dropPercentage = .1f, // TODO: Add a "drop percentage" setting and use it here
                        rpeTarget = set.rpeTarget,
                        repRangeBottom = set.repRangeBottom,
                        repRangeTop = set.repRangeTop,
                    )
                    SetType.MYOREP -> MyoRepSet(
                        id = set.id,
                        workoutLiftId = set.workoutLiftId,
                        position = set.position,
                        repFloor = 5, // TODO: Add a "myo-rep floor" setting and use it here
                        repRangeTop = set.repRangeTop,
                        repRangeBottom = set.repRangeBottom,
                        rpeTarget = set.rpeTarget,
                        setGoal = 3,
                    )
                    SetType.STANDARD -> set
                }
            is MyoRepSet ->
                when (newSetType) {
                    SetType.DROP_SET -> DropSet(
                        id = set.id,
                        workoutLiftId = set.workoutLiftId,
                        position = set.position,
                        dropPercentage = .1f, // TODO: Add a "drop percentage" setting and use it here
                        rpeTarget = 8f, // TODO: Add a "rpe target" setting and use it here
                        repRangeBottom = set.repRangeBottom,
                        repRangeTop = set.repRangeTop,
                    )
                    SetType.MYOREP -> set
                    SetType.STANDARD -> StandardSet(
                        id = set.id,
                        workoutLiftId = set.workoutLiftId,
                        position = set.position,
                        rpeTarget = 8f, // TODO: Add a "rpe target" setting and use it here
                        repRangeBottom = set.repRangeBottom,
                        repRangeTop = set.repRangeTop,
                    )
                }
            is DropSet ->
                when (newSetType) {
                    SetType.DROP_SET -> set
                    SetType.MYOREP -> MyoRepSet(
                        id = set.id,
                        workoutLiftId = set.workoutLiftId,
                        position = set.position,
                        repFloor = 5, // TODO: Add a "myo-rep floor" setting and use it here
                        repRangeBottom = set.repRangeBottom,
                        repRangeTop = set.repRangeTop,
                        rpeTarget = set.rpeTarget,
                        setGoal = 3,
                    )
                    SetType.STANDARD -> StandardSet(
                        id = set.id,
                        workoutLiftId = set.workoutLiftId,
                        position = set.position,
                        rpeTarget = 8f, // TODO: Add a "rpe target" setting and use it here
                        repRangeBottom = set.repRangeBottom,
                        repRangeTop = set.repRangeTop,
                    )
                }
            else -> throw Exception("${set::class.simpleName} is not recognized as a custom set type.")
        }
    }

    private fun updateCustomSetProperty(
        currentState: WorkoutBuilderState,
        workoutLiftId: Long,
        setPosition: Int,
        copyAll: Boolean = false,
        copySet: (GenericLiftSet) -> GenericLiftSet
    ): Workout {
        try {
            return currentState.workout!!.let { workout ->
                workout.copy(lifts = workout.lifts.map { currentWorkoutLift ->
                    if (currentWorkoutLift.id == workoutLiftId) {
                        when (currentWorkoutLift) {
                            is CustomWorkoutLift -> currentWorkoutLift.copy(
                                customLiftSets = currentWorkoutLift.customLiftSets.map { set ->
                                    if (copyAll || set.position == setPosition) copySet(set) else set
                                }
                            )
                            else -> throw Exception("${currentWorkoutLift.liftName} doesn't have custom sets.")
                        }
                    } else {
                        currentWorkoutLift
                    }
                })
            }
        } catch (e: Exception) {
            emitUserMessage("Failed to update set!")
            Log.e(TAG, "Error during updateCustomSetProperty: ${e.message}", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            return currentState.workout!!
        }
    }

    fun setLiftRepRangeBottom(workoutLiftId: Long, newRepRangeBottom: Int) {
        var updatedWorkoutLift: GenericWorkoutLift? = null
        val updatedWorkout = updateLiftProperty(_state.value, workoutLiftId) { lift ->
            updatedWorkoutLift = when (lift) {
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
            updatedWorkoutLift
        }

        if (updatedWorkoutLift != null) {
            _state.update {
                it.copy(
                    workout = updatedWorkout,
                    workoutLiftStepSizeOptions = updatedWorkout.getRecalculatedWorkoutLiftStepSizeOptions(
                        programDeloadWeek = it.programDeloadWeek!!,
                        liftLevelDeloadsEnabled = liftLevelDeloadsEnabled),
                )
            }
        }
    }

    fun setLiftRepRangeTop(workoutLiftId: Long, newRepRangeTop: Int) {
        var updatedWorkoutLift: GenericWorkoutLift? = null
        val updatedWorkout = updateLiftProperty(_state.value, workoutLiftId) { lift ->
            updatedWorkoutLift = when (lift) {
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
            updatedWorkoutLift
        }

        if (updatedWorkoutLift != null) {
            _state.update {
                it.copy(
                    workout = updatedWorkout,
                    workoutLiftStepSizeOptions = updatedWorkout.getRecalculatedWorkoutLiftStepSizeOptions(
                        programDeloadWeek = it.programDeloadWeek!!,
                        liftLevelDeloadsEnabled = liftLevelDeloadsEnabled),
                )
            }
        }
    }

    fun confirmStandardSetRepRangeBottom(workoutLiftId: Long) {
        val originalWorkout = getCurrentWorkoutAndLogIfNull() ?: return
        val originalWorkoutLift = getWorkoutLiftAndLogIfNull<StandardWorkoutLift>(workoutLiftId) ?: return
        
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
        updateStateWithWorkoutLift(workoutLiftId = workoutLiftId, workoutLift = workoutLift)

        safeExecute("update lift", originalWorkout) {
            workoutLiftsRepository.update(workoutLift)
        }
    }

    fun confirmStandardSetRepRangeTop(workoutLiftId: Long) {
        val originalWorkout = getCurrentWorkoutAndLogIfNull() ?: return
        val originalWorkoutLift = getWorkoutLiftAndLogIfNull<StandardWorkoutLift>(workoutLiftId) ?: return
        
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

        updateStateWithWorkoutLift(workoutLiftId = workoutLiftId, workoutLift = workoutLift)
        safeExecute("update lift", originalWorkout) {
            workoutLiftsRepository.update(workoutLift)
        }
    }

    private fun updateStateWithWorkoutLift(
        workoutLiftId: Long,
        workoutLift: StandardWorkoutLift
    ) {
        _state.update { currentState ->
            currentState.copy(
                workout = currentState.workout!!.copy(
                    lifts = currentState.workout.lifts.fastMap { currWorkoutLift ->
                        if (currWorkoutLift.id == workoutLiftId) {
                            workoutLift
                        } else {
                            currWorkoutLift
                        }
                    }
                )
            )
        }
    }

    fun setCustomSetRepRangeBottom(workoutLiftId: Long, position: Int, newRepRangeBottom: Int) {
        getCurrentWorkoutAndLogIfNull() ?: return
        _state.update { currentState ->
            currentState.copy(
                workout = updateCustomSetProperty(_state.value, workoutLiftId, position) { set ->
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

    fun setCustomSetRepRangeTop(workoutLiftId: Long, position: Int, newRepRangeTop: Int) {
        getCurrentWorkoutAndLogIfNull() ?: return
        _state.update { currentState ->
            currentState.copy(
                workout = updateCustomSetProperty(_state.value, workoutLiftId, position) { set ->
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

    fun confirmCustomSetRepRangeBottom(workoutLiftId: Long, position: Int) {
        val originalWorkout = getCurrentWorkoutAndLogIfNull() ?: return
        val workoutLift = getWorkoutLiftAndLogIfNull<CustomWorkoutLift>(workoutLiftId) ?: return
        val customSet = safeGetCustomSetAtPositionAndLogIfNull(workoutLift.customLiftSets, position) ?: return
        val validatedRepRangeBottom = getValidatedRepRangeBottom(
            newRepRangeBottom = customSet.repRangeBottom,
            repRangeTop = customSet.repRangeTop)

        val updatedSet = safeCopy {
            when (customSet) {
                is StandardSet -> customSet.copy(repRangeBottom = validatedRepRangeBottom)
                is MyoRepSet -> customSet.copy(repRangeBottom = validatedRepRangeBottom)
                is DropSet -> customSet.copy(repRangeBottom = validatedRepRangeBottom)
                else -> throw Exception("${customSet::class.simpleName} is not defined")
            }
        } ?: return

        updateStateWithCustomSet(
            workoutLiftId = workoutLiftId,
            workoutLift = workoutLift,
            customSet = updatedSet
        )

        safeExecute("update set", originalWorkout) {
            customLiftSetsRepository.update(updatedSet)
        }
    }

    fun confirmCustomSetRepRangeTop(workoutLiftId: Long, position: Int) {
        val originalWorkout = getCurrentWorkoutAndLogIfNull() ?: return
        val workoutLift = getWorkoutLiftAndLogIfNull<CustomWorkoutLift>(workoutLiftId) ?: return
        val customSet = safeGetCustomSetAtPositionAndLogIfNull(customLiftSets = workoutLift.customLiftSets, position) ?: return
        val validatedRepRangeTop = getValidatedRepRangeTop(
            newRepRangeTop = customSet.repRangeTop,
            repRangeBottom = customSet.repRangeBottom)

        val updatedSet = safeCopy {
            when (customSet) {
                is StandardSet -> customSet.copy(repRangeTop = validatedRepRangeTop)
                is MyoRepSet -> customSet.copy(repRangeTop = validatedRepRangeTop)
                is DropSet -> customSet.copy(repRangeTop = validatedRepRangeTop)
                else -> throw Exception("${customSet::class.simpleName} is not defined")
            }
        } ?: return

        updateStateWithCustomSet(
            workoutLiftId = workoutLiftId,
            workoutLift = workoutLift,
            customSet = updatedSet
        )

        safeExecute("update set", originalWorkout) {
            customLiftSetsRepository.update(updatedSet)
        }
    }

    private fun safeCopy(copy: () -> GenericLiftSet): GenericLiftSet? {
        try {
            return copy()
        } catch (e: Exception) {
            emitUserMessage("Failed to update set!")
            Log.e(TAG, "Failed to update set: ${e.message}", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            return null
        }
    }

    private fun updateStateWithCustomSet(
        workoutLiftId: Long,
        workoutLift: CustomWorkoutLift,
        customSet: GenericLiftSet
    ) {
        _state.update { currentState ->
            currentState.copy(
                workout = currentState.workout!!.copy(
                    lifts = currentState.workout.lifts.fastMap { currWorkoutLift ->
                        if (currWorkoutLift.id == workoutLiftId) {
                            workoutLift.copy(
                                customLiftSets = workoutLift.customLiftSets.fastMap { set ->
                                    if (set.id == customSet.id) customSet else set
                                }
                            )
                        } else {
                            currWorkoutLift
                        }
                    }
                )
            )
        }
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
    
    private fun safeExecute(actionName: String, originalWorkout: Workout, action: suspend () -> Unit) {
        try {
            executeInTransactionScope {
                action()
            }
        } catch (e: Exception) {
            _state.update { it.copy(workout = originalWorkout) }
            emitUserMessage("Failed to $actionName!")
            Log.e(TAG, "Failed $actionName: ${e.message}", e)
            FirebaseCrashlytics.getInstance().recordException(e)
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

    private fun safeGetCustomSetAtPositionAndLogIfNull(customLiftSets: List<GenericLiftSet>, position: Int): GenericLiftSet? {
        if (customLiftSets.size > position) {
            return customLiftSets[position]
        } else {
            emitUserMessage("Custom liftEntity set not found!")
            val exception = Exception("Custom liftEntity position out of bounds. set count=${customLiftSets.size}, position=$position")
            Log.e(TAG, "Custom liftEntity position out of bounds. set count=${customLiftSets.size}, position=$position", exception)
            FirebaseCrashlytics.getInstance().recordException(exception)
            return null
        }
    }
}