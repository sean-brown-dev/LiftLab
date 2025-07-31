package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingDropSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.StandardSetResult
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.CompleteSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UndoSetCompletionUseCase
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.greenrobot.eventbus.EventBus

abstract class BaseWorkoutViewModel(
    private val completeSetUseCase: CompleteSetUseCase,
    private val undoSetCompletionUseCase: UndoSetCompletionUseCase,
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

    /**
     * Persists the set if it has already been completed
     */
    private fun updateSetIfAlreadyCompleted(workoutLiftId: Long, setToUpdate: GenericLoggingSet) {
        if (setToUpdate.complete &&
            setToUpdate.completedWeight != null &&
            setToUpdate.completedReps != null &&
            setToUpdate.completedRpe != null
        ) {
            val liftPosition = mutableWorkoutState.value.workout!!.lifts.find { it.id == workoutLiftId }!!.position
            val currentResult = mutableWorkoutState.value.completedSets
                .find {
                    it.liftPosition == liftPosition &&
                            it.setPosition == setToUpdate.position
                } ?: throw Exception("Completed set was not in completedSets")
            val updatedResult = when (currentResult) {
                is StandardSetResult -> currentResult.copy(
                    weight = setToUpdate.completedWeight!!,
                    reps = setToUpdate.completedReps!!,
                    rpe = setToUpdate.completedRpe!!,
                )
                is MyoRepSetResult -> currentResult.copy(
                    weight = setToUpdate.completedWeight!!,
                    reps = setToUpdate.completedReps!!,
                    rpe = setToUpdate.completedRpe!!,
                )
                is LinearProgressionSetResult -> currentResult.copy(
                    weight = setToUpdate.completedWeight!!,
                    reps = setToUpdate.completedReps!!,
                    rpe = setToUpdate.completedRpe!!,
                )
                else -> throw Exception("${currentResult::class.simpleName} is not defined.")
            }
            completeSet(0L, false, { updatedResult })
        } else if (setToUpdate.complete) {
            val workoutLift = mutableWorkoutState.value.workout!!.lifts.find { it.id == workoutLiftId }!!
            undoSetCompletion(
                liftPosition = workoutLift.position,
                setPosition = setToUpdate.position,
                myoRepSetPosition = (setToUpdate as? LoggingMyoRepSet)?.myoRepSetPosition,
            )
        } else {
            // Simply update state since we are not persisting any changes
            updateSetInWorkout(workoutLiftId, setToUpdate)
        }
    }

    private fun updateSetInWorkout(workoutLiftId: Long, setToUpdate: GenericLoggingSet) {
        mutableWorkoutState.update { currentState ->
            val workout = currentState.workout ?: return@update currentState
            val lifts = workout.lifts

            val liftIndex = lifts.indexOfFirst { it.id == workoutLiftId }
            if (liftIndex == -1) return@update currentState // Lift not found, do nothing.

            val currentLift = lifts[liftIndex]
            val currentSets = currentLift.sets

            val setIndex = currentSets.indexOfFirst { set ->
                set.position == setToUpdate.position &&
                        (set as? LoggingMyoRepSet)?.myoRepSetPosition == (setToUpdate as? LoggingMyoRepSet)?.myoRepSetPosition
            }
            if (setIndex == -1) return@update currentState

            val newSets = currentSets.toMutableList().apply {
                this[setIndex] = setToUpdate // Just replace the set at the specific index.
            }

            val newLifts = lifts.toMutableList().apply {
                this[liftIndex] = currentLift.copy(sets = newSets)
            }

            currentState.copy(
                workout = workout.copy(lifts = newLifts)
            )
        }
    }

    fun setWeight(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, newWeight: Float?) =
        executeWithErrorHandling("Failed to update set weight") {
            val set = findSet(
                workoutLiftId = workoutLiftId,
                setPosition = setPosition,
                myoRepSetPosition = myoRepSetPosition,
            ) ?: throw Exception("Set not found")

            val updatedSet = when (set) {
                is LoggingStandardSet -> set.copy(completedWeight = newWeight)
                is LoggingDropSet -> set.copy(completedWeight = newWeight)
                is LoggingMyoRepSet -> set.copy(completedWeight = newWeight)
                else -> throw Exception("${set::class.simpleName} is not defined.")
            }
            updateSetIfAlreadyCompleted(workoutLiftId, updatedSet)
        }

    fun setReps(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, newReps: Int?) =
        executeWithErrorHandling("Failed to update set reps") {
            val set = findSet(
                workoutLiftId = workoutLiftId,
                setPosition = setPosition,
                myoRepSetPosition = myoRepSetPosition,
            ) ?: throw Exception("Set not found")

            val updatedSet = when (set) {
                is LoggingStandardSet -> set.copy(completedReps = newReps)
                is LoggingDropSet -> set.copy(completedReps = newReps)
                is LoggingMyoRepSet -> set.copy(completedReps = newReps)
                else -> throw Exception("${set::class.simpleName} is not defined.")
            }
            updateSetIfAlreadyCompleted(workoutLiftId, updatedSet)
        }

    fun setRpe(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, newRpe: Float) =
        executeWithErrorHandling("Failed to update set reps") {
            val set = findSet(
                workoutLiftId = workoutLiftId,
                setPosition = setPosition,
                myoRepSetPosition = myoRepSetPosition
            ) ?: throw Exception("Set not found")

            val updatedSet = when (set) {
                is LoggingStandardSet -> set.copy(completedRpe = newRpe)
                is LoggingDropSet -> set.copy(completedRpe = newRpe)
                is LoggingMyoRepSet -> set.copy(completedRpe = newRpe)
                else -> throw Exception("${set::class.simpleName} is not defined.")
            }
            updateSetIfAlreadyCompleted(workoutLiftId, updatedSet)
        }

    private fun findSet(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?): GenericLoggingSet? {
        return mutableWorkoutState.value.workout?.lifts?.fastFirst {
            it.id == workoutLiftId
        }?.sets?.fastFirst { set ->
            set.position == setPosition &&
                    (set as? LoggingMyoRepSet)?.myoRepSetPosition == myoRepSetPosition
        }
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

    fun completeSet(restTime: Long, restTimerEnabled: Boolean, onBuildSetResult: () -> SetResult, onError: () -> Unit = {}) =
        executeWithErrorHandling("Failed to complete set") {
            try {
                completeSetUseCase(
                    restTime = restTime,
                    restTimerEnabled = restTimerEnabled,
                    result = onBuildSetResult(),
                    existingSetResults = mutableWorkoutState.value.completedSets,
                    onUpsertSetResult = { upsertSetResult(it) }
                )
            } catch (e: Exception) {
                onError()
                throw e
            }
        }

    private suspend fun deleteSetResult(liftPosition: Int, setPosition: Int, myoRepSetPosition: Int?) {
        undoSetCompletionUseCase(
            liftPosition = liftPosition,
            setPosition = setPosition,
            myoRepSetPosition = myoRepSetPosition,
            setResults = mutableWorkoutState.value.completedSets,
            onDeleteSetResult = { deleteSetResult(it) }
        )
    }

    fun undoSetCompletion(liftPosition: Int, setPosition: Int, myoRepSetPosition: Int?) =
        executeWithErrorHandling("Failed to undo completion") {
            stopRestTimer()
            Log.d(
                "WorkoutViewModel",
                "undoSetCompletion ${mutableWorkoutState.value.completedSets}"
            )

            undoSetCompletionUseCase(
                liftPosition = liftPosition,
                setPosition = setPosition,
                myoRepSetPosition = myoRepSetPosition,
                setResults = mutableWorkoutState.value.completedSets,
                onDeleteSetResult = { deleteSetResult(it) }
            )
        }

    protected open suspend fun updateLinearProgressionFailures() {
        val resultsByLift = mutableWorkoutState.value.completedSets.associateBy {
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