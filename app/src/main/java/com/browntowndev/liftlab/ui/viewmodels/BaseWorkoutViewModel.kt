package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.Utils
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.common.roundToNearestFactor
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.LinearProgressionSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingDropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.progression.MyoRepSetGoalValidator
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.greenrobot.eventbus.EventBus

abstract class BaseWorkoutViewModel(
    transactionScope: TransactionScope,
    eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {
    protected var mutableState = MutableStateFlow(WorkoutState())
    val state = mutableState.asStateFlow()

    protected open fun stopRestTimer() { }
    protected open suspend fun insertRestTimerInProgress(restTime: Long) { }
    protected abstract suspend fun upsertManySetResults(updatedResults: List<SetResult>): List<Long>
    protected abstract suspend fun upsertSetResult(updatedResult: SetResult): Long
    protected abstract suspend fun deleteSetResult(workoutId: Long, liftId: Long, setPosition: Int, myoRepSetPosition: Int?)

    private fun updateSetIfAlreadyCompleted(workoutLiftId: Long, set: GenericLoggingSet) {
        if (set.complete &&
            set.completedWeight != null &&
            set.completedReps != null &&
            set.completedRpe != null
        ) {
            val liftPosition = mutableState.value.workout!!.lifts.find { it.id == workoutLiftId }!!.position
            val currentResult = mutableState.value.inProgressWorkout!!.completedSets
                .find {
                    it.liftPosition == liftPosition &&
                            it.setPosition == set.setPosition
                } ?: throw Exception("Completed set was not in completedSets")
            val updatedResult = when (currentResult) {
                is StandardSetResultDto -> currentResult.copy(
                    weight = set.completedWeight!!,
                    reps = set.completedReps!!,
                    rpe = set.completedRpe!!,
                )
                is MyoRepSetResultDto -> currentResult.copy(
                    weight = set.completedWeight!!,
                    reps = set.completedReps!!,
                    rpe = set.completedRpe!!,
                )
                is LinearProgressionSetResultDto -> currentResult.copy(
                    weight = set.completedWeight!!,
                    reps = set.completedReps!!,
                    rpe = set.completedRpe!!,
                )
                else -> throw Exception("${currentResult::class.simpleName} is not defined.")
            }
            completeSet(0L, false, updatedResult)
        } else if (set.complete) {
            val workoutLift = mutableState.value.workout!!.lifts.find { it.id == workoutLiftId }!!
            undoSetCompletion(
                liftId = workoutLift.liftId,
                setPosition = set.setPosition,
                myoRepSetPosition = (set as? LoggingMyoRepSetDto)?.myoRepSetPosition,
            )
        }
    }

    fun setWeight(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, newWeight: Float?) {
        // Don't persist this unless already completed. Persistence happens when entire set is completed
        completeLogEntryItem(
            workoutLiftId = workoutLiftId,
            setPosition = setPosition,
            myoRepSetPosition = myoRepSetPosition,
            copySet = { set ->
                val updatedSet = when (set) {
                    is LoggingStandardSetDto -> set.copy(completedWeight = newWeight)
                    is LoggingDropSetDto -> set.copy(completedWeight = newWeight)
                    is LoggingMyoRepSetDto -> set.copy(completedWeight = newWeight)
                    else -> throw Exception("${set::class.simpleName} is not defined.")
                }
                updateSetIfAlreadyCompleted(workoutLiftId, updatedSet)
                updatedSet
            }
        )
    }

    fun setReps(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, newReps: Int?) {
        // Don't persist this unless already completed. Persistence happens when entire set is completed
        completeLogEntryItem(
            workoutLiftId = workoutLiftId,
            setPosition = setPosition,
            myoRepSetPosition = myoRepSetPosition,
            copySet = { set ->
                val updatedSet = when (set) {
                    is LoggingStandardSetDto -> set.copy(completedReps = newReps)
                    is LoggingDropSetDto -> set.copy(completedReps = newReps)
                    is LoggingMyoRepSetDto -> set.copy(completedReps = newReps)
                    else -> throw Exception("${set::class.simpleName} is not defined.")
                }
                updateSetIfAlreadyCompleted(workoutLiftId, updatedSet)
                updatedSet
            }
        )
    }

    fun setRpe(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, newRpe: Float) {
        // Don't persist this unless already completed. Persistence happens when entire set is completed
        completeLogEntryItem(
            workoutLiftId = workoutLiftId,
            setPosition = setPosition,
            myoRepSetPosition = myoRepSetPosition,
            copySet = { set ->
                val updatedSet = when (set) {
                    is LoggingStandardSetDto -> set.copy(completedRpe = newRpe)
                    is LoggingDropSetDto -> set.copy(completedRpe = newRpe)
                    is LoggingMyoRepSetDto -> set.copy(completedRpe = newRpe)
                    else -> throw Exception("${set::class.simpleName} is not defined.")
                }
                updateSetIfAlreadyCompleted(workoutLiftId, updatedSet)
                updatedSet
            }
        )
    }

    private fun completeLogEntryItem(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, copySet: (set: GenericLoggingSet) -> GenericLoggingSet) {
        mutableState.update { currentState ->
            currentState.copy(
                workout = currentState.workout!!.copy(
                    lifts = currentState.workout.lifts.fastMap { workoutLift ->
                        if (workoutLift.id == workoutLiftId) {
                            workoutLift.copy(
                                sets = workoutLift.sets.fastMap { set ->
                                    if (set.setPosition == setPosition &&
                                        (set as? LoggingMyoRepSetDto)?.myoRepSetPosition == myoRepSetPosition
                                    ) {
                                        copySet(set)
                                    } else set
                                }
                            )
                        } else workoutLift
                    }
                )
            )
        }
    }

    private fun updateLoggingSetsOnMyoRepSetCompletion(
        mutableLoggingSets: MutableList<GenericLoggingSet>,
        thisMyoRepSet: LoggingMyoRepSetDto,
        completedWeight: Float,
    ): List<GenericLoggingSet> {
        val myoRepSets = mutableLoggingSets.filterIsInstance<LoggingMyoRepSetDto>()
        val previousMyoRepResults = myoRepSets
            .filter {
                (thisMyoRepSet.myoRepSetPosition ?: -1) > (it.myoRepSetPosition ?: -1)
            }

        val hasMyoRepSetsAfter = myoRepSets
            .any {
                (thisMyoRepSet.myoRepSetPosition ?: -1) < (it.myoRepSetPosition ?: -1)
            }

        return if (!hasMyoRepSetsAfter && // Don't add more myorep sets if it already has them
            MyoRepSetGoalValidator.shouldContinueMyoReps(
                completedSet = thisMyoRepSet,
                previousMyoRepSets = previousMyoRepResults,
            )
        ) {
            mutableLoggingSets.apply {
                val myoRepSetIndex = if(thisMyoRepSet.myoRepSetPosition != null) {
                    thisMyoRepSet.myoRepSetPosition + 1
                } else 0
                val insertAtIndex = 1 + thisMyoRepSet.setPosition + myoRepSetIndex
                add(
                    index = insertAtIndex,
                    thisMyoRepSet.copy(
                        myoRepSetPosition = previousMyoRepResults.size,
                        weightRecommendation = completedWeight,
                        repRangePlaceholder = if (thisMyoRepSet.repFloor != null) ">${thisMyoRepSet.repFloor}"
                        else "—",
                        complete = false,
                        completedWeight = null,
                        completedReps = null,
                        completedRpe = null,
                    )
                )
            }
        } else mutableLoggingSets
    }

    private fun copyDropSetWithUpdatedWeightRecommendation(
        completedWeight: Float,
        dropSet: LoggingDropSetDto,
        increment: Float,
    ): LoggingDropSetDto {
        return dropSet.copy(
            weightRecommendation = (completedWeight * (1 - dropSet.dropPercentage))
                .roundToNearestFactor(increment)
        )
    }

    private fun copySetsOnCompletion(
        setPosition: Int,
        myoRepSetPosition: Int?,
        currentSets: List<GenericLoggingSet>,
        increment: Float,
    ): List<GenericLoggingSet> {
        var completedSet: GenericLoggingSet? = null
        val mutableSetCopy = currentSets.fastMap { set ->
            if (setPosition == set.setPosition &&
                myoRepSetPosition == (set as? LoggingMyoRepSetDto)?.myoRepSetPosition) {
                completedSet = when (set) {
                    is LoggingStandardSetDto -> set.copy(complete = true)
                    is LoggingDropSetDto -> set.copy(complete = true)
                    is LoggingMyoRepSetDto -> set.copy(complete = true)
                    else -> throw Exception("${set::class.simpleName} is not defined.")
                }
                completedSet!!
            } else if (set.setPosition == (setPosition + 1)) {
                when (set) {
                    is LoggingStandardSetDto -> set.copy(weightRecommendation = completedSet!!.completedWeight)
                    is LoggingMyoRepSetDto -> set.copy(weightRecommendation = completedSet!!.completedWeight)
                    is LoggingDropSetDto -> copyDropSetWithUpdatedWeightRecommendation(
                        completedWeight = currentSets[setPosition].completedWeight!!,
                        dropSet = set,
                        increment = increment,
                    )
                    else -> throw Exception("${set::class.simpleName} is not defined.")
                }
            } else set
        }.toMutableList()

        return if (completedSet is LoggingMyoRepSetDto) {
            updateLoggingSetsOnMyoRepSetCompletion(
                mutableLoggingSets = mutableSetCopy,
                thisMyoRepSet = completedSet as LoggingMyoRepSetDto,
                completedWeight = completedSet!!.completedWeight!!,
            )
        } else mutableSetCopy
    }

    fun buildSetResult(
        liftId: Long,
        setType: SetType,
        progressionScheme: ProgressionScheme,
        liftPosition: Int,
        setPosition: Int,
        myoRepSetPosition: Int?,
        weight: Float,
        reps: Int,
        rpe: Float,
    ): SetResult {
        val workoutId = mutableState.value.workout!!.id
        val currentMesocycle = mutableState.value.programMetadata!!.currentMesocycle
        val currentMicrocycle = mutableState.value.programMetadata!!.currentMicrocycle

        return when (setType) {
            SetType.STANDARD,
            SetType.DROP_SET -> {
                if (progressionScheme != ProgressionScheme.LINEAR_PROGRESSION) {
                    StandardSetResultDto(
                        workoutId = workoutId,
                        setType = setType,
                        liftId = liftId,
                        mesoCycle = currentMesocycle,
                        microCycle = currentMicrocycle,
                        liftPosition = liftPosition,
                        setPosition = setPosition,
                        weight = weight,
                        reps = reps,
                        rpe = rpe,
                    )
                } else {
                    // LP can only be standard lift, so no myo
                    LinearProgressionSetResultDto(
                        workoutId = workoutId,
                        liftId = liftId,
                        mesoCycle = currentMesocycle,
                        microCycle = currentMicrocycle,
                        liftPosition = liftPosition,
                        setPosition = setPosition,
                        weight = weight,
                        reps = reps,
                        rpe = rpe,
                        missedLpGoals = 0, // assigned on completion
                    )
                }
            }

            SetType.MYOREP ->
                MyoRepSetResultDto(
                    workoutId = workoutId,
                    liftId = liftId,
                    mesoCycle = currentMesocycle,
                    microCycle = currentMicrocycle,
                    liftPosition = liftPosition,
                    setPosition = setPosition,
                    weight = weight,
                    reps = reps,
                    rpe = rpe,
                    myoRepSetPosition = myoRepSetPosition,
                )
        }
    }

    fun completeSet(restTime: Long, restTimerEnabled: Boolean, result: SetResult) {
        executeInTransactionScope {
            if (restTimerEnabled) {
                insertRestTimerInProgress(restTime)
            }
            mutableState.update { currentState ->
                currentState.copy(
                    restTime = if (restTimerEnabled) restTime else currentState.restTime,
                    restTimerStartedAt = if(restTimerEnabled) Utils.getCurrentDate() else currentState.restTimerStartedAt,
                    inProgressWorkout = currentState.inProgressWorkout?.copy(
                        completedSets = currentState.inProgressWorkout.completedSets.toMutableList().apply {
                            val existingResult = find { existing ->
                                existing.liftId == result.liftId &&
                                        existing.liftPosition == result.liftPosition &&
                                        existing.setPosition == result.setPosition &&
                                        (existing as? MyoRepSetResultDto)?.myoRepSetPosition ==
                                        (result as? MyoRepSetResultDto)?.myoRepSetPosition
                            }

                            if (existingResult != null) {
                                val updatedResult = when (result) {
                                    is StandardSetResultDto -> result.copy(id = existingResult.id)
                                    is MyoRepSetResultDto -> result.copy(id = existingResult.id)
                                    is LinearProgressionSetResultDto -> result.copy(id = existingResult.id)
                                    else -> throw Exception("${result::class.simpleName} is not defined.")
                                }

                                upsertSetResult(updatedResult)
                                set(indexOf(existingResult), updatedResult)
                            } else {
                                val id = upsertSetResult(result)
                                val insertedResult = when (result) {
                                    is StandardSetResultDto -> result.copy(id = id)
                                    is MyoRepSetResultDto -> result.copy(id = id)
                                    is LinearProgressionSetResultDto -> result.copy(id = id)
                                    else -> throw Exception("${result::class.simpleName} is not defined.")
                                }
                                add(insertedResult)
                            }
                        }
                    ),
                    workout = currentState.workout!!.copy(
                        lifts = currentState.workout.lifts.map { workoutLift ->
                            if (workoutLift.liftId == result.liftId) {
                                workoutLift.copy(
                                    sets = copySetsOnCompletion(
                                        setPosition = result.setPosition,
                                        myoRepSetPosition = (result as? MyoRepSetResultDto)?.myoRepSetPosition,
                                        currentSets = workoutLift.sets,
                                        increment = workoutLift.incrementOverride
                                            ?: SettingsManager.getSetting(
                                                SettingsManager.SettingNames.INCREMENT_AMOUNT,
                                                5f
                                            ))
                                )
                            } else workoutLift
                        }
                    )
                )
            }
        }
    }

    fun undoSetCompletion(liftId: Long, setPosition: Int, myoRepSetPosition: Int?) {
        executeInTransactionScope {
            stopRestTimer()
            deleteSetResult(
                workoutId = mutableState.value.workout!!.id,
                liftId = liftId,
                setPosition = setPosition,
                myoRepSetPosition = myoRepSetPosition
            )

            mutableState.update { currentState ->
                currentState.copy(
                    restTimerStartedAt = null,
                    workout = currentState.workout!!.copy(
                        lifts = currentState.workout.lifts.fastMap { workoutLift ->
                            var hasWeightRecommendation = false
                            if (workoutLift.liftId == liftId) {
                                workoutLift.copy(
                                    sets = workoutLift.sets.fastMap { set ->
                                        if (set.setPosition == setPosition &&
                                            (set as? LoggingMyoRepSetDto)?.myoRepSetPosition == myoRepSetPosition) {
                                            hasWeightRecommendation = set.weightRecommendation != null
                                            when (set) {
                                                is LoggingStandardSetDto -> set.copy(complete = false)
                                                is LoggingDropSetDto -> set.copy(complete = false)
                                                is LoggingMyoRepSetDto -> set.copy(complete = false)
                                                else -> throw Exception("${set::class.simpleName} is not defined.")
                                            }
                                        } else if (!hasWeightRecommendation &&
                                            set.setPosition == (setPosition + 1)) {
                                            // undo the set's weight recommendation when its main
                                            // set is set as uncompleted and it had no recommendation
                                            when (set) {
                                                is LoggingStandardSetDto -> set.copy(weightRecommendation = null)
                                                is LoggingDropSetDto -> set.copy(weightRecommendation = null)
                                                is LoggingMyoRepSetDto -> set.copy(weightRecommendation = null)
                                                else -> throw Exception("${set::class.simpleName} is not defined.")
                                            }
                                        } else set
                                    }
                                )
                            } else workoutLift
                        }
                    )
                )
            }
        }
    }

    fun deleteMyoRepSet(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int) {
        executeInTransactionScope {
            val workoutLift = mutableState.value.workout!!.lifts.find { it.id == workoutLiftId }!!
            val liftId = workoutLift.liftId
            val isComplete = workoutLift.sets.find {
                it is LoggingMyoRepSetDto &&
                        it.setPosition == setPosition &&
                        it.myoRepSetPosition == myoRepSetPosition
            }?.complete

            if (isComplete == true) {
                deleteSetResult(
                    workoutId = mutableState.value.workout!!.id,
                    liftId = liftId,
                    setPosition = setPosition,
                    myoRepSetPosition = myoRepSetPosition
                )
            }

            mutableState.update { currentState ->
                currentState.copy(
                    workout = currentState.workout!!.let { workout ->
                        workout.copy(
                            lifts = workout.lifts.fastMap { workoutLift ->
                                if (workoutLift.id == workoutLiftId) {
                                    workoutLift.copy(
                                        sets = workoutLift.sets.toMutableList().apply {
                                            val toDelete = find { set ->
                                                set.setPosition == setPosition &&
                                                        (set as LoggingMyoRepSetDto).myoRepSetPosition == myoRepSetPosition
                                            }!!

                                            remove(toDelete)
                                        }.mapIndexed { index, set ->
                                            if (index > 0) {
                                                val myoRepSet = set as LoggingMyoRepSetDto
                                                myoRepSet.copy(myoRepSetPosition = index - 1)
                                            } else set
                                        }
                                    )
                                } else workoutLift
                            }
                        )
                    }
                )
            }
        }
    }

    protected suspend fun updateLinearProgressionFailures() {
        val resultsByLift = mutableState.value.inProgressWorkout!!.completedSets.associateBy {
            "${it.liftId}-${it.setPosition}"
        }
        val setResultsToUpdate = mutableListOf<SetResult>()
        mutableState.value.workout!!.lifts
            .filter { workoutLift -> workoutLift.progressionScheme == ProgressionScheme.LINEAR_PROGRESSION }
            .fastForEach { workoutLift ->
                workoutLift.sets.fastForEach { set ->
                    val result = resultsByLift["${workoutLift.liftId}-${set.setPosition}"]
                    if (result != null &&
                        ((set.completedReps ?: -1) < set.repRangeBottom ||
                                (set.completedRpe ?: -1f) > set.rpeTarget)) {
                        val lpResults = result as LinearProgressionSetResultDto
                        setResultsToUpdate.add(
                            lpResults.copy(
                                missedLpGoals = lpResults.missedLpGoals + 1
                            )
                        )
                    } else if (result != null && (result as LinearProgressionSetResultDto).missedLpGoals > 0) {
                        setResultsToUpdate.add(
                            result.copy(
                                missedLpGoals = 0
                            )
                        )
                    }
                }
            }

        if (setResultsToUpdate.isNotEmpty()) {
            upsertManySetResults(setResultsToUpdate)
        }
    }
}