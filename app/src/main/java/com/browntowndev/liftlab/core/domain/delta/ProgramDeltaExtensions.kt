package com.browntowndev.liftlab.core.domain.delta

import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift

// -----------------------------------------------
//                  Validation
// -----------------------------------------------

/**
 * Validates structural invariants. Throws IllegalArgumentException if invalid.
 *  - deleteProgram is exclusive (no other mutations in same delta)
 *  - Update items must have consistent parent ids (when > 0)
 *  - removeAllSets forbids sets/removedSetIds on that lift
 *  - Removal id lists contain only >0 unique ids
 */
fun ProgramDelta.validate(programId: Long) {
    // deleteProgram is exclusive
    require(!(deleteProgram && (programUpdate != null || workouts.isNotEmpty() || removedWorkoutIds.isNotEmpty()))) {
        "deleteProgram cannot be combined with other mutations."
    }

    // Removed workouts must be >0 and unique
    require(removedWorkoutIds.all { it > 0L } && removedWorkoutIds.distinct().size == removedWorkoutIds.size) {
        "removedWorkoutIds must be positive, unique ids."
    }

    workouts.forEach { workoutChange ->
        require(workoutChange.workoutId >= 0L)

        if (workoutChange.workoutId == 0L) {
            require(workoutChange.workoutInsert != null) {
                "workoutInsert must be provided when workoutId == 0L."
            }
            // Optional: ensure the insert’s programId is 0 or matches programId
            val insert = workoutChange.workoutInsert
            require(insert.programId == programId) {
                "Inserted workout.programId must match the program being inserted into: $programId."
            }
            require(workoutChange.workoutUpdate == null) {
                "workoutUpdate must be null when inserting a workout."
            }
        } else {
            // Existing workout
            require(workoutChange.workoutInsert == null) {
                "workoutInsert must be null when updating an existing workout."
            }
        }

        // Removed workout-lift ids must be >0 and unique
        require(
            workoutChange.removedWorkoutLiftIds.all { it > 0L } &&
                    workoutChange.removedWorkoutLiftIds.distinct().size == workoutChange.removedWorkoutLiftIds.size
        ) {
            "removedWorkoutLiftIds must be positive, unique ids."
        }

        workoutChange.lifts.forEach { liftChange ->
            val insertLift = liftChange.insertLift

            if (insertLift is StandardWorkoutLift) {
                require(liftChange.sets.isEmpty()) { "StandardWorkoutLift cannot include sets." }
                require(liftChange.removedSetIds.isEmpty()) { "StandardWorkoutLift cannot remove specific sets — use removeAllSets." }
                // removeAllSets=true is allowed as a defensive purge
            }

            // removeAllSets forbids additional set edits
            require(!(liftChange.removeAllSets && (liftChange.sets.isNotEmpty() || liftChange.removedSetIds.isNotEmpty()))) {
                "removeAllSets=true forbids sets/removedSetIds on the same lift."
            }

            // Removed set ids must be >0 and unique
            require(liftChange.removedSetIds.all { it > 0L } && liftChange.removedSetIds.distinct().size == liftChange.removedSetIds.size) {
                "removedSetIds must be positive, unique ids."
            }

            // For updates, set.workoutLiftId must equal parent lift id
            liftChange.sets.forEach { setChange ->
                val set = setChange.set
                if (set.id != 0L && liftChange.workoutLiftId != 0L) {
                    require(set.workoutLiftId == liftChange.workoutLiftId) {
                        "Set(id=${set.id}) workoutLiftId=${set.workoutLiftId} does not match parent Lift(id=${liftChange.workoutLiftId})"
                    }
                }
            }
        }
    }
}
