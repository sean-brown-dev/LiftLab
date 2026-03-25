package com.browntowndev.liftlab.ui.viewmodels.workout

import android.util.Log
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_REST_TIME
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.REST_TIME
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.CompleteSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UndoSetCompletionUseCase
import com.browntowndev.liftlab.ui.extensions.copyGeneric
import com.browntowndev.liftlab.ui.mapping.toDomainModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LinearProgressionSetResultUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingDropSetUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingMyoRepSetUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingSetUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingStandardSetUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.MyoRepSetResultUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.SetResultUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.StandardSetResultUiModel
import com.browntowndev.liftlab.ui.viewmodels.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.greenrobot.eventbus.EventBus
import kotlin.time.DurationUnit
import kotlin.time.toDuration

abstract class BaseWorkoutViewModel(
    private val completeSetUseCase: CompleteSetUseCase,
    private val undoSetCompletionUseCase: UndoSetCompletionUseCase,
    eventBus: EventBus,
    private val isEditingCompletedWorkout: Boolean,
): BaseViewModel(eventBus) {
    protected val defaultRestTime = SettingsManager
        .getSetting(key = REST_TIME, defaultValue = DEFAULT_REST_TIME)
        .toDuration(unit = DurationUnit.MILLISECONDS)

    protected var mutableWorkoutState = MutableStateFlow(WorkoutState())
    val workoutState = mutableWorkoutState.asStateFlow()

    protected open suspend fun insertRestTimerInProgress(restTime: Long) { }
    protected abstract suspend fun upsertManySetResults(updatedResults: List<SetResult>): List<Long>
    protected abstract suspend fun upsertSetResult(updatedResult: SetResult): Long
    protected abstract suspend fun deleteSetResult(id: Long)

    /**
     * Persists the set if it has already been completed
     */
    private suspend fun updateSetIfAlreadyCompleted(workoutLiftId: Long, setToUpdate: LoggingSetUiModel) {
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
                is StandardSetResultUiModel -> currentResult.copy(
                    weight = setToUpdate.completedWeight!!,
                    reps = setToUpdate.completedReps!!,
                    rpe = setToUpdate.completedRpe!!,
                )
                is MyoRepSetResultUiModel -> currentResult.copy(
                    weight = setToUpdate.completedWeight!!,
                    reps = setToUpdate.completedReps!!,
                    rpe = setToUpdate.completedRpe!!,
                )
                is LinearProgressionSetResultUiModel -> currentResult.copy(
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
                workoutLiftId = workoutLift.id,
                liftPosition = workoutLift.position,
                set = setToUpdate
            )
        } else {
            // Simply update state since we are not persisting any changes
            updateSetInWorkout(workoutLiftId, setToUpdate)
        }
    }

    private fun updateSetInWorkout(workoutLiftId: Long, setToUpdate: LoggingSetUiModel) {
        mutableWorkoutState.update { currentState ->
            val workout = currentState.workout ?: return@update currentState
            val lifts = workout.lifts

            val liftIndex = lifts.indexOfFirst { it.id == workoutLiftId }
            if (liftIndex == -1) return@update currentState // Lift not found, do nothing.

            val currentLift = lifts[liftIndex]
            val currentSets = currentLift.sets

            val setIndex = currentSets.indexOfFirst { set ->
                set.position == setToUpdate.position &&
                        (set as? LoggingMyoRepSetUiModel)?.myoRepSetPosition == (setToUpdate as? LoggingMyoRepSetUiModel)?.myoRepSetPosition
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

    fun setWeight(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, newWeight: Float?) = executeWithErrorHandling("Failed to update set weight") {
        if (newWeight == null && isEditingCompletedWorkout) return@executeWithErrorHandling

        val set = findSet(
            workoutLiftId = workoutLiftId,
            setPosition = setPosition,
            myoRepSetPosition = myoRepSetPosition,
        ) ?: throw Exception("Set not found")

        val updatedSet = when (set) {
            is LoggingStandardSetUiModel -> set.copy(completedWeight = newWeight)
            is LoggingDropSetUiModel -> set.copy(completedWeight = newWeight)
            is LoggingMyoRepSetUiModel -> set.copy(completedWeight = newWeight)
            else -> throw Exception("${set::class.simpleName} is not defined.")
        }
        updateSetIfAlreadyCompleted(workoutLiftId, updatedSet)
    }

    fun setReps(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, newReps: Int?) = executeWithErrorHandling("Failed to update set reps") {
        if (newReps == null && isEditingCompletedWorkout) return@executeWithErrorHandling

        val set = findSet(
            workoutLiftId = workoutLiftId,
            setPosition = setPosition,
            myoRepSetPosition = myoRepSetPosition,
        ) ?: throw Exception("Set not found")

        val updatedSet = when (set) {
            is LoggingStandardSetUiModel -> set.copy(completedReps = newReps)
            is LoggingDropSetUiModel -> set.copy(completedReps = newReps)
            is LoggingMyoRepSetUiModel -> set.copy(completedReps = newReps)
            else -> throw Exception("${set::class.simpleName} is not defined.")
        }
        updateSetIfAlreadyCompleted(workoutLiftId, updatedSet)
    }

    fun setRpe(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, newRpe: Float) = executeWithErrorHandling("Failed to update set reps") {
        val set = findSet(
            workoutLiftId = workoutLiftId,
            setPosition = setPosition,
            myoRepSetPosition = myoRepSetPosition
        ) ?: throw Exception("Set not found")

        val updatedSet = when (set) {
            is LoggingStandardSetUiModel -> set.copy(completedRpe = newRpe)
            is LoggingDropSetUiModel -> set.copy(completedRpe = newRpe)
            is LoggingMyoRepSetUiModel -> set.copy(completedRpe = newRpe)
            else -> throw Exception("${set::class.simpleName} is not defined.")
        }
        updateSetIfAlreadyCompleted(workoutLiftId, updatedSet)
    }

    private fun findSet(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?): LoggingSetUiModel? {
        return mutableWorkoutState.value.workout?.lifts?.fastFirst {
            it.id == workoutLiftId
        }?.sets?.fastFirst { set ->
            set.position == setPosition &&
                    (set as? LoggingMyoRepSetUiModel)?.myoRepSetPosition == myoRepSetPosition
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
    ): SetResultUiModel {
        val workoutId = mutableWorkoutState.value.workout!!.id
        val currentMicrocycle = mutableWorkoutState.value.programMetadata!!.currentMicrocycle

        return buildSetResult(
            workoutId = workoutId,
            currentMicrocycle = currentMicrocycle,
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
        currentMicrocycle: Int,
        liftId: Long,
        setType: SetType,
        progressionScheme: ProgressionScheme? = null,
        liftPosition: Int,
        setPosition: Int,
        myoRepSetPosition: Int?,
        weight: Float,
        reps: Int,
        rpe: Float,
    ): SetResultUiModel {
        val isDeload = (mutableWorkoutState.value.workout!!.lifts
            .find { it.liftId == liftId && it.position == liftPosition }
            ?.deloadWeek ?: mutableWorkoutState.value.programMetadata!!.deloadWeek) ==
                (currentMicrocycle + 1)

        return when (setType) {
            SetType.STANDARD,
            SetType.DROP_SET -> {
                if (progressionScheme != ProgressionScheme.LINEAR_PROGRESSION) {
                    StandardSetResultUiModel(
                        id = id,
                        workoutId = workoutId,
                        setType = setType,
                        liftId = liftId,
                        liftPosition = liftPosition,
                        setPosition = setPosition,
                        weight = weight,
                        reps = reps,
                        rpe = rpe,
                        persistedOneRepMax = null,
                        isDeload = isDeload,
                    )
                } else {
                    // LP can only be standard liftEntity, so no myo
                    LinearProgressionSetResultUiModel(
                        id = id,
                        workoutId = workoutId,
                        liftId = liftId,
                        liftPosition = liftPosition,
                        setPosition = setPosition,
                        weight = weight,
                        reps = reps,
                        rpe = rpe,
                        persistedOneRepMax = null,
                        missedLpGoals = 0, // assigned on completion
                        isDeload = isDeload,
                    )
                }
            }

            SetType.MYOREP ->
                MyoRepSetResultUiModel(
                    id = id,
                    workoutId = workoutId,
                    liftId = liftId,
                    liftPosition = liftPosition,
                    setPosition = setPosition,
                    weight = weight,
                    reps = reps,
                    rpe = rpe,
                    persistedOneRepMax = null,
                    myoRepSetPosition = myoRepSetPosition,
                    isDeload = isDeload,
                )
        }
    }

    fun completeSet(restTime: Long, restTimerEnabled: Boolean, onBuildSetResult: () -> SetResultUiModel, onError: () -> Unit = {}) = executeWithErrorHandling("Failed to complete set") {
            try {
                completeSetUseCase(
                    restTimeInMillis = restTime,
                    restTimerEnabled = restTimerEnabled,
                    result = onBuildSetResult().toDomainModel(),
                    existingSetResults = mutableWorkoutState.value.completedSets.fastMap { it.toDomainModel() },
                    onUpsertSetResult = { upsertSetResult(it) }
                )
            } catch (e: Exception) {
                onError()
                throw e
            }
        }

    fun undoSetCompletion(liftPosition: Int, setPosition: Int, myoRepSetPosition: Int?) = executeWithErrorHandling("Failed to undo completion") {
        Log.d(
            "WorkoutViewModel",
            "undoSetCompletion ${mutableWorkoutState.value.completedSets}"
        )
        val workoutLift = mutableWorkoutState.value.workout?.lifts?.get(liftPosition) ?: error("Workout lift not found. Lift position: $liftPosition")
        val setToMarkIncomplete = findSet(
            workoutLiftId = workoutLift.id,
            setPosition = setPosition,
            myoRepSetPosition = myoRepSetPosition,
        ) ?: error("Set not found")

        undoSetCompletion(
            workoutLiftId = workoutLift.id,
            liftPosition = liftPosition,
            set = setToMarkIncomplete
        )
    }

    private suspend fun undoSetCompletion(workoutLiftId: Long, liftPosition: Int, set: LoggingSetUiModel) {

        // Optimistically update UI so that re-hydration of the weight, reps, & rpe can happen
        val originalWorkout = mutableWorkoutState.value.workout
        updateSetInWorkout(workoutLiftId, set.copyGeneric(complete = false))

        try {
            undoSetCompletionUseCase(
                liftPosition = liftPosition,
                setPosition = set.position,
                myoRepSetPosition = (set as? LoggingMyoRepSetUiModel)?.myoRepSetPosition,
                setResults = mutableWorkoutState.value.completedSets.fastMap { it.toDomainModel() },
                onDeleteSetResult = { deleteSetResult(it) }
            )
        } catch (e: Exception) {
            mutableWorkoutState.update {
                it.copy(workout = originalWorkout)
            }
            throw e
        }
    }

    protected open suspend fun updateLinearProgressionFailures() {
        val resultsByLift = mutableWorkoutState.value.completedSets.associateBy {
            "${it.liftId}-${it.setPosition}"
        }
        val setResultsToUpdate = mutableListOf<SetResultUiModel>()
        mutableWorkoutState.value.workout!!.lifts
            .filter { workoutLift -> workoutLift.progressionScheme == ProgressionScheme.LINEAR_PROGRESSION }
            .fastForEach { workoutLift ->
                workoutLift.sets.fastForEach { set ->
                    val result = resultsByLift["${workoutLift.liftId}-${set.position}"]
                    if (result != null &&
                        ((set.completedReps ?: -1) < set.repRangeBottom!! ||
                                (set.completedRpe ?: -1f) > set.rpeTarget)) {
                        val lpResults = result as LinearProgressionSetResultUiModel
                        setResultsToUpdate.add(
                            lpResults.copy(
                                missedLpGoals = lpResults.missedLpGoals + 1
                            )
                        )
                    } else if (result != null && (result as LinearProgressionSetResultUiModel).missedLpGoals > 0) {
                        setResultsToUpdate.add(
                            result.copy(
                                missedLpGoals = 0
                            )
                        )
                    }
                }
            }

        if (setResultsToUpdate.isNotEmpty()) {
            upsertManySetResults(setResultsToUpdate.fastMap { it.toDomainModel() })
        }
    }
}