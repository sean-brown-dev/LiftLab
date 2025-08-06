package com.browntowndev.liftlab.core.domain.models.metrics

import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry

/**
 * Represents the ID of a specific workout log.
 */
@JvmInline
value class WorkoutLogId(val value: Long)

/**
 * Represents the ID of a specific lift.
 */
@JvmInline
value class LiftId(val value: Long)

/**
 * A summary of top sets across multiple workout sessions.
 * This class is a direct, type-safe replacement for the original complex map.
 */
data class AllWorkoutTopSets(
    private val topSetsByWorkout: Map<WorkoutLogId, WorkoutTopSets>
) {
    val size get() = topSetsByWorkout.size

    fun isEmpty() = topSetsByWorkout.isEmpty()

    /**
     * Retrieves the top sets for a specific workout.
     */
    operator fun get(workoutLogId: WorkoutLogId): WorkoutTopSets? = topSetsByWorkout[workoutLogId]

    /**
     * Contains a summary of the top sets for all exercises
     * performed within one specific workout session.
     */
    data class WorkoutTopSets(
        // The map is private to ensure access only through our safe functions
        private val recordsByExercise: Map<LiftId, TopSet>
    ) {
        val liftIds: Set<LiftId> get() = recordsByExercise.keys

        val topSets: List<TopSet> get() = recordsByExercise.values.toList()

        val size get() = recordsByExercise.size

        /**
         * The number of personal records across all exercises.
         */
        val personalRecordCount get() = recordsByExercise.count { it.value.setLog.isPersonalRecord }

        /**
         * Retrieves the top set for a specific lift.
         */
        operator fun get(id: LiftId): TopSet? = recordsByExercise[id]

        /**
         * Represents the single top set of a specific exercise, performed
         * within a single workout session.
         */
        data class TopSet(
            val setCount: Int,
            val setLog: SetLogEntry
        ) {
            val isPersonalRecord get() = setLog.isPersonalRecord
            val reps get() = setLog.reps
            val weight get() = setLog.weight
            val rpe get() = setLog.rpe
            val liftName get() = setLog.liftName
        }
    }
}
