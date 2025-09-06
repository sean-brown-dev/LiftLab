package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import android.util.Log
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.models.workoutLogging.HistoricalWorkoutName
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.repositories.HistoricalWorkoutNamesRepository
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import com.browntowndev.liftlab.ui.models.workout.WorkoutInProgressUiModel
import com.google.common.base.Stopwatch
import java.util.concurrent.TimeUnit

class CompleteWorkoutUseCase(
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val restTimerInProgressRepository: RestTimerInProgressRepository,
    private val programsRepository: ProgramsRepository,
    private val historicalWorkoutNamesRepository: HistoricalWorkoutNamesRepository,
    private val workoutLogRepository: WorkoutLogRepository,
    private val liveWorkoutCompletedSetsRepository: LiveWorkoutCompletedSetsRepository,
    private val setLogEntryRepository: SetLogEntryRepository,
    private val transactionScope: TransactionScope,
) {
    companion object {
        private const val TAG = "CompleteWorkoutUseCase"
    }

    suspend operator fun invoke(
        inProgressWorkout: WorkoutInProgressUiModel,
        programMetadata: ActiveProgramMetadata,
        workout: LoggingWorkout,
        completedSets: List<SetResult>,
        isDeloadWeek: Boolean,
    ) = transactionScope.execute {
        workoutInProgressRepository.delete()
        restTimerInProgressRepository.delete()

        val startTimeInMillis = inProgressWorkout.startTime.time
        val durationInMillis = (getCurrentDate().time - startTimeInMillis)

        // Increment the mesocycle and microcycle
        val microCycleComplete =
            (programMetadata.workoutCount - 1) == programMetadata.currentMicrocyclePosition
        val liftLevelDeloadsEnabled =
            SettingsManager.getSetting(
                LIFT_SPECIFIC_DELOADING,
                DEFAULT_LIFT_SPECIFIC_DELOADING
            )
        val deloadWeekComplete =
            !liftLevelDeloadsEnabled && microCycleComplete && isDeloadWeek
        val newMesoCycle =
            if (deloadWeekComplete) programMetadata.currentMesocycle + 1 else programMetadata.currentMesocycle
        val newMicroCycle =
            if (deloadWeekComplete) 0 else if (microCycleComplete) programMetadata.currentMicrocycle + 1 else programMetadata.currentMicrocycle
        val newMicroCyclePosition =
            if (microCycleComplete) 0 else programMetadata.currentMicrocyclePosition + 1
        val delta = programDelta {
            updateProgram(
                currentMesocycle = Patch.Set(newMesoCycle),
                currentMicrocycle = Patch.Set(newMicroCycle),
                currentMicrocyclePosition = Patch.Set(newMicroCyclePosition),
            )
        }
        val stopwatch = Stopwatch.createStarted()
        programsRepository.applyDelta(programMetadata.programId, delta)
        Log.d(TAG, "programsRepository.applyDelta ${stopwatch.elapsed(TimeUnit.MILLISECONDS)}ms")
        stopwatch.reset()

        stopwatch.start()
        // Get/create the historical workoutEntity name entry then use it to insert a workoutEntity log entry
        var historicalWorkoutNameId =
            historicalWorkoutNamesRepository.getIdByProgramAndWorkoutId(
                programId = programMetadata.programId,
                workoutId = workout.id,
            )
        Log.d(TAG, "historicalWorkoutNamesRepository.getIdByProgramAndWorkoutId ${stopwatch.elapsed(TimeUnit.MILLISECONDS)}ms")
        stopwatch.reset()

        if (historicalWorkoutNameId == null) {
            stopwatch.start()
            historicalWorkoutNameId = historicalWorkoutNamesRepository.insert(
                HistoricalWorkoutName(
                    programId = programMetadata.programId,
                    workoutId = workout.id,
                    programName = programMetadata.name,
                    workoutName = workout.name,
                )
            )
            Log.d(TAG, "historicalWorkoutNamesRepository.insert ${stopwatch.elapsed(TimeUnit.MILLISECONDS)}ms")
            stopwatch.reset()
        }

        stopwatch.start()
        val workoutLogEntryId = workoutLogRepository.insertWorkoutLogEntry(
            historicalWorkoutNameId = historicalWorkoutNameId,
            programDeloadWeek = programMetadata.deloadWeek,
            programWorkoutCount = programMetadata.workoutCount,
            mesoCycle = programMetadata.currentMesocycle,
            microCycle = programMetadata.currentMicrocycle,
            microcyclePosition = programMetadata.currentMicrocyclePosition,
            date = getCurrentDate(),
            durationInMillis = durationInMillis,
        )
        Log.d(TAG, "workoutLogRepository.insertWorkoutLogEntry ${stopwatch.elapsed(TimeUnit.MILLISECONDS)}ms")
        stopwatch.reset()

        stopwatch.start()
        // Update any Linear Progression failures
        // The reason this is done when the workout is completed is because if it were done on the fly
        // you'd have no easy way of knowing if someone failed (increment), changed result (still failure)
        // and then you get double increment. Or any variation of them going between success/failure by
        // modifying results.
        updateLinearProgressionFailures(
            completedSets = completedSets,
            workout = workout,
        )
        Log.d(TAG, "updateLinearProgressionFailures ${stopwatch.elapsed(TimeUnit.MILLISECONDS)}ms")
        stopwatch.reset()

        stopwatch.start()
        moveSetResultsToLogHistory(
            workoutLogEntryId = workoutLogEntryId,
            workout = workout,
            completedSets = completedSets,
        )
        Log.d(TAG, "moveSetResultsToLogHistory ${stopwatch.elapsed(TimeUnit.MILLISECONDS)}ms")
        stopwatch.reset()
    }

    private suspend fun moveSetResultsToLogHistory(
        workoutLogEntryId: Long,
        workout: LoggingWorkout,
        completedSets: List<SetResult>,
    ) {
        val liftsAndPositions = workout.lifts.associate {
            it.liftId to it.position
        }

        // If any lifts were changed and had completed results do not copy them
        val excludeFromCopy = completedSets.filter { result ->
            val liftPosition = liftsAndPositions[result.liftId]
            liftPosition != result.liftPosition
        }.map {
            it.id
        }.toHashSet()

        // Copy all of the set results from this workout into the set history table
        setLogEntryRepository.insertFromLiveWorkoutCompletedSets(
            workoutLogEntryId = workoutLogEntryId,
            excludeFromCopy = excludeFromCopy.toList(),
        )

        // Delete all live set results now that they were copied over
        liveWorkoutCompletedSetsRepository.deleteAll()
    }

    suspend fun updateLinearProgressionFailures(
        completedSets: List<SetResult>,
        workout: LoggingWorkout,
    ) {
        val resultsByLift = completedSets.associateBy {
            "${it.liftId}-${it.setPosition}"
        }
        val setResultsToUpdate = mutableListOf<SetResult>()
        workout.lifts
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
            liveWorkoutCompletedSetsRepository.upsertMany(setResultsToUpdate)
        }
    }
}