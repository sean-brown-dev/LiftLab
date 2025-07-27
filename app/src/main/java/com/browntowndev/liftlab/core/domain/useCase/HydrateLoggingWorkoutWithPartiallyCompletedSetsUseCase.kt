package com.browntowndev.liftlab.core.domain.useCase

import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.domain.models.LoggingDropSet
import com.browntowndev.liftlab.core.domain.models.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet

class HydrateLoggingWorkoutWithPartiallyCompletedSetsUseCase {
    fun hydrateWithPartiallyCompletedSets(
        loggingWorkout: LoggingWorkout
    ): LoggingWorkout {
        val partiallyCompletedSets = loggingWorkout.lifts.fastMapNotNull { lift ->
            val incompleteSetsForLift = lift.sets.filter { set ->
                !set.complete &&
                        (set.completedRpe != null ||
                                set.completedReps != null ||
                                set.completedWeight != null)
            }
            if (incompleteSetsForLift.isNotEmpty()) {
                Pair("${lift.id}-${lift.liftId}", incompleteSetsForLift)
            } else null
        }.associate { it.first to it.second }

        return if (partiallyCompletedSets.isNotEmpty()) {
            Log.d("WorkoutViewModel", "partiallyCompletedSets: $partiallyCompletedSets")
             loggingWorkout.copy(
                lifts = loggingWorkout.lifts.fastMap { lift ->
                    val partiallyCompletedSetsForLift = partiallyCompletedSets["${lift.id}-${lift.liftId}"]
                    if (partiallyCompletedSetsForLift == null) return@fastMap lift
                    lift.copy(
                        sets = lift.sets.fastMap { set ->
                            val incompleteSet = partiallyCompletedSetsForLift
                                .find { partiallyCompletedSet -> partiallyCompletedSet.position == set.position &&
                                        (partiallyCompletedSet as? LoggingMyoRepSet)?.myoRepSetPosition == (set as? LoggingMyoRepSet)?.myoRepSetPosition
                                }

                            if (incompleteSet != null) {
                                set.copyCompletionData(
                                    complete = false,
                                    completedWeight = incompleteSet.completedWeight,
                                    completedReps = incompleteSet.completedReps,
                                    completedRpe = incompleteSet.completedRpe,
                                )
                            } else set
                        }
                    )
                }
            )
        } else loggingWorkout
    }
}