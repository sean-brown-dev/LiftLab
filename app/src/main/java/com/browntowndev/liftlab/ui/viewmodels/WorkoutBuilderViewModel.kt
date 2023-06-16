package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.DropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericCustomLiftSet
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutsRepository
import com.browntowndev.liftlab.ui.viewmodels.states.PickerState
import com.browntowndev.liftlab.ui.viewmodels.states.PickerType
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutBuilderState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class WorkoutBuilderViewModel(
    private val workoutId: Long,
    private val navHostController: NavHostController,
    private val workoutsRepository: WorkoutsRepository,
    private val eventBus: EventBus,
): ViewModel() {
    private var _state = MutableStateFlow(WorkoutBuilderState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(workout = workoutsRepository.get(workoutId))
            }
        }
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
            TopAppBarAction.NavigatedBack -> navHostController.popBackStack()
            else -> {}
        }
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

    fun addSet(workoutLiftId: Long) {
        var addedSetPosition: Int? = null;

        val workoutCopy = _state.value.workout!!.let { workout ->
            workout.copy(
                lifts = workout.lifts.map { lift ->
                    if (lift.id == workoutLiftId) {
                        if (lift !is CustomWorkoutLiftDto) throw Exception("Cannot add set to non-custom lift.")
                        lift.copy(
                            customLiftSets = lift.customLiftSets.toMutableList().apply {
                                addedSetPosition = lift.customLiftSets.count()
                                add(
                                    StandardSetDto(
                                        position = addedSetPosition!!,
                                        rpeTarget = 8.toDouble(),
                                        repRangeBottom = 8,
                                        repRangeTop = 10,
                                    )
                                )
                            }
                        )
                    }
                    else lift
                }
            )
        }

        if (addedSetPosition != null) {
            _state.update {
                _state.value.copy(
                    workout = workoutCopy,
                    detailExpansionStates = _state.value.detailExpansionStates.let { expansionStates ->
                        val expansionStatesCopy = HashMap(expansionStates)
                        val setStates = expansionStatesCopy[workoutLiftId]
                        if(setStates != null) {
                            setStates.add(addedSetPosition!!)
                        } else {
                            expansionStatesCopy[workoutLiftId]= hashSetOf(addedSetPosition!!)
                        }

                        expansionStatesCopy
                    },
                )
            }
        }
    }

    fun deleteSet(workoutLiftId: Long, position: Int) {
        //TODO
    }

    fun toggleHasCustomLiftSets(workoutLiftId: Long, enableCustomSets: Boolean) {
        _state.update {
            if (enableCustomSets) {
                createCustomSets(it, workoutLiftId)
            } else {
                it.copy(workout = it.workout!!.copy(
                    lifts = it.workout.lifts.map { lift ->
                        if (lift.id == workoutLiftId) {
                            val topCustomLiftSet =
                                (lift as CustomWorkoutLiftDto).customLiftSets.firstOrNull()
                            StandardWorkoutLiftDto(
                                id = lift.id,
                                liftId = lift.liftId,
                                liftName = lift.liftName,
                                liftMovementPattern = lift.liftMovementPattern,
                                position = lift.position,
                                setCount = lift.setCount,
                                repRangeBottom = topCustomLiftSet?.repRangeBottom ?: 8,
                                repRangeTop = topCustomLiftSet?.repRangeTop ?: 10,
                                rpeTarget = if (topCustomLiftSet is StandardSetDto) topCustomLiftSet.rpeTarget else 8.toDouble(),
                                useReversePyramidSets = lift.useReversePyramidSets,
                                progressionScheme = lift.progressionScheme,
                            )
                        } else lift
                    }
                ))
            }
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
                                position = i,
                                rpeTarget = lift.rpeTarget,
                                repRangeBottom = lift.repRangeBottom,
                                repRangeTop = lift.repRangeTop
                            )
                        )
                    }

                    CustomWorkoutLiftDto(
                        id = lift.id,
                        liftId = lift.liftId,
                        liftName = lift.liftName,
                        liftMovementPattern = lift.liftMovementPattern,
                        position = lift.position,
                        setCount = lift.setCount,
                        useReversePyramidSets = lift.useReversePyramidSets,
                        progressionScheme = lift.progressionScheme,
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
        _state.update { currentState ->
            currentState.copy(
                workout = updateLiftProperty(currentState, workoutLiftId) { lift ->
                    when (lift) {
                        is StandardWorkoutLiftDto -> lift.copy(setCount = newSetCount)
                        is CustomWorkoutLiftDto -> lift.copy(setCount = newSetCount)
                        else -> throw Exception("${lift::class.simpleName} cannot have a set count.")
                    }
                }
            )
        }
    }

    fun setLiftRepRangeBottom(workoutLiftId: Long, newRepRangeBottom: Int) {
        _state.update { currentState ->
            currentState.copy(
                workout = updateLiftProperty(currentState, workoutLiftId) {
                    when (it) {
                        is StandardWorkoutLiftDto -> it.copy(repRangeBottom = newRepRangeBottom)
                        else -> throw Exception("${it::class.simpleName} cannot have a bottom rep range.")
                    }
                }
            )
        }
    }

    fun setLiftRepRangeTop(workoutLiftId: Long, newRepRangeTop: Int) {
        _state.update { currentState ->
            currentState.copy(
                workout = updateLiftProperty(currentState, workoutLiftId) {
                    when (it) {
                        is StandardWorkoutLiftDto -> it.copy(repRangeTop = newRepRangeTop)
                        else -> throw Exception("${it::class.simpleName} cannot have a top rep range.")
                    }
                }
            )
        }
    }

    fun setLiftRpeTarget(workoutLiftId: Long, newRpeTarget: Double) {
        _state.update { currentState ->
            currentState.copy(
                workout = updateLiftProperty(currentState, workoutLiftId) {
                    when (it) {
                        is StandardWorkoutLiftDto -> it.copy(rpeTarget = newRpeTarget)
                        else -> throw Exception("${it::class.simpleName} cannot have an RPE target.")
                    }
                }
            )
        }
    }

    fun setLiftProgressionScheme(workoutLiftId: Long, newProgressionScheme: ProgressionScheme) {
        _state.update { currentState ->
            currentState.copy(
                workout = updateLiftProperty(currentState, workoutLiftId) {
                    when (it) {
                        is StandardWorkoutLiftDto -> it.copy(progressionScheme = newProgressionScheme)
                        is CustomWorkoutLiftDto -> it.copy(progressionScheme = newProgressionScheme)
                        else -> throw Exception("${it::class.simpleName} cannot have an RPE target.")
                    }
                }
            )
        }
    }

    private fun setCustomSetProperty(
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

    fun setCustomSetRepRangeBottom(workoutLiftId: Long, position: Int, newRepRangeBottom: Int) {
        _state.update { currentState ->
            currentState.copy(
                workout = setCustomSetProperty(currentState, workoutLiftId, position) { set ->
                    when (set) {
                        is StandardSetDto -> set.copy(repRangeBottom = newRepRangeBottom)
                        is DropSetDto -> set.copy(repRangeBottom = newRepRangeBottom)
                        is MyoRepSetDto -> set.copy(repRangeBottom = newRepRangeBottom)
                        else -> throw Exception("${set::class.simpleName} cannot have a bottom rep range.")
                    }
                }
            )
        }
    }

    fun setCustomSetRepRangeTop(workoutLiftId: Long, position: Int, newRepRangeTop: Int) {
        _state.update { currentState ->
            currentState.copy(
                workout = setCustomSetProperty(currentState, workoutLiftId, position) { set ->
                    when (set) {
                        is StandardSetDto -> set.copy(repRangeTop = newRepRangeTop)
                        is DropSetDto -> set.copy(repRangeTop = newRepRangeTop)
                        is MyoRepSetDto -> set.copy(repRangeTop = newRepRangeTop)
                        else -> throw Exception("${set::class.simpleName} cannot have a top rep range.")
                    }
                }
            )
        }
    }

    fun setCustomSetRpeTarget(workoutLiftId: Long, position: Int, newRpeTarget: Double) {
        _state.update { currentState ->
            currentState.copy(
                workout = setCustomSetProperty(currentState, workoutLiftId, position) { set ->
                    when (set) {
                        is StandardSetDto -> set.copy(rpeTarget = newRpeTarget)
                        is DropSetDto -> set.copy(rpeTarget = newRpeTarget)
                        else -> throw Exception("${set::class.simpleName} cannot have an rpe target.")
                    }
                }
            )
        }
    }

    fun setCustomSetRepFloor(workoutLiftId: Long, position: Int, newRepFloor: Int) {
        _state.update { currentState ->
            currentState.copy(
                workout = setCustomSetProperty(currentState, workoutLiftId, position) { set ->
                    when (set) {
                        is MyoRepSetDto -> set.copy(repFloor = newRepFloor)
                        else -> throw Exception("${set::class.simpleName} cannot have a rep floor.")
                    }
                }
            )
        }
    }

    fun setUseSetMatching(workoutLiftId: Long, position: Int, setMatching: Boolean) {
        _state.update { currentState ->
            currentState.copy(
                workout = setCustomSetProperty(currentState, workoutLiftId, position) { set ->
                    when (set) {
                        is MyoRepSetDto -> set.copy(setMatching = setMatching, matchSetGoal = set.matchSetGoal?:2)
                        else -> throw Exception("${set::class.simpleName} cannot have a rep floor.")
                    }
                }
            )
        }
    }

    fun setMatchSetGoal(workoutLiftId: Long, position: Int, newMatchSetGoal: Int) {
        _state.update { currentState ->
            currentState.copy(
                workout = setCustomSetProperty(currentState, workoutLiftId, position) { set ->
                    when (set) {
                        is MyoRepSetDto -> set.copy(matchSetGoal = newMatchSetGoal)
                        else -> throw Exception("${set::class.simpleName} cannot have a rep floor.")
                    }
                }
            )
        }
    }

    fun setCustomSetDropPercentage(workoutLiftId: Long, position: Int, newDropPercentage: Double) {
        _state.update { currentState ->
            currentState.copy(
                workout = setCustomSetProperty(currentState, workoutLiftId, position) { set ->
                    when (set) {
                        is DropSetDto -> set.copy(dropPercentage = newDropPercentage)
                        else -> throw Exception("${set::class.simpleName} cannot have a drop percentage.")
                    }
                }
            )
        }
    }

    fun changeCustomSetType(workoutLiftId: Long, position: Int, newSetType: SetType) {
        _state.update { currentState ->
            currentState.copy(
                workout = setCustomSetProperty(currentState, workoutLiftId, position) { set ->
                    when (set) {
                        is StandardSetDto -> if (newSetType != SetType.STANDARD_SET) transformCustomLiftSet(set, newSetType) else set
                        is DropSetDto -> if (newSetType != SetType.DROP_SET) transformCustomLiftSet(set, newSetType) else set
                        is MyoRepSetDto -> if (newSetType != SetType.MYOREP_SET) transformCustomLiftSet(set, newSetType) else set
                        else -> throw Exception("${set::class.simpleName} cannot have a drop percentage.")
                    }
                },
            )
        }
    }

    private fun transformCustomLiftSet(set: GenericCustomLiftSet, newSetType: SetType): GenericCustomLiftSet {
        return when (set) {
            is StandardSetDto ->
                when (newSetType) {
                    SetType.DROP_SET -> DropSetDto(
                        position = set.position,
                        dropPercentage = .1, // TODO: Add a "drop percentage" setting and use it here
                        rpeTarget = set.rpeTarget,
                        repRangeBottom = set.repRangeBottom,
                        repRangeTop = set.repRangeTop,
                    )
                    SetType.MYOREP_SET -> MyoRepSetDto(
                        position = set.position,
                        repFloor = 5, // TODO: Add a "myo-rep floor" setting and use it here
                        repRangeTop = set.repRangeTop,
                        repRangeBottom = set.repRangeBottom,
                    )
                    SetType.STANDARD_SET -> set
                }
            is MyoRepSetDto ->
                when (newSetType) {
                    SetType.DROP_SET -> DropSetDto(
                        position = set.position,
                        dropPercentage = .1, // TODO: Add a "drop percentage" setting and use it here
                        rpeTarget = 8.toDouble(), // TODO: Add a "rpe target" setting and use it here
                        repRangeBottom = set.repRangeBottom,
                        repRangeTop = set.repRangeTop,
                    )
                    SetType.MYOREP_SET -> set
                    SetType.STANDARD_SET -> StandardSetDto(
                        position = set.position,
                        rpeTarget = 8.toDouble(), // TODO: Add a "rpe target" setting and use it here
                        repRangeBottom = set.repRangeBottom,
                        repRangeTop = set.repRangeTop,
                    )
                }
            is DropSetDto ->
                when (newSetType) {
                    SetType.DROP_SET -> set
                    SetType.MYOREP_SET -> MyoRepSetDto(
                        position = set.position,
                        repFloor = 5, // TODO: Add a "myo-rep floor" setting and use it here
                        repRangeBottom = set.repRangeBottom ?: 8,
                        repRangeTop = set.repRangeTop ?: 10,
                    )
                    SetType.STANDARD_SET -> StandardSetDto(
                        position = set.position,
                        rpeTarget = 8.toDouble(), // TODO: Add a "rpe target" setting and use it here
                        repRangeBottom = set.repRangeBottom ?: 8,
                        repRangeTop = set.repRangeTop ?: 10,
                    )
                }
            else -> throw Exception("${set::class.simpleName} is not recognized as a custom set type.")
        }
    }
}