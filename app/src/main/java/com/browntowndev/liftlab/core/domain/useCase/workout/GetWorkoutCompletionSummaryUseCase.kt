package com.browntowndev.liftlab.core.domain.useCase.workout

import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.domain.models.LiftCompletionSummary
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.PersonalRecord
import com.browntowndev.liftlab.core.domain.models.WorkoutCompletionSummary
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.useCase.utils.WeightCalculationUtils
import kotlin.collections.get

class GetWorkoutCompletionSummaryUseCase {
    operator fun invoke(
        loggingWorkout: LoggingWorkout,
        personalRecords: List<PersonalRecord>,
        completedSets: List<SetResult>,
    ): WorkoutCompletionSummary {
        val personalRecordsByLiftId = personalRecords.associateBy { it.liftId }
        val liftsById = loggingWorkout.lifts.associateBy { it.liftId }
        val liftEntityCompletionSummaries = completedSets
            .groupBy { "${it.liftId}-${it.liftPosition}" }
            .values.map { resultsForLift ->
                val lift = liftsById[resultsForLift[0].liftId]
                val setsCompleted = resultsForLift.size
                val totalSets = (lift?.setCount ?: setsCompleted)
                    .let { total ->
                        // Myo can meet this condition
                        if (setsCompleted > total) setsCompleted else total
                    }
                var bestSet1RM: Int? = null
                var bestSet: SetResult? = null
                resultsForLift.fastForEach { result ->
                    val oneRepMax = WeightCalculationUtils.getOneRepMax(
                        weight = if (result.weight > 0) result.weight else 1f,
                        reps = result.reps,
                        rpe = result.rpe
                    )
                    if (bestSet1RM == null || oneRepMax > bestSet1RM) {
                        bestSet = result
                        bestSet1RM = oneRepMax
                    }
                }

                LiftCompletionSummary(
                    liftName = lift?.liftName ?: "Unknown Lift",
                    liftId = lift?.liftId ?: -1,
                    liftPosition = lift?.position ?: -1,
                    setsCompleted = setsCompleted,
                    totalSets = totalSets,
                    bestSetReps = bestSet?.reps ?: 0,
                    bestSetWeight = bestSet?.weight ?: 0f,
                    bestSetRpe = bestSet?.rpe ?: 0f,
                    bestSet1RM = bestSet1RM ?: 0,
                    isNewPersonalRecord = personalRecordsByLiftId[lift?.liftId]?.let {
                        it.personalRecord < (bestSet1RM ?: -1)
                    } ?: false
                )
            }.toMutableList().apply {
                val liftsWithNoCompletedSets = liftsById.values.filter { loggingLift ->
                    !this.fastAny { summaryLift ->
                        summaryLift.liftId == loggingLift.liftId && summaryLift.liftPosition == loggingLift.position
                    }
                }

                addAll(
                    liftsWithNoCompletedSets.map { incompleteLift ->
                        LiftCompletionSummary(
                            liftName = incompleteLift.liftName,
                            liftId = incompleteLift.liftId,
                            liftPosition = incompleteLift.position,
                            setsCompleted = 0,
                            totalSets = incompleteLift.setCount,
                            bestSetReps = 0,
                            bestSetWeight = 0f,
                            bestSetRpe = 0f,
                            bestSet1RM = 0,
                            isNewPersonalRecord = false,
                        )
                    }
                )
            }.sortedBy { it.liftPosition }

        return WorkoutCompletionSummary(
            workoutName = loggingWorkout.name,
            liftCompletionSummaries = liftEntityCompletionSummaries
        )
    }
}