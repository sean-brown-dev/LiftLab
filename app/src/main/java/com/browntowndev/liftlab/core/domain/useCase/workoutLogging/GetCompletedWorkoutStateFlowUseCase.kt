package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.toTimeString
import com.browntowndev.liftlab.core.data.mapping.SetResultMappingExtensions.toSetResult
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.models.workoutLogging.BuildSetResultUseCase
import com.browntowndev.liftlab.core.domain.models.workoutLogging.CompletedWorkoutState
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingDropSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import java.lang.Integer.max

class GetCompletedWorkoutStateFlowUseCase(
    private val workoutLogRepository: WorkoutLogRepository,
    private val buildSetResultUseCase: BuildSetResultUseCase,
) {
    private data class StageOneWorkoutState(
        val workoutLog: WorkoutLogEntry? = null,
        val duration: String? = null,
        val completedSetsFromLog: List<SetResult>? = null,
        val historicallyCompletedSetResults: List<SetResult>? = null,
        val programMetadata: ActiveProgramMetadata? = null,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(workoutLogEntryId: Long): Flow<CompletedWorkoutState> {
        return workoutLogRepository.getFlow(workoutLogEntryId = workoutLogEntryId)
            .flatMapLatest { workoutLog ->
                flowOf(
                    StageOneWorkoutState(
                        workoutLog = workoutLog,
                        duration = workoutLog.durationInMillis.toTimeString(),
                        programMetadata = ActiveProgramMetadata(
                            programId = workoutLog.programId,
                            name = workoutLog.programName,
                            deloadWeek = workoutLog.programDeloadWeek,
                            workoutCount = workoutLog.programWorkoutCount,
                            currentMesocycle = workoutLog.mesocycle,
                            currentMicrocycle = workoutLog.microcycle,
                            currentMicrocyclePosition = workoutLog.microcyclePosition,
                        ),
                    )
                )
            }.scan(StageOneWorkoutState()) { oldState, newState ->
                // Workout log is going to constantly change due to updates, but these result will not
                val historicalSetResults = if (oldState.historicallyCompletedSetResults == null && newState.workoutLog != null) {
                    workoutLogRepository.getMostRecentSetResultsForLiftIdsPriorToDate(
                        liftIds = newState.workoutLog.setLogEntries.map { it.liftId },
                        linearProgressionLiftIds = newState.workoutLog.setLogEntries
                            .filter { it.progressionScheme == ProgressionScheme.LINEAR_PROGRESSION }
                            .map { it.liftId }
                            .toSet(),
                        date = newState.workoutLog.date,
                    ).fastMap { setLogEntry ->
                        setLogEntry.toSetResult(
                            workoutId = newState.workoutLog.workoutId,
                            isLinearProgression = setLogEntry.progressionScheme == ProgressionScheme.LINEAR_PROGRESSION,
                        )
                    }
                } else oldState.historicallyCompletedSetResults

                newState.copy(
                    historicallyCompletedSetResults = historicalSetResults,
                )
            }.map { stageOneState ->
                if (stageOneState.workoutLog != null) {
                    val workout = buildLoggingWorkoutFromWorkoutLogs(
                        workoutLog = stageOneState.workoutLog,
                        previousResults = stageOneState.historicallyCompletedSetResults ?: emptyList())

                    val deloadWeekForLifts = workout.lifts
                        .associate { it.liftId to (it.deloadWeek ?: stageOneState.programMetadata!!.deloadWeek) }
                    val completedSetResults = buildCompletedSetResultsFromLog(
                        workoutLog = stageOneState.workoutLog,
                        deloadWeekForLifts = deloadWeekForLifts)
                    CompletedWorkoutState(
                        workout = workout,
                        duration = stageOneState.duration,
                        programMetadata = stageOneState.programMetadata,
                        completedSetsFromLog = completedSetResults,
                    )
                } else CompletedWorkoutState()
            }
    }

    private fun buildLoggingWorkoutFromWorkoutLogs(workoutLog: WorkoutLogEntry, previousResults: List<SetResult>): LoggingWorkout {
        val previousSetResults = previousResults
            .groupBy { it.liftId }
            .entries
            .associate {  lift ->
                lift.key to
                        lift.value.associateBy { "${it.setPosition}-${(it as? MyoRepSetResult)?.myoRepSetPosition}" }
            }
        var fauxWorkoutLiftId = 0L
        return LoggingWorkout(
            id = workoutLog.workoutId,
            name = workoutLog.workoutName,
            lifts = workoutLog.setLogEntries.groupBy { it.liftPosition }.map { groupedResults ->
                fauxWorkoutLiftId++
                val setLogEntry = groupedResults.value[0]
                LoggingWorkoutLift(
                    id = fauxWorkoutLiftId,
                    liftId = setLogEntry.liftId,
                    liftName = setLogEntry.liftName,
                    liftMovementPattern = setLogEntry.liftMovementPattern,
                    liftVolumeTypes = 0,
                    liftSecondaryVolumeTypes = null,
                    position = setLogEntry.liftPosition,
                    progressionScheme = setLogEntry.progressionScheme,
                    deloadWeek = max(setLogEntry.workoutLiftDeloadWeek ?: 0, workoutLog.programDeloadWeek),
                    incrementOverride = null,
                    restTime = null,
                    restTimerEnabled = false,
                    note = null,
                    sets = groupedResults.value
                        .sortedWith(
                            compareBy<SetLogEntry> { it.setPosition }
                                .thenBy { it.myoRepSetPosition ?: -1 }
                        ).fastMap { setLogEntry ->
                            when (setLogEntry.setType) {
                                SetType.STANDARD ->
                                    LoggingStandardSet(
                                        position = setLogEntry.setPosition,
                                        repRangeTop = setLogEntry.repRangeTop!!,
                                        repRangeBottom = setLogEntry.repRangeBottom!!,
                                        rpeTarget = setLogEntry.rpeTarget,
                                        weightRecommendation = setLogEntry.weightRecommendation,
                                        hadInitialWeightRecommendation = setLogEntry.weightRecommendation != null,
                                        previousSetResultLabel = getPreviousSetResultLabel(
                                            previousSetResults = previousSetResults,
                                            liftId = setLogEntry.liftId,
                                            setPosition = setLogEntry.setPosition,
                                            myoRepSetPosition = setLogEntry.myoRepSetPosition,
                                        ),
                                        repRangePlaceholder = "${setLogEntry.repRangeBottom}-${setLogEntry.repRangeTop}",
                                        complete = true,
                                        completedWeight = setLogEntry.weight,
                                        completedReps = setLogEntry.reps,
                                        completedRpe = setLogEntry.rpe,
                                    )
                                SetType.MYOREP ->
                                    LoggingMyoRepSet(
                                        position = setLogEntry.setPosition,
                                        myoRepSetPosition = setLogEntry.myoRepSetPosition,
                                        repRangeTop = setLogEntry.repRangeTop,
                                        repRangeBottom = setLogEntry.repRangeBottom,
                                        rpeTarget = setLogEntry.rpeTarget,
                                        weightRecommendation = setLogEntry.weightRecommendation,
                                        hadInitialWeightRecommendation = setLogEntry.weightRecommendation != null,
                                        previousSetResultLabel = getPreviousSetResultLabel(
                                            previousSetResults = previousSetResults,
                                            liftId = setLogEntry.liftId,
                                            setPosition = setLogEntry.setPosition,
                                            myoRepSetPosition = setLogEntry.myoRepSetPosition,
                                        ),
                                        repRangePlaceholder = if (setLogEntry.repRangeBottom != null && setLogEntry.repRangeTop != null) {
                                            "${setLogEntry.repRangeBottom}-${setLogEntry.repRangeTop}"
                                        } else "",
                                        setMatching = setLogEntry.setMatching!!,
                                        maxSets = setLogEntry.maxSets,
                                        repFloor = setLogEntry.repFloor,
                                        complete = true,
                                        completedWeight = setLogEntry.weight,
                                        completedReps = setLogEntry.reps,
                                        completedRpe = setLogEntry.rpe,
                                    )
                                SetType.DROP_SET ->
                                    LoggingDropSet(
                                        position = setLogEntry.setPosition,
                                        repRangeTop = setLogEntry.repRangeTop!!,
                                        repRangeBottom = setLogEntry.repRangeBottom!!,
                                        rpeTarget = setLogEntry.rpeTarget,
                                        weightRecommendation = setLogEntry.weightRecommendation,
                                        hadInitialWeightRecommendation = setLogEntry.weightRecommendation != null,
                                        previousSetResultLabel = getPreviousSetResultLabel(
                                            previousSetResults = previousSetResults,
                                            liftId = setLogEntry.liftId,
                                            setPosition = setLogEntry.setPosition,
                                            myoRepSetPosition = setLogEntry.myoRepSetPosition,
                                        ),
                                        repRangePlaceholder = "${setLogEntry.repRangeBottom}-${setLogEntry.repRangeTop}",
                                        dropPercentage = setLogEntry.dropPercentage!!,
                                        complete = true,
                                        completedWeight = setLogEntry.weight,
                                        completedReps = setLogEntry.reps,
                                        completedRpe = setLogEntry.rpe,
                                    )
                            }
                        }
                )
            }
        ).let { unsortedWorkout ->
            // Sort lifts & sets by position
            unsortedWorkout.copy(
                lifts = unsortedWorkout.lifts
                    .sortedBy { unsortedLift -> unsortedLift.position }
                    .map { sortedLift ->
                        sortedLift.copy(
                            sets = sortedLift.sets.sortedBy { unsortedSet -> unsortedSet.position }
                        )
                    }
            )
        }
    }

    private fun getPreviousSetResultLabel(
        previousSetResults: Map<Long, Map<String, SetResult>>,
        liftId: Long,
        setPosition: Int,
        myoRepSetPosition: Int?,
    ): String {
        val result = previousSetResults[liftId]?.get("$setPosition-$myoRepSetPosition")
        return if (result != null) {
            "${result.weight.toString().removeSuffix(".0")}x${result.reps} @${result.rpe}"
        } else {
            "—"
        }
    }

    private fun buildCompletedSetResultsFromLog(
        workoutLog: WorkoutLogEntry,
        deloadWeekForLifts: Map<Long, Int>,
    ): List<SetResult> {
        return workoutLog.setLogEntries.fastMap { setLogEntry ->
            val deloadWeek = deloadWeekForLifts[setLogEntry.liftId]
            val isDeload = deloadWeek == workoutLog.microcycle + 1
            buildSetResultUseCase(
                id = setLogEntry.id,
                workoutId = workoutLog.workoutId,
                currentMesocycle = workoutLog.mesocycle,
                currentMicrocycle = workoutLog.microcycle,
                weightRecommendation = setLogEntry.weightRecommendation,
                liftId = setLogEntry.liftId,
                setType = setLogEntry.setType,
                progressionScheme = setLogEntry.progressionScheme,
                liftPosition = setLogEntry.liftPosition,
                setPosition = setLogEntry.setPosition,
                myoRepSetPosition = setLogEntry.myoRepSetPosition,
                weight = setLogEntry.weight,
                reps = setLogEntry.reps,
                rpe = setLogEntry.rpe,
                isDeload = isDeload,
            )
        }
    }
}