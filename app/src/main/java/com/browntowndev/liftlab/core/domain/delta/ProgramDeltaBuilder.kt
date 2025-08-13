package com.browntowndev.liftlab.core.domain.delta

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
        name: String? = null,
        isActive: Boolean? = null,
        deloadWeek: Int? = null,
        currentMesocycle: Int? = null,
        currentMicrocycle: Int? = null,
        currentMicrocyclePosition: Int? = null
    ) {
        updateProgram {
            copy(
                name = name ?: this.name,
                isActive = isActive ?: this.isActive,
                deloadWeek = deloadWeek ?: this.deloadWeek,
                currentMesocycle = currentMesocycle ?: this.currentMesocycle,
                currentMicrocycle = currentMicrocycle ?: this.currentMicrocycle,
                currentMicrocyclePosition = currentMicrocyclePosition ?: this.currentMicrocyclePosition
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

    /** Add a new workout via builder DSL. */
    fun workout(insertWorkout: Workout) {
        workoutChanges += WorkoutChangeBuilder(insertWorkout).build()
    }

    /** Update an existing workout and add lift changes via builder DSL. */
    fun workout(workoutId: Long, name: String? = null, position: Int? = null, build: WorkoutChangeBuilder.() -> Unit = { }) {
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
