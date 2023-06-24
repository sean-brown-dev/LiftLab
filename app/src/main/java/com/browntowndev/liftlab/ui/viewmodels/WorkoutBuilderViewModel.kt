package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.common.ReorderableListItem
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.DropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericCustomLiftSet
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.persistence.repositories.LiftsRepository
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutsRepository
import com.browntowndev.liftlab.ui.viewmodels.states.PickerState
import com.browntowndev.liftlab.ui.viewmodels.states.PickerType
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutBuilderState
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LabScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import kotlin.time.Duration

class WorkoutBuilderViewModel(
    private val workoutId: Long,
    private val navHostController: NavHostController,
    private val programsRepository: ProgramsRepository,
    private val workoutsRepository: WorkoutsRepository,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val customLiftSetsRepository: CustomLiftSetsRepository,
    private val liftsRepository: LiftsRepository,
    private val transactionScope: TransactionScope,
    private val eventBus: EventBus,
): ViewModel() {
    private var _state = MutableStateFlow(WorkoutBuilderState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update {
                val workout = workoutsRepository.get(workoutId)
                val deloadWeek = programsRepository.getDeloadWeek(workout.programId)
                it.copy(
                    workout = workout,
                    programDeloadWeek = deloadWeek
                )
            }
        }
    }

    private suspend fun updateLift(currentState: WorkoutBuilderState, workoutLiftId: Long) {
        val updatedLift = currentState.workout!!.lifts.find { it.id == workoutLiftId }!!
        workoutLiftsRepository.update(updatedLift)
    }
    
    fun registerEventBus() {
        if (!eventBus.isRegistered(this)) {
            eventBus.register(this)
            Log.d(Log.DEBUG.toString(), "Registered event bus for ${this::class.simpleName}")
        }
    }

    @Subscribe
    fun handleActionBarEvents(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.NavigatedBack -> {
                navHostController.popBackStack()
                navHostController.navigate(LabScreen.navigation.route)
            }
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

    fun toggleDeloadWeekModal(workoutLift: GenericWorkoutLift? = null) {
        _state.update {
            it.copy(workoutLiftToChangeDeloadWeek = workoutLift)
        }
    }

    fun updateDeloadWeek(newDeloadWeek: Int) {
        executeInTransactionScope {
            var updatedWorkoutLift: GenericWorkoutLift? = null
            val workoutCopy = _state.value.workout!!.copy(
                lifts = _state.value.workout!!.lifts.fastMap { lift ->
                    if (lift.id == _state.value.workoutLiftToChangeDeloadWeek?.id) {
                        updatedWorkoutLift = when(lift) {
                            is StandardWorkoutLiftDto -> lift.copy(deloadWeek = newDeloadWeek)
                            is CustomWorkoutLiftDto -> lift.copy(deloadWeek = newDeloadWeek)
                            else -> throw Exception("${lift::class.simpleName} not recognized.")
                        }
                        updatedWorkoutLift!!
                    } else lift
                }
            )
            if (updatedWorkoutLift != null) {
                workoutLiftsRepository.update(updatedWorkoutLift!!)
                _state.update {
                    it.copy(workout = workoutCopy, workoutLiftToChangeDeloadWeek = null)
                }
            }
        }
    }

    fun deleteMovementPattern() {
        val liftToDelete = _state.value.workout?.lifts?.find { it.id == _state.value.workoutLiftIdToDelete }
        if (liftToDelete != null) {
            executeInTransactionScope {
                workoutLiftsRepository.delete(liftToDelete)
                _state.update { currentState ->
                    val mutableLifts = currentState.workout!!.lifts.toMutableList()
                    mutableLifts.remove(liftToDelete)
                    currentState.copy(
                        workout = currentState.workout.copy(
                            lifts = mutableLifts.toList()
                        )
                    )
                }
            }
        }
    }

    fun updateWorkoutName(newName: String) {
        if (_state.value.workout != null) {
            executeInTransactionScope {
                workoutsRepository.updateName(_state.value.workout!!.id, newName)
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

    fun togglePicker(visible: Boolean, workoutLiftId: Long, position: Int? = null, type: PickerType) {
        _state.update {
            it.copy(pickerState = if(visible) PickerState(workoutLiftId, position, type) else null)
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
                            val topCustomLiftSet = (lift as CustomWorkoutLiftDto).customLiftSets.firstOrNull()
                            StandardWorkoutLiftDto(
                                id = lift.id,
                                workoutId = lift.workoutId,
                                liftId = lift.liftId,
                                liftName = lift.liftName,
                                liftMovementPattern = lift.liftMovementPattern,
                                liftIncrementOverride = lift.incrementOverride,
                                liftRestTime = lift.restTime,
                                deloadWeek = lift.deloadWeek,
                                position = lift.position,
                                setCount = lift.setCount,
                                repRangeBottom = topCustomLiftSet?.repRangeBottom ?: 8,
                                repRangeTop = topCustomLiftSet?.repRangeTop ?: 10,
                                rpeTarget = if (topCustomLiftSet is StandardSetDto) topCustomLiftSet.rpeTarget else 8f,
                                incrementOverride = null,
                                restTime = lift.restTime,
                                progressionScheme = lift.progressionScheme,
                            )
                        } else lift
                    })
                )
            }

            updateLift(updatedStateCopy, workoutLiftId)
            if (!enableCustomSets) {
                customLiftSetsRepository.deleteAllForLift(workoutLiftId)
            } else {
                val lift = (updatedStateCopy.workout!!.lifts.find { it.id == workoutLiftId } as CustomWorkoutLiftDto)
                val newIds = customLiftSetsRepository.insertAll(lift.customLiftSets)
                val liftCopy = lift.copy(customLiftSets = newIds.mapIndexed { index, id ->
                    when (val currCustomSet = lift.customLiftSets[index]) {
                        is StandardSetDto -> currCustomSet.copy(id = id)
                        is MyoRepSetDto -> currCustomSet.copy(id = id)
                        is DropSetDto -> currCustomSet.copy(id = id)
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

    private fun createCustomSets(state: WorkoutBuilderState, workoutLiftId: Long): WorkoutBuilderState {
        val workoutCopy = state.workout?.let {
            val liftsWithCustomSetsCopy: List<GenericWorkoutLift> = it.lifts.map { lift ->
                if (lift.id == workoutLiftId && lift is StandardWorkoutLiftDto) {
                    val customSets = mutableListOf<GenericCustomLiftSet>()
                    for (i in 0 until lift.setCount) {
                        customSets.add(
                            StandardSetDto(
                                workoutLiftId = workoutLiftId,
                                position = i,
                                rpeTarget = lift.rpeTarget,
                                repRangeBottom = lift.repRangeBottom,
                                repRangeTop = lift.repRangeTop
                            )
                        )
                    }

                    CustomWorkoutLiftDto(
                        id = lift.id,
                        workoutId = lift.workoutId,
                        liftId = lift.liftId,
                        liftName = lift.liftName,
                        liftMovementPattern = lift.liftMovementPattern,
                        liftIncrementOverride = lift.incrementOverride,
                        liftRestTime = lift.restTime,
                        deloadWeek = lift.deloadWeek,
                        position = lift.position,
                        setCount = lift.setCount,
                        progressionScheme = lift.progressionScheme,
                        incrementOverride = lift.incrementOverride,
                        restTime = lift.restTime,
                        customLiftSets = customSets
                    )
                }
                else if (lift is CustomWorkoutLiftDto) {
                    lift.copy()
                }
                else if (lift is StandardWorkoutLiftDto) {
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

    fun setRestTime(workoutLiftId: Long, newRestTime: Duration, applyAcrossWorkouts: Boolean) {
        Log.d(Log.DEBUG.toString(), "minutes: ${newRestTime.inWholeMinutes} seconds: ${newRestTime.inWholeSeconds % 60}")

        executeInTransactionScope {
            val workoutLift = _state.value.workout!!.lifts.find{ it.id == workoutLiftId }!!
            val workoutLiftCopy = when (workoutLift) {
                is StandardWorkoutLiftDto -> {
                    if (applyAcrossWorkouts) workoutLift.copy(restTime = null, liftRestTime = newRestTime)
                    else workoutLift.copy(restTime = newRestTime)
                }
                is CustomWorkoutLiftDto -> {
                    if (applyAcrossWorkouts) workoutLift.copy(restTime = null, liftRestTime = newRestTime)
                    else workoutLift.copy(restTime = newRestTime)
                }
                else -> throw Exception("${workoutLift::class.simpleName} is not defined.")
            }

            if (applyAcrossWorkouts) {
                liftsRepository.updateRestTime(workoutLift.liftId, newRestTime)
            }

            workoutLiftsRepository.update(workoutLiftCopy)
            _state.update { currentState ->
                currentState.copy(
                    workout = currentState.workout!!.copy(
                        lifts = currentState.workout.lifts.fastMap { lift ->
                            if (lift.id == workoutLiftId) workoutLiftCopy
                            else lift
                        }
                    )
                )
            }
        }
    }

    fun reorderLifts(newLiftOrder: List<ReorderableListItem>) {
        executeInTransactionScope {
            val updatedWorkoutCopy = _state.value.workout!!.copy(
                lifts = newLiftOrder.mapIndexed { index, item ->
                    when(val lift = _state.value.workout!!.lifts.find { it.id == item.key }!!) {
                        is StandardWorkoutLiftDto -> lift.copy(position = index)
                        is CustomWorkoutLiftDto -> lift.copy(position = index)
                        else -> lift
                    }
                }
            )

            workoutLiftsRepository.updateMany(updatedWorkoutCopy.lifts)
            _state.update { it.copy(workout = updatedWorkoutCopy, isReordering = false) }
        }
    }

    private fun updateLiftProperty(
        currentState: WorkoutBuilderState, 
        workoutLiftId: Long, 
        copyLift: (GenericWorkoutLift) -> GenericWorkoutLift
    ): WorkoutDto {
        return currentState.workout!!.let { workout ->
            workout.copy(lifts = workout.lifts.map { currentWorkoutLift ->
                if (currentWorkoutLift.id == workoutLiftId) copyLift(currentWorkoutLift)
                else currentWorkoutLift
            })
        }
    }

    fun setLiftSetCount(workoutLiftId: Long, newSetCount: Int) {
        executeInTransactionScope {
            val updatedStateCopy = _state.value.copy(
                workout = updateLiftProperty(_state.value, workoutLiftId) { lift ->
                    when (lift) {
                        is StandardWorkoutLiftDto -> lift.copy(setCount = newSetCount)
                        is CustomWorkoutLiftDto -> lift.copy(setCount = newSetCount)
                        else -> throw Exception("${lift::class.simpleName} cannot have a set count.")
                    }
                }
            )
            updateLift(updatedStateCopy, workoutLiftId)
            _state.update { updatedStateCopy }
        }
    }

    fun setLiftRepRangeBottom(workoutLiftId: Long, newRepRangeBottom: Int) {
        executeInTransactionScope {
            val updatedStateCopy = _state.value.copy(
                workout = updateLiftProperty(_state.value, workoutLiftId) {
                    when (it) {
                        is StandardWorkoutLiftDto -> it.copy(repRangeBottom = newRepRangeBottom)
                        else -> throw Exception("${it::class.simpleName} cannot have a bottom rep range.")
                    }
                }
            )
            updateLift(updatedStateCopy, workoutLiftId)
            _state.update { updatedStateCopy }
        }
    }

    fun setLiftRepRangeTop(workoutLiftId: Long, newRepRangeTop: Int) {
        executeInTransactionScope {
            val updatedStateCopy = _state.value.copy(
                workout = updateLiftProperty(_state.value, workoutLiftId) {
                    when (it) {
                        is StandardWorkoutLiftDto -> it.copy(repRangeTop = newRepRangeTop)
                        else -> throw Exception("${it::class.simpleName} cannot have a top rep range.")
                    }
                }
            )
            updateLift(updatedStateCopy, workoutLiftId)
            _state.update { updatedStateCopy }
        }
    }

    fun setLiftRpeTarget(workoutLiftId: Long, newRpeTarget: Float) {
        executeInTransactionScope {
            val updatedStateCopy = _state.value.copy(
                workout = updateLiftProperty(_state.value, workoutLiftId) {
                    when (it) {
                        is StandardWorkoutLiftDto -> it.copy(rpeTarget = newRpeTarget)
                        else -> throw Exception("${it::class.simpleName} cannot have an RPE target.")
                    }
                }
            )
            updateLift(updatedStateCopy, workoutLiftId)
            _state.update { updatedStateCopy }
        }
    }

    fun setLiftProgressionScheme(workoutLiftId: Long, newProgressionScheme: ProgressionScheme) {
        executeInTransactionScope {
            val updatedStateCopy = _state.value.copy(
                workout = updateLiftProperty(_state.value, workoutLiftId) {
                    when (it) {
                        is StandardWorkoutLiftDto -> it.copy(progressionScheme = newProgressionScheme)
                        is CustomWorkoutLiftDto -> it.copy(progressionScheme = newProgressionScheme)
                        else -> throw Exception("${it::class.simpleName} cannot have an RPE target.")
                    }
                }
            )
            updateLift(updatedStateCopy, workoutLiftId)
            _state.update { updatedStateCopy }
        }
    }

    private fun updateCustomSetProperty(
        currentState: WorkoutBuilderState,
        workoutLiftId: Long,
        position: Int,
        copyAll: Boolean = false,
        copySet: (GenericCustomLiftSet) -> GenericCustomLiftSet
    ): WorkoutDto {
        return currentState.workout!!.let { workout ->
            workout.copy(lifts = workout.lifts.map { currentWorkoutLift ->
                if (currentWorkoutLift.id == workoutLiftId) {
                    when (currentWorkoutLift) {
                        is CustomWorkoutLiftDto -> currentWorkoutLift.copy(
                            customLiftSets = currentWorkoutLift.customLiftSets.map { set ->
                                if (copyAll || set.position == position) copySet(set) else set
                            }
                        )
                        else -> throw Exception("${currentWorkoutLift.liftName} doesn't have custom sets.")
                    }
                } else {
                    currentWorkoutLift
                }
            })
        }
    }

    fun addSet(workoutLiftId: Long) {
        var addedSet: GenericCustomLiftSet? = null
        val workoutCopy = _state.value.workout!!.let { workout ->
            workout.copy(
                lifts = workout.lifts.map { lift ->
                    if (lift.id == workoutLiftId) {
                        if (lift !is CustomWorkoutLiftDto) throw Exception("Cannot add set to non-custom lift.")
                        lift.copy(
                            customLiftSets = lift.customLiftSets.toMutableList().apply {
                                addedSet = StandardSetDto(
                                    workoutLiftId = lift.id,
                                    position = lift.customLiftSets.count(),
                                    rpeTarget = 8f,
                                    repRangeBottom = 8,
                                    repRangeTop = 10,
                                )
                                add(addedSet as StandardSetDto)
                            },
                            setCount = lift.setCount + 1,
                        )
                    }
                    else lift
                }
            )
        }

        if (addedSet != null) {
            executeInTransactionScope {
                val updatedStateCopy = _state.value.copy(
                    workout = workoutCopy,
                    detailExpansionStates = _state.value.detailExpansionStates.let { expansionStates ->
                        val expansionStatesCopy = HashMap(expansionStates)
                        val setStates = expansionStatesCopy[workoutLiftId]
                        if (setStates != null) {
                            setStates.add(addedSet!!.position)
                        } else {
                            expansionStatesCopy[workoutLiftId] = hashSetOf(addedSet!!.position)
                        }

                        expansionStatesCopy
                    },
                )

                updateLift(updatedStateCopy, workoutLiftId)
                customLiftSetsRepository.insert(addedSet!!)
                _state.update { updatedStateCopy }
            }
        }
    }

    fun deleteSet(workoutLiftId: Long, position: Int) {
        executeInTransactionScope {
            var updatedWorkoutLift: GenericWorkoutLift? = null
            val updatedState = _state.value.let { currentState ->
                currentState.copy(
                    workout = currentState.workout!!.copy(
                        lifts = currentState.workout.lifts.map { workoutLift ->
                            if(workoutLift.id == workoutLiftId) {
                                updatedWorkoutLift = (workoutLift as CustomWorkoutLiftDto).copy(
                                    setCount = workoutLift.setCount - 1,
                                    customLiftSets = workoutLift.customLiftSets
                                        .filter { it.position != position }
                                        .mapIndexed { index, customSet ->
                                            when (customSet) {
                                                is StandardSetDto -> customSet.copy(position = index)
                                                is MyoRepSetDto -> customSet.copy(position = index)
                                                is DropSetDto -> customSet.copy(position = index)
                                                else -> throw Exception("${customSet::class.simpleName} is not defined.")
                                            }
                                        }
                                )
                                updatedWorkoutLift!!
                            } else workoutLift
                        }
                    )
                )
            }

            if (updatedWorkoutLift != null) {
                customLiftSetsRepository.deleteByPosition(workoutLiftId, position)
                workoutLiftsRepository.update(updatedWorkoutLift!!)

                _state.update { updatedState }
            }
        }
    }

    fun setCustomSetRepRangeBottom(workoutLiftId: Long, position: Int, newRepRangeBottom: Int) {
        executeInTransactionScope {
            var updatedSet: GenericCustomLiftSet? = null
            val updatedStateCopy = _state.value.copy(
                workout = updateCustomSetProperty(_state.value, workoutLiftId, position) { set ->
                    updatedSet = when (set) {
                        is StandardSetDto -> set.copy(repRangeBottom = newRepRangeBottom)
                        is DropSetDto -> set.copy(repRangeBottom = newRepRangeBottom)
                        is MyoRepSetDto -> set.copy(repRangeBottom = newRepRangeBottom)
                        else -> throw Exception("${set::class.simpleName} cannot have a bottom rep range.")
                    }
                    updatedSet!!
                }
            )

            if (updatedSet != null) {
                customLiftSetsRepository.update(updatedSet!!)
                _state.update { updatedStateCopy }
            }
        }
    }

    fun setCustomSetRepRangeTop(workoutLiftId: Long, position: Int, newRepRangeTop: Int) {
        executeInTransactionScope {
            var updatedSet: GenericCustomLiftSet? = null
            val updatedStateCopy = _state.value.copy(
                workout = updateCustomSetProperty(_state.value, workoutLiftId, position) { set ->
                    updatedSet = when (set) {
                        is StandardSetDto -> set.copy(repRangeTop = newRepRangeTop)
                        is DropSetDto -> set.copy(repRangeTop = newRepRangeTop)
                        is MyoRepSetDto -> set.copy(repRangeTop = newRepRangeTop)
                        else -> throw Exception("${set::class.simpleName} cannot have a top rep range.")
                    }
                    updatedSet!!
                }
            )

            if (updatedSet != null) {
                customLiftSetsRepository.update(updatedSet!!)
                _state.update { updatedStateCopy }
            }
        }
    }

    fun setCustomSetRpeTarget(workoutLiftId: Long, position: Int, newRpeTarget: Float) {
        executeInTransactionScope {
            var updatedSet: GenericCustomLiftSet? = null
            val updatedStateCopy = _state.value.copy(
                workout = updateCustomSetProperty(_state.value, workoutLiftId, position) { set ->
                    updatedSet = when (set) {
                        is StandardSetDto -> set.copy(rpeTarget = newRpeTarget)
                        is DropSetDto -> set.copy(rpeTarget = newRpeTarget)
                        else -> throw Exception("${set::class.simpleName} cannot have an rpe target.")
                    }
                    updatedSet!!
                }
            )

            if (updatedSet != null) {
                customLiftSetsRepository.update(updatedSet!!)
                _state.update { updatedStateCopy }
            }
        }
    }

    fun setCustomSetRepFloor(workoutLiftId: Long, position: Int, newRepFloor: Int) {
        executeInTransactionScope {
            var updatedSet: GenericCustomLiftSet? = null
            val updatedStateCopy = _state.value.copy(
                workout = updateCustomSetProperty(_state.value, workoutLiftId, position) { set ->
                    updatedSet = when (set) {
                        is MyoRepSetDto -> set.copy(repFloor = newRepFloor)
                        else -> throw Exception("${set::class.simpleName} cannot have a rep floor.")
                    }
                    updatedSet!!
                }
            )

            if (updatedSet != null) {
                customLiftSetsRepository.update(updatedSet!!)
                _state.update { updatedStateCopy }
            }
        }
    }

    fun setCustomSetUseSetMatching(workoutLiftId: Long, position: Int, setMatching: Boolean) {
        executeInTransactionScope {
            var updatedSet: GenericCustomLiftSet? = null
            val updatedStateCopy = _state.value.copy(
                workout = updateCustomSetProperty(_state.value, workoutLiftId, position) { set ->

                    updatedSet = when (set) {
                        is MyoRepSetDto -> set.copy(
                            setMatching = setMatching,
                            matchSetGoal = null,
                            maxSets = null,
                            repFloor = null
                        )
                        else -> throw Exception("${set::class.simpleName} cannot have set matching.")
                    }
                    updatedSet!!
                }
            )

            if (updatedSet != null) {
                customLiftSetsRepository.update(updatedSet!!)
                _state.update { updatedStateCopy }
            }
        }
    }

    fun setCustomSetMatchSetGoal(workoutLiftId: Long, position: Int, newMatchSetGoal: Int) {
        executeInTransactionScope {
            var updatedSet: GenericCustomLiftSet? = null
            val updatedStateCopy = _state.value.copy(
                workout = updateCustomSetProperty(_state.value, workoutLiftId, position) { set ->
                    updatedSet = when (set) {
                        is MyoRepSetDto -> set.copy(matchSetGoal = newMatchSetGoal)
                        else -> throw Exception("${set::class.simpleName} cannot have a match set goal.")
                    }
                    updatedSet!!
                }
            )

            if (updatedSet != null) {
                customLiftSetsRepository.update(updatedSet!!)
                _state.update { updatedStateCopy }
            }
        }
    }

    fun setCustomSetMaxSets(workoutLiftId: Long, position: Int, newMaxSets: Int) {
        executeInTransactionScope {
            var updatedSet: GenericCustomLiftSet? = null
            val updatedStateCopy = _state.value.copy(
                workout = updateCustomSetProperty(_state.value, workoutLiftId, position) { set ->
                    updatedSet = when (set) {
                        is MyoRepSetDto -> set.copy(maxSets = newMaxSets)
                        else -> throw Exception("${set::class.simpleName} cannot have a max set limit.")
                    }
                    updatedSet!!
                }
            )

            if (updatedSet != null) {
                customLiftSetsRepository.update(updatedSet!!)
                _state.update { updatedStateCopy }
            }
        }
    }

    fun setCustomSetDropPercentage(workoutLiftId: Long, position: Int, newDropPercentage: Float) {
        executeInTransactionScope {
            var updatedSet: GenericCustomLiftSet? = null
            val updatedStateCopy = _state.value.copy(
                workout = updateCustomSetProperty(_state.value, workoutLiftId, position) { set ->
                    updatedSet = when (set) {
                        is DropSetDto -> set.copy(dropPercentage = newDropPercentage)
                        else -> throw Exception("${set::class.simpleName} cannot have a drop percentage.")
                    }
                    updatedSet!!
                }
            )

            if (updatedSet != null) {
                customLiftSetsRepository.update(updatedSet!!)
                _state.update { updatedStateCopy }
            }
        }
    }

    fun changeCustomSetType(workoutLiftId: Long, position: Int, newSetType: SetType) {
        executeInTransactionScope {
            var updatedSet: GenericCustomLiftSet? = null
            val updatedStateCopy = _state.value.copy(
                workout = updateCustomSetProperty(_state.value, workoutLiftId, position) { set ->
                    updatedSet = when (set) {
                        is StandardSetDto -> if (newSetType != SetType.STANDARD_SET) transformCustomLiftSet(set, newSetType) else set
                        is DropSetDto -> if (newSetType != SetType.DROP_SET) transformCustomLiftSet(set, newSetType) else set
                        is MyoRepSetDto -> if (newSetType != SetType.MYOREP_SET) transformCustomLiftSet(set, newSetType) else set
                        else -> throw Exception("${set::class.simpleName} cannot have a drop percentage.")
                    }
                    updatedSet!!
                },
            )

            if (updatedSet != null) {
                customLiftSetsRepository.update(updatedSet!!)
                _state.update { updatedStateCopy }
            }
        }
    }

    private fun transformCustomLiftSet(set: GenericCustomLiftSet, newSetType: SetType): GenericCustomLiftSet {
        return when (set) {
            is StandardSetDto ->
                when (newSetType) {
                    SetType.DROP_SET -> DropSetDto(
                        id = set.id,
                        workoutLiftId = set.workoutLiftId,
                        position = set.position,
                        dropPercentage = .1f, // TODO: Add a "drop percentage" setting and use it here
                        rpeTarget = set.rpeTarget,
                        repRangeBottom = set.repRangeBottom,
                        repRangeTop = set.repRangeTop,
                    )
                    SetType.MYOREP_SET -> MyoRepSetDto(
                        id = set.id,
                        workoutLiftId = set.workoutLiftId,
                        position = set.position,
                        repFloor = 5, // TODO: Add a "myo-rep floor" setting and use it here
                        repRangeTop = set.repRangeTop,
                        repRangeBottom = set.repRangeBottom,
                        rpeTarget = set.rpeTarget,
                    )
                    SetType.STANDARD_SET -> set
                }
            is MyoRepSetDto ->
                when (newSetType) {
                    SetType.DROP_SET -> DropSetDto(
                        id = set.id,
                        workoutLiftId = set.workoutLiftId,
                        position = set.position,
                        dropPercentage = .1f, // TODO: Add a "drop percentage" setting and use it here
                        rpeTarget = 8f, // TODO: Add a "rpe target" setting and use it here
                        repRangeBottom = set.repRangeBottom,
                        repRangeTop = set.repRangeTop,
                    )
                    SetType.MYOREP_SET -> set
                    SetType.STANDARD_SET -> StandardSetDto(
                        id = set.id,
                        workoutLiftId = set.workoutLiftId,
                        position = set.position,
                        rpeTarget = 8f, // TODO: Add a "rpe target" setting and use it here
                        repRangeBottom = set.repRangeBottom,
                        repRangeTop = set.repRangeTop,
                    )
                }
            is DropSetDto ->
                when (newSetType) {
                    SetType.DROP_SET -> set
                    SetType.MYOREP_SET -> MyoRepSetDto(
                        id = set.id,
                        workoutLiftId = set.workoutLiftId,
                        position = set.position,
                        repFloor = 5, // TODO: Add a "myo-rep floor" setting and use it here
                        repRangeBottom = set.repRangeBottom,
                        repRangeTop = set.repRangeTop,
                        rpeTarget = set.rpeTarget,
                    )
                    SetType.STANDARD_SET -> StandardSetDto(
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
    
    private fun executeInTransactionScope(action: suspend () -> Unit) {
        viewModelScope.launch { 
            transactionScope.execute {
                action()
            }
        }
    }
}