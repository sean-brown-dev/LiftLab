package com.browntowndev.liftlab.core.domain.useCase.workout

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.models.HistoricalWorkoutName
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.repositories.HistoricalWorkoutNamesRepository
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import com.browntowndev.liftlab.ui.models.workout.WorkoutInProgressUiModel

class CompleteWorkoutUseCase(
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val restTimerInProgressRepository: RestTimerInProgressRepository,
    private val programsRepository: ProgramsRepository,
    private val historicalWorkoutNamesRepository: HistoricalWorkoutNamesRepository,
    private val workoutLogRepository: WorkoutLogRepository,
    private val setResultsRepository: PreviousSetResultsRepository,
    private val setLogEntryRepository: SetLogEntryRepository,
) {
    // Operator function allows calling it like a function: completeWorkoutUseCase(...)
    suspend operator fun invoke(
        inProgressWorkout: WorkoutInProgressUiModel, // Pass in the necessary data from the ViewModel's state
        programMetadata: ActiveProgramMetadata,
        workout: LoggingWorkout,
        completedSets: List<SetResult>,
        isDeloadWeek: Boolean,
    ) {
            // Remove the workoutEntity from in progress
            workoutInProgressRepository.deleteAll()
            restTimerInProgressRepository.deleteAll()

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
            programsRepository.updateMesoAndMicroCycle(
                id = programMetadata.programId,
                mesoCycle = newMesoCycle,
                microCycle = newMicroCycle,
                microCyclePosition = newMicroCyclePosition,
            )

            // Get/create the historical workoutEntity name entry then use it to insert a workoutEntity log entry
            var historicalWorkoutNameId =
                historicalWorkoutNamesRepository.getIdByProgramAndWorkoutId(
                    programId = programMetadata.programId,
                    workoutId = workout.id,
                )
            if (historicalWorkoutNameId == null) {
                historicalWorkoutNameId = historicalWorkoutNamesRepository.insert(
                    HistoricalWorkoutName(
                        programId = programMetadata.programId,
                        workoutId = workout.id,
                        programName = programMetadata.name,
                        workoutName = workout.name,
                    )
                )
            }
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

            moveSetResultsToLogHistory(
                workoutLogEntryId = workoutLogEntryId,
                programMetadata = programMetadata,
                workout = workout,
                completedSets = completedSets,
            )

            // Update any Linear Progression failures
            // The reason this is done when the workout is completed is because if it were done on the fly
            // you'd have no easy way of knowing if someone failed (increment), changed result (still failure)
            // and then you get double increment. Or any variation of them going between success/failure by
            // modifying results.
            updateLinearProgressionFailures(
                completedSets = completedSets,
                workout = workout,
            )
    }

    private suspend fun moveSetResultsToLogHistory(
        workoutLogEntryId: Long,
        programMetadata: ActiveProgramMetadata,
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

        // If someone marked a myorep set incomplete in the middle of a sequence and didn't
        // finish it, fix the myorep set positions to fill the gap
        val myoRepSetsToSynchronizePositonsFor = completedSets
            .filterIsInstance<MyoRepSetResult>()
            .filter { it.id !in excludeFromCopy }
            .groupBy { "${it.liftId}-${it.liftPosition}" }
            .values
            .flatMap { resultsForLift ->
                resultsForLift.sortedBy { it.id }.mapIndexedNotNull { index, result ->
                    val expectedMyoRepSetPosition: Int? = if (index == 0) null else index - 1
                    if (result.myoRepSetPosition != expectedMyoRepSetPosition) {
                        result.copy(myoRepSetPosition = expectedMyoRepSetPosition)
                    } else null
                }
            }

        if (myoRepSetsToSynchronizePositonsFor.isNotEmpty()) {
            setResultsRepository.upsertMany(myoRepSetsToSynchronizePositonsFor)
        }

        // Copy all of the set results from this workout into the set history table
        setLogEntryRepository.insertFromPreviousSetResults(
            workoutLogEntryId = workoutLogEntryId,
            workoutId = workout.id,
            mesocycle = programMetadata.currentMesocycle,
            microcycle = programMetadata.currentMicrocycle,
            excludeFromCopy = excludeFromCopy.toList(),
        )

        // Get all the set results for deloaded lifts
        val deloadSetResults = workout.lifts
            .filter { workoutLift ->
                // workoutEntity lifts whose deload week it is
                val deloadWeek = (workoutLift.deloadWeek ?: programMetadata.deloadWeek) - 1
                deloadWeek == programMetadata.currentMicrocycle
            }.fastMap {
                // key that can be used to match set results
                "${it.liftId}-${it.position}"
            }.toHashSet().let { deloadedWorkoutLiftIds ->
                // set results for deloaded workoutEntity lifts
                completedSets
                    .filter { deloadedWorkoutLiftIds.contains("${it.liftId}-${it.liftPosition}") }
                    .fastMap { it.id }
            }

        // Delete all set results from the previous workoutEntity OR ones that were deloaded. Deloaded
        // ones are deleted so next progressions are calculated using most recent non-deload results
        setResultsRepository.deleteAllForPreviousWorkout(
            workoutId = workout.id,
            currentMesocycle = programMetadata.currentMesocycle,
            currentMicrocycle = programMetadata.currentMicrocycle,
            currentResultsToDeleteInstead = deloadSetResults,
        )
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
            setResultsRepository.upsertMany(setResultsToUpdate)
        }
    }
}