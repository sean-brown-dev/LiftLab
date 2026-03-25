package com.browntowndev.liftlab.core.domain.delta

import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.common.overwrite
import com.browntowndev.liftlab.core.domain.models.workout.Workout

/**
 * Builder for ProgramDelta.
 */
class ProgramDeltaBuilder {
    private var deleteProgramFlag: Boolean = false
    private var pendingProgramUpdate: ProgramDelta.ProgramUpdate? = null
    private val workoutChanges: MutableList<ProgramDelta.WorkoutChange> = mutableListOf()
    private val workoutIdsMarkedForRemoval: MutableList<Long> = mutableListOf()

    /** Mark the whole program for deletion (exclusive with other mutations). */
    fun deleteProgram() {
        deleteProgramFlag = true
    }

    /** Patch program fields (merges with previous). */
    fun updateProgram(modify: ProgramDelta.ProgramUpdate.() -> ProgramDelta.ProgramUpdate) {
        pendingProgramUpdate = (pendingProgramUpdate ?: ProgramUpdate()).let(modify)
    }

    /** Patch program fields (simple overload). */
    fun updateProgram(
        name: Patch<String> = Patch.Unset,
        isActive: Patch<Boolean> = Patch.Unset,
        deloadWeek: Patch<Int> = Patch.Unset,
        currentMesocycle: Patch<Int> = Patch.Unset,
        currentMicrocycle: Patch<Int> = Patch.Unset,
        currentMicrocyclePosition: Patch<Int> = Patch.Unset
    ) {
        updateProgram {
            copy(
                name = this.name.overwrite(name),
                isActive = this.isActive.overwrite(isActive),
                deloadWeek = this.deloadWeek.overwrite(deloadWeek),
                currentMesocycle = this.currentMesocycle.overwrite(currentMesocycle),
                currentMicrocycle = this.currentMicrocycle.overwrite(currentMicrocycle),
                currentMicrocyclePosition = this.currentMicrocyclePosition.overwrite(currentMicrocyclePosition)
            )
        }
    }

    /** Add a pre-built workout change. */
    fun workout(change: ProgramDelta.WorkoutChange) {
        workoutChanges += change
    }

    /** Build and add a workout change that will contain only lift changes via builder DSL. */
    fun workout(workoutId: Long, build: WorkoutChangeBuilder.() -> Unit) {
        workoutChanges += WorkoutChangeBuilder(workoutId).apply(build).build()
    }

    /** Build and add a workout change that will contain only lift changes via builder DSL. */
    suspend fun workoutSuspend(workoutId: Long, buildSuspending: suspend WorkoutChangeBuilder.() -> Unit) {
        workoutChanges += WorkoutChangeBuilder(workoutId).apply {
            buildSuspending()
        }.build()
    }

    /** Add a new workout via builder DSL. */
    fun workout(insertWorkout: Workout) {
        workoutChanges += WorkoutChangeBuilder(insertWorkout).build()
    }

    /** Update an existing workout and add lift changes via builder DSL. */
    fun workout(workoutId: Long, name: Patch<String> = Patch.Unset, position: Patch<Int> = Patch.Unset, build: WorkoutChangeBuilder.() -> Unit = { }) {
        workoutChanges += WorkoutChangeBuilder(workoutId, name, position).apply(build).build()
    }

    /** Update an existing workout and add lift changes via builder DSL. */
    fun workout(workoutId: Long, workoutUpdate: WorkoutUpdate, build: WorkoutChangeBuilder.() -> Unit = { }) {
        workoutChanges += WorkoutChangeBuilder(workoutId, workoutUpdate).apply(build).build()
    }

    /** Mark workouts for removal by their ids. */
    fun removeWorkouts(vararg workoutIds: Long) {
        workoutIdsMarkedForRemoval += workoutIds.toList()
    }

    fun build(): ProgramDelta =
        ProgramDelta(
            deleteProgram = deleteProgramFlag,
            programUpdate = pendingProgramUpdate,
            workouts = workoutChanges.toList(),
            removedWorkoutIds = workoutIdsMarkedForRemoval.toList()
        )
}
