package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.common.roundToNearestFactor
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.domain.models.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.LoggingDropSet
import com.browntowndev.liftlab.core.domain.models.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.StandardSetResult
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.progression.CalculationEngine
import com.browntowndev.liftlab.core.domain.progression.MyoRepSetGoalValidator
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.greenrobot.eventbus.EventBus

abstract class BaseWorkoutViewModel(
    transactionScope: TransactionScope,
    eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {
    protected var mutableWorkoutState = MutableStateFlow(WorkoutState())
    val workoutState = mutableWorkoutState.asStateFlow()

    protected open fun stopRestTimer() { }
    protected open suspend fun insertRestTimerInProgress(restTime: Long) { }
    protected abstract suspend fun upsertManySetResults(updatedResults: List<SetResult>): List<Long>
    protected abstract suspend fun upsertSetResult(updatedResult: SetResult): Long
    protected abstract suspend fun deleteSetResult(id: Long)

    fun resetMyoRepSetsCompleted() {
        mutableWorkoutState.update {
            it.copy(completedMyoRepSets = false)
        }
    }

    private fun updateSetIfAlreadyCompleted(workoutLiftId: Long, set: GenericLoggingSet) {
        if (set.complete &&
            set.completedWeight != null &&
            set.completedReps != null &&
            set.completedRpe != null
        ) {
            val liftPosition = mutableWorkoutState.value.workout!!.lifts.find { it.id == workoutLiftId }!!.position
            val currentResult = mutableWorkoutState.value.inProgressWorkout!!.completedSets
                .find {
                    it.liftPosition == liftPosition &&
                            it.setPosition == set.position
                } ?: throw Exception("Completed set was not in completedSets")
            val updatedResult = when (currentResult) {
                is StandardSetResult -> currentResult.copy(
                    weight = set.completedWeight!!,
                    reps = set.completedReps!!,
                    rpe = set.completedRpe!!,
                )
                is MyoRepSetResult -> currentResult.copy(
                    weight = set.completedWeight!!,
                    reps = set.completedReps!!,
                    rpe = set.completedRpe!!,
                )
                is LinearProgressionSetResult -> currentResult.copy(
                    weight = set.completedWeight!!,
                    reps = set.completedReps!!,
                    rpe = set.completedRpe!!,
                )
                else -> throw Exception("${currentResult::class.simpleName} is not defined.")
            }
            completeSet(0L, false, updatedResult)
        } else if (set.complete) {
            val workoutLift = mutableWorkoutState.value.workout!!.lifts.find { it.id == workoutLiftId }!!
            undoSetCompletion(
                liftPosition = workoutLift.position,
                setPosition = set.position,
                myoRepSetPosition = (set as? LoggingMyoRepSet)?.myoRepSetPosition,
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
                    is LoggingStandardSet -> set.copy(completedWeight = newWeight)
                    is LoggingDropSet -> set.copy(completedWeight = newWeight)
                    is LoggingMyoRepSet -> set.copy(completedWeight = newWeight)
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
                    is LoggingStandardSet -> set.copy(completedReps = newReps)
                    is LoggingDropSet -> set.copy(completedReps = newReps)
                    is LoggingMyoRepSet -> set.copy(completedReps = newReps)
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
                    is LoggingStandardSet -> set.copy(completedRpe = newRpe)
                    is LoggingDropSet -> set.copy(completedRpe = newRpe)
                    is LoggingMyoRepSet -> set.copy(completedRpe = newRpe)
                    else -> throw Exception("${set::class.simpleName} is not defined.")
                }
                updateSetIfAlreadyCompleted(workoutLiftId, updatedSet)
                updatedSet
            }
        )
    }

    private fun completeLogEntryItem(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, copySet: (set: GenericLoggingSet) -> GenericLoggingSet) {
        mutableWorkoutState.update { currentState ->
            currentState.copy(
                workout = currentState.workout!!.copy(
                    lifts = currentState.workout.lifts.fastMap { workoutLift ->
                        if (workoutLift.id == workoutLiftId) {
                            workoutLift.copy(
                                sets = workoutLift.sets.fastMap { set ->
                                    if (set.position == setPosition &&
                                        (set as? LoggingMyoRepSet)?.myoRepSetPosition == myoRepSetPosition
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
        thisMyoRepSet: LoggingMyoRepSet,
        roundingFactor: Float,
    ): List<GenericLoggingSet> {

        val myoRepLoggingSets: List<LoggingMyoRepSet> = mutableLoggingSets
            .filterIsInstance<LoggingMyoRepSet>()
            .filter { it.position == thisMyoRepSet.position }

        return MyoRepSetGoalValidator.shouldContinueMyoReps(
            completedSet = thisMyoRepSet,
            myoRepSetResults = myoRepLoggingSets,
        ).let { continueMyoReps ->
            if (continueMyoReps) {
                mutableLoggingSets.apply {
                    val insertAtIndex = indexOf(thisMyoRepSet) + 1
                    add(
                        index = insertAtIndex,
                        thisMyoRepSet.copy(
                            myoRepSetPosition = myoRepLoggingSets.size - 1,
                            weightRecommendation = thisMyoRepSet.completedWeight,
                            repRangePlaceholder = if (thisMyoRepSet.repFloor != null) ">${thisMyoRepSet.repFloor}"
                            else "—",
                            complete = false,
                            rpeTarget = 10f,
                            completedWeight = null,
                            completedReps = null,
                            completedRpe = null,
                        )
                    )
                }
            } else if (MyoRepSetGoalValidator.shouldContinueMyoReps(
                    completedSet = thisMyoRepSet,
                    myoRepSetResults = myoRepLoggingSets,
                    activationSetAlwaysSuccess = true
            )) {
                mutableLoggingSets.apply {
                    val insertAtIndex = indexOf(thisMyoRepSet) + 1
                    add(
                        index = insertAtIndex,
                        thisMyoRepSet.copy(
                            myoRepSetPosition = myoRepLoggingSets.size - 1,
                            weightRecommendation = CalculationEngine.calculateSuggestedWeight(
                                completedWeight = thisMyoRepSet.completedWeight!!,
                                completedReps = thisMyoRepSet.completedReps!!,
                                completedRpe = thisMyoRepSet.completedRpe!!,
                                repGoal = thisMyoRepSet.repRangeBottom!!,
                                rpeGoal = thisMyoRepSet.rpeTarget,
                                roundingFactor = roundingFactor,
                            ),
                            repRangePlaceholder = if (thisMyoRepSet.repFloor != null) ">${thisMyoRepSet.repFloor}"
                            else "—",
                            complete = false,
                            rpeTarget = 10f,
                            completedWeight = null,
                            completedReps = null,
                            completedRpe = null,
                        )
                    )
                }
            } else if (!MyoRepSetGoalValidator.shouldContinueMyoReps(
                completedSet = myoRepLoggingSets.last { it.complete },
                myoRepSetResults = myoRepLoggingSets.filter { it.complete },
            )) {
                mutableWorkoutState.update {
                    it.copy(completedMyoRepSets = true)
                }

                mutableLoggingSets.apply {
                    myoRepLoggingSets
                        .filter {
                            !it.complete &&
                                    (it.myoRepSetPosition ?: -1) > (thisMyoRepSet.myoRepSetPosition ?: -1)
                        }.let { removeAll(it.toSet()) }
                }
            } else {
                mutableLoggingSets
            }
        }
    }

    private fun copyDropSetWithUpdatedWeightRecommendation(
        dropFromSet: LoggingStandardSet,
        dropSet: LoggingDropSet,
        increment: Float,
    ): LoggingDropSet {
        val isDropFromSetSuccess =
            (dropFromSet.repRangeBottom >= dropFromSet.completedReps!!) &&
                    (dropFromSet.rpeTarget >= dropFromSet.completedRpe!!)

        val percentageOfDroppedSet = (1 - dropSet.dropPercentage)
        return dropSet.copy(
            weightRecommendation = if (isDropFromSetSuccess) {
                (dropFromSet.completedWeight!! * percentageOfDroppedSet).roundToNearestFactor(increment)
            } else {
                (CalculationEngine.calculateSuggestedWeight(
                    completedWeight = dropFromSet.completedWeight!!,
                    completedReps = dropFromSet.completedReps,
                    completedRpe = dropFromSet.completedRpe!!,
                    repGoal = dropFromSet.repRangeTop,
                    rpeGoal = dropFromSet.rpeTarget,
                    roundingFactor = increment,
                ) * percentageOfDroppedSet).roundToNearestFactor(increment)
            }
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
            if (setPosition == set.position &&
                myoRepSetPosition == (set as? LoggingMyoRepSet)?.myoRepSetPosition) {
                completedSet = when (set) {
                    is LoggingStandardSet -> set.copy(complete = true)
                    is LoggingDropSet -> set.copy(complete = true)
                    is LoggingMyoRepSet -> set.copy(complete = true)
                    else -> throw Exception("${set::class.simpleName} is not defined.")
                }
                completedSet!!
            } else if (set.position == (setPosition + 1) && !set.hadInitialWeightRecommendation) {
                when (set) {
                    is LoggingStandardSet -> set.copy(
                        weightRecommendation = completedSet!!.completedWeight
                    )
                    is LoggingMyoRepSet -> set.copy(
                        weightRecommendation = completedSet!!.completedWeight
                    )
                    is LoggingDropSet -> copyDropSetWithUpdatedWeightRecommendation(
                        dropFromSet = currentSets[setPosition] as LoggingStandardSet,
                        dropSet = set,
                        increment = increment,
                    )
                    else -> throw Exception("${set::class.simpleName} is not defined.")
                }
            } else set
        }.toMutableList()

        return if (completedSet is LoggingMyoRepSet) {
            updateLoggingSetsOnMyoRepSetCompletion(
                mutableLoggingSets = mutableSetCopy,
                thisMyoRepSet = completedSet as LoggingMyoRepSet,
                roundingFactor = increment,
            )
        } else mutableSetCopy
    }

    fun buildSetResult(
        liftId: Long,
        setType: SetType,
        progressionScheme: ProgressionScheme? = null,
        liftPosition: Int,
        setPosition: Int,
        myoRepSetPosition: Int?,
        weight: Float,
        reps: Int,
        rpe: Float,
    ): SetResult {
        val workoutId = mutableWorkoutState.value.workout!!.id
        val currentMesocycle = mutableWorkoutState.value.programMetadata!!.currentMesocycle
        val currentMicrocycle = mutableWorkoutState.value.programMetadata!!.currentMicrocycle
        val weightRecommendation = mutableWorkoutState.value.setsByPositions
            ?.get(liftPosition)
            ?.get(setPosition)
            ?.weightRecommendation

        return buildSetResult(
            workoutId = workoutId,
            currentMesocycle = currentMesocycle,
            currentMicrocycle = currentMicrocycle,
            weightRecommendation = weightRecommendation,
            liftId = liftId,
            setType = setType,
            progressionScheme = progressionScheme,
            liftPosition = liftPosition,
            setPosition = setPosition,
            myoRepSetPosition = myoRepSetPosition,
            weight = weight,
            reps = reps,
            rpe = rpe,
        )
    }

    protected fun buildSetResult(
        id: Long = 0L,
        workoutId: Long,
        currentMesocycle: Int,
        currentMicrocycle: Int,
        weightRecommendation: Float?,
        liftId: Long,
        setType: SetType,
        progressionScheme: ProgressionScheme? = null,
        liftPosition: Int,
        setPosition: Int,
        myoRepSetPosition: Int?,
        weight: Float,
        reps: Int,
        rpe: Float,
    ): SetResult {
        val isDeload = (mutableWorkoutState.value.workout!!.lifts
            .find { it.liftId == liftId && it.position == liftPosition }
            ?.deloadWeek ?: mutableWorkoutState.value.programMetadata!!.deloadWeek) ==
                (currentMicrocycle + 1)

        return when (setType) {
            SetType.STANDARD,
            SetType.DROP_SET -> {
                if (progressionScheme != ProgressionScheme.LINEAR_PROGRESSION) {
                    StandardSetResult(
                        id = id,
                        workoutId = workoutId,
                        setType = setType,
                        liftId = liftId,
                        mesoCycle = currentMesocycle,
                        microCycle = currentMicrocycle,
                        liftPosition = liftPosition,
                        setPosition = setPosition,
                        weightRecommendation = weightRecommendation,
                        weight = weight,
                        reps = reps,
                        rpe = rpe,
                        isDeload = isDeload,
                    )
                } else {
                    // LP can only be standard liftEntity, so no myo
                    LinearProgressionSetResult(
                        id = id,
                        workoutId = workoutId,
                        liftId = liftId,
                        mesoCycle = currentMesocycle,
                        microCycle = currentMicrocycle,
                        liftPosition = liftPosition,
                        setPosition = setPosition,
                        weightRecommendation = weightRecommendation,
                        weight = weight,
                        reps = reps,
                        rpe = rpe,
                        missedLpGoals = 0, // assigned on completion
                        isDeload = isDeload,
                    )
                }
            }

            SetType.MYOREP ->
                MyoRepSetResult(
                    id = id,
                    workoutId = workoutId,
                    liftId = liftId,
                    mesoCycle = currentMesocycle,
                    microCycle = currentMicrocycle,
                    liftPosition = liftPosition,
                    setPosition = setPosition,
                    weightRecommendation = weightRecommendation,
                    weight = weight,
                    reps = reps,
                    rpe = rpe,
                    myoRepSetPosition = myoRepSetPosition,
                    isDeload = isDeload,
                )
        }
    }

    fun completeSet(restTime: Long, restTimerEnabled: Boolean, result: SetResult): Job {
        return executeInTransactionScope {
            if (restTimerEnabled) {
                insertRestTimerInProgress(restTime)
            }
            mutableWorkoutState.update { currentState ->
                currentState.copy(
                    restTime = if (restTimerEnabled) restTime else currentState.restTime,
                    restTimerStartedAt = if(restTimerEnabled) getCurrentDate() else currentState.restTimerStartedAt,
                    inProgressWorkout = currentState.inProgressWorkout?.copy(
                        completedSets = currentState.inProgressWorkout.completedSets.toMutableList().apply {
                            val existingResult = find { existing ->
                                existing.liftId == result.liftId &&
                                        existing.liftPosition == result.liftPosition &&
                                        existing.setPosition == result.setPosition &&
                                        (existing as? MyoRepSetResult)?.myoRepSetPosition == (result as? MyoRepSetResult)?.myoRepSetPosition
                            }

                            if (existingResult != null) {
                                val updatedResult = when (result) {
                                    is StandardSetResult -> result.copy(id = existingResult.id)
                                    is MyoRepSetResult -> result.copy(id = existingResult.id)
                                    is LinearProgressionSetResult -> result.copy(id = existingResult.id)
                                    else -> throw Exception("${result::class.simpleName} is not defined.")
                                }

                                upsertSetResult(updatedResult)
                                set(indexOf(existingResult), updatedResult)
                            } else {
                                val id = upsertSetResult(result)
                                val insertedResult = when (result) {
                                    is StandardSetResult -> result.copy(id = id)
                                    is MyoRepSetResult -> result.copy(id = id)
                                    is LinearProgressionSetResult -> result.copy(id = id)
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
                                        myoRepSetPosition = (result as? MyoRepSetResult)?.myoRepSetPosition,
                                        currentSets = workoutLift.sets,
                                        increment = workoutLift.incrementOverride
                                            ?: SettingsManager.getSetting(
                                                SettingsManager.SettingNames.INCREMENT_AMOUNT,
                                                SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
                                            ))
                                )
                            } else workoutLift
                        }
                    )
                )
            }
        }
    }

    private suspend fun deleteSetResultAndReturnDeleted(liftPosition: Int, setPosition: Int, myoRepSetPosition: Int?): SetResult? {
        return mutableWorkoutState.value.inProgressWorkout
            ?.completedSets
            ?.find {
                it.liftPosition == liftPosition &&
                        it.setPosition == setPosition &&
                        (it as? MyoRepSetResult)?.myoRepSetPosition == myoRepSetPosition
            }
            ?.also { result ->
                deleteSetResult(id = result.id)
            }
    }

    fun undoSetCompletion(liftPosition: Int, setPosition: Int, myoRepSetPosition: Int?) {
        executeInTransactionScope {
            stopRestTimer()
            val deletedResult = deleteSetResultAndReturnDeleted(
                liftPosition = liftPosition,
                setPosition = setPosition,
                myoRepSetPosition = myoRepSetPosition,
            )
            mutableWorkoutState.update { currentState ->
                currentState.copy(
                    restTimerStartedAt = null,
                    inProgressWorkout = currentState.inProgressWorkout?.copy(
                        completedSets = currentState.inProgressWorkout.completedSets.fastMapNotNull { setResult ->
                            if (setResult.id != deletedResult?.id) {
                                setResult
                            } else null
                        }
                    ),
                    workout = currentState.workout!!.copy(
                        lifts = currentState.workout.lifts.fastMap { workoutLift ->
                            var hasWeightRecommendation = false
                            if (workoutLift.position == liftPosition) {
                                workoutLift.copy(
                                    sets = workoutLift.sets.fastMap { set ->
                                        if (set.position == setPosition &&
                                            (set as? LoggingMyoRepSet)?.myoRepSetPosition == myoRepSetPosition) {
                                            hasWeightRecommendation = set.weightRecommendation != null
                                            when (set) {
                                                is LoggingStandardSet -> set.copy(complete = false)
                                                is LoggingDropSet -> set.copy(complete = false)
                                                is LoggingMyoRepSet -> set.copy(complete = false)
                                                else -> throw Exception("${set::class.simpleName} is not defined.")
                                            }
                                        } else if (!hasWeightRecommendation &&
                                            set.position == (setPosition + 1)) {
                                            // undo the set's weight recommendation when its main
                                            // set is set as uncompleted and it had no recommendation
                                            when (set) {
                                                is LoggingStandardSet -> set.copy(weightRecommendation = null)
                                                is LoggingDropSet -> set.copy(weightRecommendation = null)
                                                is LoggingMyoRepSet -> set.copy(weightRecommendation = null)
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
            val workoutLift = mutableWorkoutState.value.workout!!.lifts.find { it.id == workoutLiftId }!!
            val liftPosition = workoutLift.position
            val isComplete = workoutLift.sets.find {
                it is LoggingMyoRepSet &&
                        it.position == setPosition &&
                        it.myoRepSetPosition == myoRepSetPosition
            }?.complete

            var deletedResult: SetResult? = null
            if (isComplete == true) {
                deletedResult = deleteSetResultAndReturnDeleted(
                    liftPosition = liftPosition,
                    setPosition = setPosition,
                    myoRepSetPosition = myoRepSetPosition,
                )
            }

            mutableWorkoutState.update { currentState ->
                currentState.copy(
                    inProgressWorkout = currentState.inProgressWorkout?.copy(
                        completedSets = currentState.inProgressWorkout.completedSets.fastMapNotNull { setResult ->
                            if (setResult.id != deletedResult?.id) {
                                setResult
                            } else null
                        }
                    ),
                    workout = currentState.workout!!.let { workout ->
                        workout.copy(
                            lifts = workout.lifts.fastMap { workoutLift ->
                                if (workoutLift.id == workoutLiftId) {
                                    workoutLift.copy(
                                        sets = workoutLift.sets.toMutableList().apply {
                                            val toDelete = find { set ->
                                                set.position == setPosition &&
                                                        (set as LoggingMyoRepSet).myoRepSetPosition == myoRepSetPosition
                                            }!!

                                            remove(toDelete)
                                        }.mapIndexed { index, set ->
                                            if (set is LoggingMyoRepSet && set.myoRepSetPosition != null) {
                                                set.copy(myoRepSetPosition = index - 1)
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

    protected open suspend fun updateLinearProgressionFailures() {
        val resultsByLift = mutableWorkoutState.value.inProgressWorkout!!.completedSets.associateBy {
            "${it.liftId}-${it.setPosition}"
        }
        val setResultsToUpdate = mutableListOf<SetResult>()
        mutableWorkoutState.value.workout!!.lifts
            .filter { workoutLift -> workoutLift.progressionScheme == ProgressionScheme.LINEAR_PROGRESSION }
            .fastForEach { workoutLift ->
                workoutLift.sets.fastForEach { set ->
                    val result = resultsByLift["${workoutLift.liftId}-${set.position}"]
                    if (result != null &&
                        ((set.completedReps ?: -1) < set.repRangeBottom!! ||
                                (set.completedRpe ?: -1f) > set.rpeTarget)) {
                        val lpResults = result as LinearProgressionSetResult
                        setResultsToUpdate.add(
                            lpResults.copy(
                                missedLpGoals = lpResults.missedLpGoals + 1
                            )
                        )
                    } else if (result != null && (result as LinearProgressionSetResult).missedLpGoals > 0) {
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