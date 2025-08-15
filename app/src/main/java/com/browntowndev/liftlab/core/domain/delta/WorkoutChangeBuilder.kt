package com.browntowndev.liftlab.core.domain.delta

import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta.WorkoutChange.WorkoutUpdate
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import kotlin.time.Duration

class WorkoutChangeBuilder {
    private var workoutId: Long = 0L
    private var workoutInsert: Workout? = null
    private var workoutUpdate: WorkoutUpdate? = null
    private val liftChanges: MutableList<LiftChange> = mutableListOf()
    private val workoutLiftIdsMarkedForRemoval: MutableList<Long> = mutableListOf()

    /** Construct a builder for lift changes only */
    constructor(workoutId: Long) {
        this.workoutId = workoutId
    }

    /**
     * Insert a new workout (id will be generated). The repository will set programId for you.
     */
    constructor(insertWorkout: Workout) {
        this.workoutId = 0L
        this.workoutInsert = insertWorkout
    }

    /**
     * Target an existing workout to patch scalar fields.
     */
    constructor(workoutId: Long, update: WorkoutUpdate) {
        this.workoutId = workoutId
        this.workoutUpdate = update
    }

    /**
     * Target an existing workout to patch scalar fields.
     */
    constructor(
        workoutId: Long,
        name: Patch<String> = Patch.Unset,
        position: Patch<Int> = Patch.Unset
    ) {
        this.workoutId = workoutId
        this.workoutInsert = null
        this.workoutUpdate = WorkoutUpdate(
            name = name,
            position = position
        )
    }

    /** Insert a new lift via DSL. */
    fun insertLift(insertWorkoutLift: GenericWorkoutLift) {
        liftChanges += LiftChangeBuilder(insertWorkoutLift).build()
    }

    /** Build a lift with set changes via DSL. */
    fun updateSets(workoutLiftId: Long, build: LiftChangeBuilder.() -> Unit) {
        liftChanges += LiftChangeBuilder(workoutLiftId).apply(build).build()
    }

    /** Add a pre-built lift change. */
    fun updateLift(change: LiftChange) {
        liftChanges += change
    }

    /** Update an existing lift and add set changes via DSL. */
    fun updateLift(
        workoutLiftId: Long,
        liftId: Patch<Long> = Patch.Unset,
        position: Patch<Int> = Patch.Unset,
        setCount: Patch<Int> = Patch.Unset,
        progressionScheme: Patch<ProgressionScheme> = Patch.Unset,
        deloadWeek: Patch<Int?> = Patch.Unset,
        incrementOverride: Patch<Float?> = Patch.Unset,
        restTime: Patch<Duration?> = Patch.Unset,
        restTimerEnabled: Patch<Boolean?> = Patch.Unset,
        repRangeTop: Patch<Int?> = Patch.Unset,
        repRangeBottom: Patch<Int?> = Patch.Unset,
        rpeTarget: Patch<Float?> = Patch.Unset,
        stepSize: Patch<Int?> = Patch.Unset,
        build: LiftChangeBuilder.() -> Unit = { }
    ) {
        val liftUpdate = LiftUpdate(
            liftId = liftId,
            position = position,
            setCount = setCount,
            progressionScheme = progressionScheme,
            deloadWeek = deloadWeek,
            incrementOverride = incrementOverride,
            restTime = restTime,
            restTimerEnabled = restTimerEnabled,
            repRangeTop = repRangeTop,
            repRangeBottom = repRangeBottom,
            rpeTarget = rpeTarget,
            stepSize = stepSize
        )
        liftChanges += LiftChangeBuilder(workoutLiftId, liftUpdate).apply(build).build()
    }

    /** Update an existing lift and add set changes via DSL. */
    fun updateLift(workoutLiftId: Long, liftUpdate: LiftUpdate, build: LiftChangeBuilder.() -> Unit = { }) {
        liftChanges += LiftChangeBuilder(workoutLiftId, liftUpdate).apply(build).build()
    }

    /** Mark workout-lifts for removal by their primary keys. */
    fun removeWorkoutLifts(vararg workoutLiftIds: Long) {
        workoutLiftIdsMarkedForRemoval += workoutLiftIds.toList()
    }

    fun build(): ProgramDelta.WorkoutChange =
        WorkoutChange(
            workoutId = workoutId,
            workoutInsert = workoutInsert,
            workoutUpdate = workoutUpdate,
            lifts = liftChanges.toList(),
            removedWorkoutLiftIds = workoutLiftIdsMarkedForRemoval.toList()
        )
}
