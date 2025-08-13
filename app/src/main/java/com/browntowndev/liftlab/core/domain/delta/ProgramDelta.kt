package com.browntowndev.liftlab.core.domain.delta

// -----------------------------------------------
// ProgramDelta (domain/application layer)
// -----------------------------------------------

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.DropSet
import com.browntowndev.liftlab.core.domain.models.workout.MyoRepSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardSet
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import kotlin.time.Duration

/**
 * Domain-level intent over the Program aggregate (Program -> Workout -> WorkoutLift -> CustomSets).
 * Conventions:
 *  - id == 0L  => insert
 *  - id  > 0L  => update/delete target
 *
 * This type is storage-agnostic and expresses *what* to do, not *how* to persist.
 */
data class ProgramDelta(
    /** Delete the whole program aggregate. Repository will cascade deletes to workouts, lifts, sets, and tangential tables. */
    val deleteProgram: Boolean = false,

    /** Patch scalar program fields; nulls mean "no change". */
    val programUpdate: ProgramUpdate? = null,

    /** Upserts under the program (both inserts and updates). Order matters for ID propagation on inserts. */
    val workouts: List<WorkoutChange> = emptyList(),

    /** Soft-delete these workouts by *workout id*. Repository will cascade to lifts, sets, and tangential tables. */
    val removedWorkoutIds: List<Long> = emptyList()
) {

    data class ProgramUpdate(
        val name: String? = null,
        val isActive: Boolean? = null,
        val deloadWeek: Int? = null,
        val currentMesocycle: Int? = null,
        val currentMicrocycle: Int? = null,
        val currentMicrocyclePosition: Int? = null
    )

    data class WorkoutChange(
        /** The canonical workout id: id == 0L => inserting new, otherwise id must be > 0L */
        val workoutId: Long,

        /** A new workout to insert */
        val workoutInsert: Workout? = null,

        /** Update scalar fields of a canonical workout. Nulls mean "no change". */
        val workoutUpdate: WorkoutUpdate? = null,

        /**
         * Upserts under this workout. Order matters for ID propagation on inserts.
         * Only inserts/updates are listed here; deletions go in [removedWorkoutLiftIds].
         */
        val lifts: List<LiftChange> = workoutInsert?.lifts?.fastMap { lift ->
            val sets = if (lift is CustomWorkoutLift) {
                lift.customLiftSets.fastMap { set ->
                    val setToInsert = when (set) {
                        is StandardSet -> set.copy(id = 0L)
                        is MyoRepSet -> set.copy(id = 0L)
                        is DropSet -> set.copy(id = 0L)
                        else -> error("Unknown set type: $set")
                    }
                    SetChange(setToInsert)
                }
            } else emptyList()
            LiftChange(workoutLiftId = 0L, sets = sets)
        } ?: emptyList(),

        /** Soft-delete these *workout-lift primary keys*. Repository will cascade to sets. */
        val removedWorkoutLiftIds: List<Long> = emptyList()
    ) {
        data class WorkoutUpdate(
            val name: String? = null,
            val position: Int? = null,
        )

        data class LiftChange(
            /** The canonical workoutLiftId: id==0L => insert, id>0L => update. */
            val workoutLiftId: Long,

            /** The canonical workout-lift: id==0L => insert, id>0L => update. Its workoutId may be 0L for inserts. */
            val insertLift: GenericWorkoutLift? = null,

            /** Update scalar fields of a canonical workout-lift. Nulls mean "no change". */
            val liftUpdate: LiftUpdate? = null,

            /**
             * Upserts under this workout-lift. Order matters for ID propagation on inserts.
             * Only inserts/updates are listed here; deletions go in [removedSetIds].
             */
            val sets: List<SetChange> = emptyList(),

            /**
             * If true, repository will purge *all* sets under this workout-lift id (e.g., switched to Standard).
             * When true, [sets] and [removedSetIds] must be empty (enforced by validate()).
             */
            val removeAllSets: Boolean = false,

            /** Soft-delete these set ids. */
            val removedSetIds: List<Long> = emptyList()
        ) {
            data class LiftUpdate(
                val liftId: Long? = null,
                val position: Int? = null,
                val setCount: Int? = null,
                val progressionScheme: ProgressionScheme? = null,
                val deloadWeek: Int? = null,
                val incrementOverride: Float? = null,
                val restTime: Duration? = null,
                val restTimerEnabled: Boolean? = null,
                val repRangeTop: Int? = null,
                val repRangeBottom: Int? = null,
                val rpeTarget: Float? = null,
                val stepSize: Int? = null,
            )

            /** A single set upsert (id==0L => insert, id>0L => update). */
            data class SetChange(val set: GenericLiftSet)
        }
    }
}

/** Short aliases to keep call sites tidy. */
typealias ProgramUpdate = ProgramDelta.ProgramUpdate
typealias WorkoutChange = ProgramDelta.WorkoutChange
typealias WorkoutUpdate = ProgramDelta.WorkoutChange.WorkoutUpdate
typealias LiftChange = ProgramDelta.WorkoutChange.LiftChange
typealias LiftUpdate = ProgramDelta.WorkoutChange.LiftChange.LiftUpdate
typealias SetChange = ProgramDelta.WorkoutChange.LiftChange.SetChange
