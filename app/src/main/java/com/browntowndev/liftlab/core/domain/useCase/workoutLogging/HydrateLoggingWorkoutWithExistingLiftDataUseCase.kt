package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift

/**
 * Copies over all modified sets from the list of lifts into the given logging workout
 */
class HydrateLoggingWorkoutWithExistingLiftDataUseCase {
    private data class ModifiedLiftKey(
        val workoutLiftId: Long,
        val liftId: Long,
        val liftPosition: Int,
    )

    private data class ModifiedSetKey(
        val position: Int,
        val myoRepSetPosition: Int?,
    )

    operator fun invoke(
        loggingWorkout: LoggingWorkout,
        liftsToUpdateFrom: List<LoggingWorkoutLift>
    ): LoggingWorkout {
        if (liftsToUpdateFrom.isEmpty() || loggingWorkout.lifts.isEmpty()) return loggingWorkout

        val modifiedSetsByLift = liftsToUpdateFrom.fastMapNotNull { lift ->
            val modifiedSetsForLift = lift.sets.filter { set ->
                (set.completedRpe != null ||
                        set.completedReps != null ||
                        set.completedWeight != null)
            }
            if (modifiedSetsForLift.isNotEmpty()) {
                val liftKey = ModifiedLiftKey(
                    workoutLiftId = lift.id,
                    liftId = lift.liftId,
                    liftPosition = lift.position
                )
                liftKey to modifiedSetsForLift
            } else null
        }.associate { it.first to it.second }

        return if (modifiedSetsByLift.isNotEmpty()) {
            Log.d("WorkoutViewModel", "modifiedSetsByLift: $modifiedSetsByLift")
             loggingWorkout.copy(
                lifts = loggingWorkout.lifts.fastMap { lift ->
                    val liftKey = ModifiedLiftKey(
                        workoutLiftId = lift.id,
                        liftId = lift.liftId,
                        liftPosition = lift.position
                    )
                    val modifiedSetsForLift = modifiedSetsByLift[liftKey]?.associateBy { set ->
                       ModifiedSetKey(
                            position = set.position,
                            myoRepSetPosition = (set as? LoggingMyoRepSet)?.myoRepSetPosition
                        )
                    }
                    if (modifiedSetsForLift == null) return@fastMap lift

                    lift.copy(
                        sets = lift.sets.fastMap { set ->
                            val setKey = ModifiedSetKey(
                                position = set.position,
                                myoRepSetPosition = (set as? LoggingMyoRepSet)?.myoRepSetPosition
                            )
                            val modifiedSet = modifiedSetsForLift[setKey]
                            if (modifiedSet == null) return@fastMap set

                            set.copyCompletionData(
                                complete = modifiedSet.complete,
                                completedWeight = modifiedSet.completedWeight,
                                completedReps = modifiedSet.completedReps,
                                completedRpe = modifiedSet.completedRpe,
                            )
                        }
                    )
                }
            )
        } else loggingWorkout
    }
}