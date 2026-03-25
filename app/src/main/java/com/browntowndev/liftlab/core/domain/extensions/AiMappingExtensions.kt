package com.browntowndev.liftlab.core.domain.extensions

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.programConfiguration.ProgramPayload
import com.browntowndev.liftlab.core.domain.models.programConfiguration.WorkoutCore
import com.browntowndev.liftlab.core.domain.models.programConfiguration.WorkoutLiftCore
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout

fun ProgramPayload.toProgramDomainModel(lifts: List<Lift>): Program {
    val liftsById = lifts.associateBy { it.id }
    return Program(
        name = program.name,
        deloadWeek = program.deloadWeek,
        workouts = workouts.fastMap {
            it.toWorkoutDomainModel(liftsById)
        },
    )
}

fun WorkoutCore.toWorkoutDomainModel(liftsById: Map<Long, Lift>): Workout =
    Workout(
        programId = 0L,
        name = name,
        position = position,
        lifts = workoutLifts.fastMap {
            val lift = liftsById[it.liftId] ?: error("Lift not found")
            it.toWorkoutLiftDomainModel(lift)
        }
    )

fun WorkoutLiftCore.toWorkoutLiftDomainModel(lift: Lift): StandardWorkoutLift =
    StandardWorkoutLift(
        workoutId = 0L,
        liftId = liftId,
        liftName = lift.name,
        liftMovementPattern = lift.movementPattern,
        liftVolumeTypes = lift.volumeTypesBitmask,
        liftSecondaryVolumeTypes = lift.secondaryVolumeTypesBitmask,
        incrementOverride = lift.incrementOverride,
        restTime = lift.restTime,
        restTimerEnabled = lift.restTimerEnabled,
        liftNote = lift.note,
        progressionScheme = ProgressionScheme.valueOf(progressionScheme),
        position = position,
        setCount = setCount,
        deloadWeek = deloadWeek,
        rpeTarget = rpeTarget.toFloat(),
        repRangeBottom = repRangeBottom,
        repRangeTop = repRangeTop,
        stepSize = stepSize,
    )
