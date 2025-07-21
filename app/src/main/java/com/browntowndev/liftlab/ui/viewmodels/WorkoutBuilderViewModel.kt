package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.ReorderableListItem
import com.browntowndev.liftlab.core.common.Utils.StepSize.Companion.generateFirstCompleteStepSequence
import com.browntowndev.liftlab.core.common.Utils.StepSize.Companion.getPossibleStepSizes
import com.browntowndev.liftlab.core.common.Utils.StepSize.Companion.getRecalculatedStepSizeForLift
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.domain.models.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.DropSet
import com.browntowndev.liftlab.core.domain.models.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.MyoRepSet
import com.browntowndev.liftlab.core.domain.models.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.StandardSet
import com.browntowndev.liftlab.core.domain.models.StandardSetResult
import com.browntowndev.liftlab.core.domain.models.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.Workout
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.standard.CustomLiftSetsRepositoryImpl
import com.browntowndev.liftlab.core.domain.repositories.standard.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.standard.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.domain.repositories.standard.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.standard.WorkoutInProgressRepositoryImpl
import com.browntowndev.liftlab.core.domain.repositories.standard.WorkoutLiftsRepositoryImpl
import com.browntowndev.liftlab.core.domain.repositories.standard.WorkoutsRepositoryImpl
import com.browntowndev.liftlab.ui.viewmodels.states.PickerState
import com.browntowndev.liftlab.ui.viewmodels.states.PickerType
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutBuilderState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import kotlin.time.Duration

class WorkoutBuilderViewModel(
    private val workoutId: Long,
    private val onNavigateBack: () -> Unit,
    private val programsRepository: ProgramsRepository,
    private val workoutsRepositoryImpl: WorkoutsRepositoryImpl,
    private val workoutLiftsRepositoryImpl: WorkoutLiftsRepositoryImpl,
    private val customLiftSetsRepositoryImpl: CustomLiftSetsRepositoryImpl,
    private val liftsRepository: LiftsRepository,
    private val liftLevelDeloadsEnabled: Boolean,
    private val workoutInProgressRepositoryImpl: WorkoutInProgressRepositoryImpl,
    private val setResultsRepository: PreviousSetResultsRepository,
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
        viewModelScope.launch {
            workoutsRepositoryImpl.getFlow(workoutId)
                .collect { workout ->
                    Log.d(TAG, "workoutEntity=$workout")
                    _state.update { currentState ->
                        val programDeloadWeek = if (workout != null && workout.programId != _state.value.workout?.programId) {
                            programsRepository.getDeloadWeek(workout.programId)
                        } else _state.value.programDeloadWeek

                        currentState.copy(
                            workout = workout,
                            programDeloadWeek = programDeloadWeek,
                            workoutLiftStepSizeOptions = workout?.let {
                                getRecalculatedWorkoutLiftStepSizeOptions(
                                    workout = workout,
                                    programDeloadWeek = programDeloadWeek!!)
                            } ?: mapOf(),
                        )
                    }
                }
        }
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

    private fun getRecalculatedWorkoutLiftStepSizeOptions(workout: Workout, programDeloadWeek: Int): Map<Long, Map<Int, List<Int>>> {
        return workout.lifts
            .filterIsInstance<StandardWorkoutLift>()
            .filter { it.progressionScheme == ProgressionScheme.WAVE_LOADING_PROGRESSION }
            .associate { workoutLift ->
                workoutLift.id to getPossibleStepSizes(
                    repRangeTop = workoutLift.repRangeTop,
                    repRangeBottom = workoutLift.repRangeBottom,
                    stepCount = (if (liftLevelDeloadsEnabled) workoutLift.deloadWeek else programDeloadWeek)?.let { it - 2 }
                ).associateWith { option ->
                    generateFirstCompleteStepSequence(
                        repRangeTop = workoutLift.repRangeTop,
                        repRangeBottom = workoutLift.repRangeBottom,
                        stepSize = option
                    )
                }
            }
    }

    fun toggleMovementPatternDeletionModal(workoutLiftId: Long? = null) {
        _state.update {
            it.copy(workoutLiftIdToDelete = workoutLiftId)
        }
    }

    fun deleteMovementPattern() {
        val liftToDelete = _state.value.workout?.lifts?.find { it.id == _state.value.workoutLiftIdToDelete }
        if (liftToDelete != null) {
            executeInTransactionScope {
                workoutLiftsRepositoryImpl.delete(liftToDelete)
                _state.update { currentState ->
                    val mutableLifts = currentState.workout!!.lifts.toMutableList()
                    mutableLifts.remove(liftToDelete)
                    currentState.copy(
                        workout = currentState.workout.copy(
                            lifts = mutableLifts.toList()
                        ),
                        workoutLiftIdToDelete = null,
                    )
                }
            }
        }
    }

    fun updateWorkoutName(newName: String) {
        if (_state.value.workout != null) {
            executeInTransactionScope {
                workoutsRepositoryImpl.updateName(_state.value.workout!!.id, newName)
                _state.update {
                    it.copy(workout = it.workout?.copy(name = newName), isEditingName = false)
                }
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

    fun toggleHasCustomLiftSets(workoutLiftId: Long, enableCustomSets: Boolean) {
        executeInTransactionScope {
            var updatedStateCopy = if (enableCustomSets) {
                createCustomSets(_state.value, workoutLiftId)
            } else {
                _state.value.copy(workout = _state.value.workout!!.copy(
                    lifts = _state.value.workout!!.lifts.map { lift ->
                        if (lift.id == workoutLiftId) {
                            val topCustomLiftSet = (lift as CustomWorkoutLift).customLiftSets.firstOrNull()
                            val standardWorkoutLift = StandardWorkoutLift(
                                id = lift.id,
                                workoutId = lift.workoutId,
                                liftId = lift.liftId,
                                liftName = lift.liftName,
                                liftMovementPattern = lift.liftMovementPattern,
                                liftVolumeTypes = lift.liftVolumeTypes,
                                liftSecondaryVolumeTypes = lift.liftSecondaryVolumeTypes,
                                deloadWeek = lift.deloadWeek,
                                liftNote = null,
                                position = lift.position,
                                setCount = lift.setCount,
                                repRangeBottom = topCustomLiftSet?.repRangeBottom ?: 8,
                                repRangeTop = topCustomLiftSet?.repRangeTop ?: 10,
                                rpeTarget = topCustomLiftSet?.rpeTarget ?: 8f,
                                incrementOverride = null,
                                restTime = lift.restTime,
                                restTimerEnabled = lift.restTimerEnabled,
                                progressionScheme = lift.progressionScheme,
                            )
                            workoutLiftsRepositoryImpl.update(standardWorkoutLift)
                            standardWorkoutLift
                        } else lift
                    })
                )
            }
            if (!enableCustomSets) {
                customLiftSetsRepositoryImpl.deleteAllForLift(workoutLiftId)
            } else {
                val lift = (updatedStateCopy.workout!!.lifts.find { it.id == workoutLiftId } as CustomWorkoutLift)
                val newIds = customLiftSetsRepositoryImpl.insertMany(lift.customLiftSets)
                val liftCopy = lift.copy(customLiftSets = newIds.mapIndexed { index, id ->
                    when (val currCustomSet = lift.customLiftSets[index]) {
                        is StandardSet -> currCustomSet.copy(id = id)
                        is MyoRepSet -> currCustomSet.copy(id = id)
                        is DropSet -> currCustomSet.copy(id = id)
                        else -> currCustomSet
                    }
                })
                updatedStateCopy = _state.value.copy(workout = _state.value.workout!!.copy(
                    lifts = _state.value.workout!!.lifts.fastMap {
                        if (it.id == liftCopy.id) liftCopy
                        else it
                    }
                ))
            }
            _state.update { updatedStateCopy }
        }
    }

    private suspend fun createCustomSets(state: WorkoutBuilderState, workoutLiftId: Long): WorkoutBuilderState {
        val workoutCopy = state.workout?.let {
            val liftsWithCustomSetsCopy: List<GenericWorkoutLift> = it.lifts.map { lift ->
                if (lift.id == workoutLiftId && lift is StandardWorkoutLift) {
                    val customSets = mutableListOf<GenericLiftSet>()
                    for (i in 0 until lift.setCount) {
                        customSets.add(
                            StandardSet(
                                workoutLiftId = workoutLiftId,
                                position = i,
                                rpeTarget = lift.rpeTarget,
                                repRangeBottom = lift.repRangeBottom,
                                repRangeTop = lift.repRangeTop
                            )
                        )
                    }

                    val customWorkoutLift = CustomWorkoutLift(
                        id = lift.id,
                        workoutId = lift.workoutId,
                        liftId = lift.liftId,
                        liftName = lift.liftName,
                        liftMovementPattern = lift.liftMovementPattern,
                        liftVolumeTypes = lift.liftVolumeTypes,
                        liftSecondaryVolumeTypes = lift.liftSecondaryVolumeTypes,
                        position = lift.position,
                        setCount = lift.setCount,
                        progressionScheme = lift.progressionScheme,
                        deloadWeek = lift.deloadWeek,
                        liftNote = null,
                        incrementOverride = lift.incrementOverride,
                        restTime = lift.restTime,
                        restTimerEnabled = lift.restTimerEnabled,
                        customLiftSets = customSets
                    )
                    workoutLiftsRepositoryImpl.update(customWorkoutLift)
                    customWorkoutLift
                }
                else if (lift is CustomWorkoutLift) {
                    lift.copy()
                }
                else if (lift is StandardWorkoutLift) {
                    lift.copy()
                }
                else {
                    throw Exception("${lift::class} is not defined.")
                }
            }

            it.copy(lifts = liftsWithCustomSetsCopy)
        }

        return if (workoutCopy != null) state.copy(workout = workoutCopy) else state
    }

    fun setRestTime(workoutLiftId: Long, newRestTime: Duration, enabled: Boolean) {
        executeInTransactionScope {
            _state.update { currentState ->
                currentState.copy(
                    workout = currentState.workout!!.copy(
                        lifts = currentState.workout.lifts.fastMap { lift ->
                            if (lift.id == workoutLiftId) {
                                val workoutLiftCopy = when (lift) {
                                    is StandardWorkoutLift -> {
                                        lift.copy(restTime = newRestTime, restTimerEnabled = enabled)
                                    }
                                    is CustomWorkoutLift -> {
                                        lift.copy(restTime = newRestTime, restTimerEnabled = enabled)
                                    }
                                    else -> throw Exception("${lift::class.simpleName} is not defined.")
                                }
                                liftsRepository.updateRestTime(
                                    id = workoutLiftCopy.liftId,
                                    enabled = enabled,
                                    newRestTime = newRestTime
                                )
                                workoutLiftCopy
                            }
                            else lift
                        }
                    )
                )
            }
        }
    }

    fun setIncrementOverride(workoutLiftId: Long, newIncrement: Float) {
        executeInTransactionScope {
            _state.update { currentState ->
                currentState.copy(
                    workout = currentState.workout!!.copy(
                        lifts = currentState.workout.lifts.fastMap { lift ->
                            if (lift.id == workoutLiftId) {
                                val workoutLiftCopy = when (lift) {
                                    is StandardWorkoutLift -> {
                                        lift.copy(incrementOverride = newIncrement)
                                    }
                                    is CustomWorkoutLift -> {
                                        lift.copy(incrementOverride = newIncrement)
                                    }
                                    else -> throw Exception("${lift::class.simpleName} is not defined.")
                                }

                                liftsRepository.updateIncrementOverride(
                                    id = lift.liftId,
                                    newIncrement = newIncrement,
                                )
                                workoutLiftCopy
                            }
                            else lift
                        }
                    )
                )
            }
        }
    }

    fun reorderLifts(newLiftOrder: List<ReorderableListItem>) {
        executeInTransactionScope {
            val newWorkoutLiftIndices = newLiftOrder
                .mapIndexed { index, item -> item.key to index }
                .associate { it.first to it.second }

            val updatedWorkoutCopy = _state.value.workout!!.let { workout ->
                workout.copy(
                    lifts = workout.lifts.fastMap { lift ->
                        when(lift) {
                            is StandardWorkoutLift -> lift.copy(position = newWorkoutLiftIndices[lift.id]!!)
                            is CustomWorkoutLift -> lift.copy(position = newWorkoutLiftIndices[lift.id]!!)
                            else -> throw Exception("${lift::class.simpleName} is not defined.")
                        }
                    }.sortedBy { it.position }
                )
            }

            workoutLiftsRepositoryImpl.updateMany(updatedWorkoutCopy.lifts)

            if (workoutInProgressRepositoryImpl.getWithoutCompletedSets() != null) {
                programsRepository.getActive()?.let { programMetadata ->
                    val workoutLiftIdByLiftId = _state.value.workout!!.lifts.associate { it.liftId to it.id }
                    val updatedInProgressSetResults = setResultsRepository.getForWorkout(
                        workoutId = workoutId,
                        mesoCycle = programMetadata.currentMesocycle,
                        microCycle = programMetadata.currentMicrocycle,
                    ).map { completedSet ->
                        val workoutLiftIdOfCompletedSet = workoutLiftIdByLiftId[completedSet.liftId]
                        when (completedSet) {
                            is StandardSetResult -> completedSet.copy(liftPosition = newWorkoutLiftIndices[workoutLiftIdOfCompletedSet]!!)
                            is MyoRepSetResult -> completedSet.copy(liftPosition = newWorkoutLiftIndices[workoutLiftIdOfCompletedSet]!!)
                            is LinearProgressionSetResult -> completedSet.copy(liftPosition = newWorkoutLiftIndices[workoutLiftIdOfCompletedSet]!!)
                            else -> throw Exception("${completedSet::class.simpleName} is not defined.")
                        }
                    }

                    setResultsRepository.upsertMany(updatedInProgressSetResults)
                }
            }

            _state.update { it.copy(workout = updatedWorkoutCopy, isReordering = false) }
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

    fun updateDeloadWeek(workoutLiftId: Long, newDeloadWeek: Int?) {
        executeInTransactionScope {
            var updatedWorkoutLift: GenericWorkoutLift? = null
            val updatedWorkout = updateLiftProperty(_state.value, workoutLiftId) { lift ->
                updatedWorkoutLift = when(lift) {
                    is StandardWorkoutLift -> lift.copy(
                        deloadWeek = newDeloadWeek,
                        stepSize = getRecalculatedStepSizeForLift(
                            currStepSize = lift.stepSize,
                            repRangeTop = lift.repRangeTop,
                            repRangeBottom = lift.repRangeBottom,
                            deloadWeek = newDeloadWeek,
                            progressionScheme = lift.progressionScheme,
                        )
                    )
                    is CustomWorkoutLift -> lift.copy(deloadWeek = newDeloadWeek)
                    else -> throw Exception("${lift::class.simpleName} not recognized.")
                }
                updatedWorkoutLift
            }

            if (updatedWorkoutLift != null) {
                workoutLiftsRepositoryImpl.update(updatedWorkoutLift)
                _state.update {
                    it.copy(
                        workout = updatedWorkout,
                        workoutLiftStepSizeOptions = getRecalculatedWorkoutLiftStepSizeOptions(updatedWorkout, it.programDeloadWeek!!),
                    )
                }
            }
        }
    }

    fun setLiftSetCount(workoutLiftId: Long, newSetCount: Int) {
        executeInTransactionScope {
            var updatedWorkoutLift: GenericWorkoutLift? = null
            val updatedStateCopy = _state.value.copy(
                workout = updateLiftProperty(_state.value, workoutLiftId) { lift ->
                    updatedWorkoutLift = when (lift) {
                        is StandardWorkoutLift -> lift.copy(setCount = newSetCount)
                        is CustomWorkoutLift -> lift.copy(setCount = newSetCount)
                        else -> throw Exception("${lift::class.simpleName} cannot have a set count.")
                    }
                    updatedWorkoutLift
                }
            )

            if (updatedWorkoutLift != null) {
                workoutLiftsRepositoryImpl.update(updatedWorkoutLift)
                _state.update { updatedStateCopy }
            }
        }
    }

    fun setLiftRpeTarget(workoutLiftId: Long, newRpeTarget: Float) {
        executeInTransactionScope {
            var updatedWorkoutLift: GenericWorkoutLift? = null
            val updatedStateCopy = _state.value.copy(
                workout = updateLiftProperty(_state.value, workoutLiftId) {
                    updatedWorkoutLift = when (it) {
                        is StandardWorkoutLift -> it.copy(rpeTarget = newRpeTarget)
                        else -> throw Exception("${it::class.simpleName} cannot have an RPE target.")
                    }
                    updatedWorkoutLift
                }
            )

            if(updatedWorkoutLift != null) {
                workoutLiftsRepositoryImpl.update(updatedWorkoutLift)
                _state.update { updatedStateCopy }
            }
        }
    }

    fun setLiftProgressionScheme(workoutLiftId: Long, newProgressionScheme: ProgressionScheme) {
        executeInTransactionScope {
            var updatedWorkoutLift: GenericWorkoutLift? = null
            val updatedWorkout = updateLiftProperty(_state.value, workoutLiftId) { lift ->
                updatedWorkoutLift = when (lift) {
                    is StandardWorkoutLift -> lift.copy(
                        progressionScheme = newProgressionScheme,
                        stepSize = getRecalculatedStepSizeForLift(
                            currStepSize = lift.stepSize,
                            repRangeTop = lift.repRangeTop,
                            repRangeBottom = lift.repRangeBottom,
                            deloadWeek = lift.deloadWeek ?: getProgramDeloadWeekAndLogIfNull(),
                            progressionScheme = newProgressionScheme,
                        )
                    )
                    is CustomWorkoutLift -> lift.copy(progressionScheme = newProgressionScheme)
                    else -> throw Exception("${lift::class.simpleName} cannot have an RPE target.")
                }
                updatedWorkoutLift
            }

            if (updatedWorkoutLift != null) {
                workoutLiftsRepositoryImpl.update(updatedWorkoutLift)
                _state.update {
                    it.copy(
                        workout = updatedWorkout,
                        workoutLiftStepSizeOptions = getRecalculatedWorkoutLiftStepSizeOptions(updatedWorkout, it.programDeloadWeek!!),
                    )
                }
            }
        }
    }

    fun updateStepSize(workoutLiftId: Long, newStepSize: Int) {
        executeInTransactionScope {
            var updatedWorkoutLift: GenericWorkoutLift? = null
            val updatedWorkout = updateLiftProperty(_state.value, workoutLiftId) { lift ->
                updatedWorkoutLift = when (lift) {
                    is StandardWorkoutLift -> lift.copy(stepSize = newStepSize)
                    else -> throw Exception("${lift::class.simpleName} cannot have an RPE target.")
                }
                updatedWorkoutLift
            }

            if (updatedWorkoutLift != null) {
                workoutLiftsRepositoryImpl.update(updatedWorkoutLift)
                _state.update {
                    it.copy(
                        workout = updatedWorkout,
                        workoutLiftStepSizeOptions = getRecalculatedWorkoutLiftStepSizeOptions(updatedWorkout, it.programDeloadWeek!!),
                    )
                }
            }
        }
    }

    fun addSet(workoutLiftId: Long) {
        //TODO: This is a mess
        executeInTransactionScope {
            var addedSet: StandardSet? = null
            val updatedWorkout = _state.value.workout!!.let { workout ->
                workout.copy(
                    lifts = workout.lifts.map { lift ->
                        if (lift.id == workoutLiftId) {
                            if (lift !is CustomWorkoutLift) throw Exception("Cannot add set to non-custom liftEntity.")
                            addedSet = StandardSet(
                                workoutLiftId = lift.id,
                                position = lift.customLiftSets.size,
                                rpeTarget = 8f,
                                repRangeBottom = 8,
                                repRangeTop = 10
                            ).let { newSet ->
                                val newSetId = customLiftSetsRepositoryImpl.insert(newSet)
                                newSet.copy(id = newSetId)
                            }

                            lift.copy(
                                customLiftSets = lift.customLiftSets + addedSet,
                                setCount = lift.setCount + 1
                            ).also { workoutLiftsRepositoryImpl.update(it) }
                        } else lift
                    }
                )
            }

            if (addedSet != null) {
                _state.update { currentState ->
                    currentState.copy(
                        workout = updatedWorkout,
                        detailExpansionStates = HashMap(currentState.detailExpansionStates.apply {
                            val setStates = getOrPut(workoutLiftId) { hashSetOf() }
                            setStates.add(addedSet.position)
                        }),
                    )
                }
            }
        }
    }

    fun deleteSet(workoutLiftId: Long, position: Int) {
        val originalWorkout = getCurrentWorkoutAndLogIfNull() ?: return
        _state.update { currentState ->
            currentState.copy(
                workout = currentState.workout!!.copy(
                    lifts = currentState.workout.lifts.fastMap { workoutLift ->
                        if(workoutLift.id == workoutLiftId) {
                            (workoutLift as CustomWorkoutLift).copy(
                                setCount = workoutLift.setCount - 1,
                                customLiftSets = workoutLift.customLiftSets
                                    .filter { it.position != position }
                                    .mapIndexed { index, customSet ->
                                        when (customSet) {
                                            is StandardSet -> customSet.copy(position = index)
                                            is MyoRepSet -> customSet.copy(position = index)
                                            is DropSet -> customSet.copy(position = index)
                                            else -> throw Exception("${customSet::class.simpleName} is not defined.")
                                        }
                                    }
                            )
                        } else workoutLift
                    }
                )
            )
        }

        safeExecute("delete set", originalWorkout) {
            customLiftSetsRepositoryImpl.deleteByPosition(workoutLiftId, position)
        }
    }

    fun setCustomSetRpeTarget(workoutLiftId: Long, position: Int, newRpeTarget: Float) {
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
                customLiftSetsRepositoryImpl.update(updatedSet)
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
                customLiftSetsRepositoryImpl.update(updatedSet)
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
                customLiftSetsRepositoryImpl.update(updatedSet)
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
                customLiftSetsRepositoryImpl.update(updatedSet)
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
                customLiftSetsRepositoryImpl.update(updatedSet)
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
                customLiftSetsRepositoryImpl.update(updatedSet)
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
                customLiftSetsRepositoryImpl.update(updatedSet)
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
            showToast("Failed to update set!")
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
                    workoutLiftStepSizeOptions = getRecalculatedWorkoutLiftStepSizeOptions(
                        updatedWorkout,
                        it.programDeloadWeek!!
                    ),
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
                    workoutLiftStepSizeOptions = getRecalculatedWorkoutLiftStepSizeOptions(
                        updatedWorkout,
                        it.programDeloadWeek!!
                    ),
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

        safeExecute("update liftEntity", originalWorkout) {
            workoutLiftsRepositoryImpl.update(workoutLift)
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
        safeExecute("update liftEntity", originalWorkout) {
            workoutLiftsRepositoryImpl.update(workoutLift)
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
        var updatedSet: GenericLiftSet? = null
        _state.update { currentState ->
            currentState.copy(
                workout = updateCustomSetProperty(_state.value, workoutLiftId, position) { set ->
                    updatedSet = when (set) {
                        is StandardSet -> set.copy(repRangeBottom = newRepRangeBottom)
                        is DropSet -> set.copy(repRangeBottom = newRepRangeBottom)
                        is MyoRepSet -> set.copy(repRangeBottom = newRepRangeBottom)
                        else -> throw Exception("${set::class.simpleName} cannot have a bottom rep range.")
                    }
                    updatedSet
                }
            )
        }
    }

    fun setCustomSetRepRangeTop(workoutLiftId: Long, position: Int, newRepRangeTop: Int) {
        getCurrentWorkoutAndLogIfNull() ?: return
        var updatedSet: GenericLiftSet? = null
        _state.update { currentState ->
            currentState.copy(
                workout = updateCustomSetProperty(_state.value, workoutLiftId, position) { set ->
                    updatedSet = when (set) {
                        is StandardSet -> set.copy(repRangeTop = newRepRangeTop)
                        is DropSet -> set.copy(repRangeTop = newRepRangeTop)
                        is MyoRepSet -> set.copy(repRangeTop = newRepRangeTop)
                        else -> throw Exception("${set::class.simpleName} cannot have a top rep range.")
                    }
                    updatedSet
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

        val updatedSet = safeCopy({
            when (customSet) {
                is StandardSet -> customSet.copy(repRangeBottom = validatedRepRangeBottom)
                is MyoRepSet -> customSet.copy(repRangeBottom = validatedRepRangeBottom)
                is DropSet -> customSet.copy(repRangeBottom = validatedRepRangeBottom)
                else -> throw Exception("${customSet::class.simpleName} is not defined")
            }
        }) ?: return

        updateStateWithCustomSet(
            workoutLiftId = workoutLiftId,
            workoutLift = workoutLift,
            customSet = updatedSet
        )

        safeExecute("update set", originalWorkout) {
            customLiftSetsRepositoryImpl.update(updatedSet)
        }
    }

    fun confirmCustomSetRepRangeTop(workoutLiftId: Long, position: Int) {
        val originalWorkout = getCurrentWorkoutAndLogIfNull() ?: return
        val workoutLift = getWorkoutLiftAndLogIfNull<CustomWorkoutLift>(workoutLiftId) ?: return
        val customSet = safeGetCustomSetAtPositionAndLogIfNull(customLiftSets = workoutLift.customLiftSets, position) ?: return
        val validatedRepRangeTop = getValidatedRepRangeTop(
            newRepRangeTop = customSet.repRangeTop,
            repRangeBottom = customSet.repRangeBottom)

        val updatedSet = safeCopy({
            when (customSet) {
                is StandardSet -> customSet.copy(repRangeTop = validatedRepRangeTop)
                is MyoRepSet -> customSet.copy(repRangeTop = validatedRepRangeTop)
                is DropSet -> customSet.copy(repRangeTop = validatedRepRangeTop)
                else -> throw Exception("${customSet::class.simpleName} is not defined")
            }
        }) ?: return

        updateStateWithCustomSet(
            workoutLiftId = workoutLiftId,
            workoutLift = workoutLift,
            customSet = updatedSet
        )

        safeExecute("update set", originalWorkout) {
            customLiftSetsRepositoryImpl.update(updatedSet)
        }
    }

    private fun safeCopy(copy: () -> GenericLiftSet): GenericLiftSet? {
        try {
            return copy()
        } catch (e: Exception) {
            showToast("Failed to update set!")
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
            showToast("Failed to $actionName!")
            Log.e(TAG, "Failed $actionName: ${e.message}", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }
    
    private fun getCurrentWorkoutAndLogIfNull(): Workout? {
        val workout = _state.value.workout
        if (workout == null) {
            showToast("WorkoutEntity not initialized!")
            val exception = Exception("WorkoutEntity not initialized!")
            Log.e(TAG, "WorkoutEntity was not initialized", exception)
            FirebaseCrashlytics.getInstance().recordException(exception)
        }
        
        return workout
    }
    
    private inline fun<reified T: GenericWorkoutLift> getWorkoutLiftAndLogIfNull(workoutLiftId: Long): T? {
        val workoutLift = _state.value.workout?.lifts
            ?.find { it.id == workoutLiftId } as? T

        if (workoutLift == null) {
            showToast("Must be standard workoutEntity liftEntity.")
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
            showToast("Using default deload week of $DEFAULT_PROGRAM_DELOAD_WEEK")
            val exception = Exception("ProgramEntity deload week not initialized!")
            Log.e(TAG, "ProgramEntity deload week was not initialized", exception)
            FirebaseCrashlytics.getInstance().recordException(exception)
            DEFAULT_PROGRAM_DELOAD_WEEK
        }
    }

    private fun safeGetCustomSetAtPositionAndLogIfNull(customLiftSets: List<GenericLiftSet>, position: Int): GenericLiftSet? {
        if (customLiftSets.size > position) {
            return customLiftSets[position]
        } else {
            showToast("Custom liftEntity set not found!")
            val exception = Exception("Custom liftEntity position out of bounds. set count=${customLiftSets.size}, position=$position")
            Log.e(TAG, "Custom liftEntity position out of bounds. set count=${customLiftSets.size}, position=$position", exception)
            FirebaseCrashlytics.getInstance().recordException(exception)
            return null
        }
    }
}