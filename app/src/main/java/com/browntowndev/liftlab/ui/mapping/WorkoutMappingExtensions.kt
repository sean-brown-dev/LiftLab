package com.browntowndev.liftlab.ui.mapping

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.domain.enums.toVolumeTypes
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.DropSet
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.models.workout.MyoRepSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.ui.models.workout.CustomLiftSetUiModel
import com.browntowndev.liftlab.ui.models.workout.CustomWorkoutLiftUiModel
import com.browntowndev.liftlab.ui.models.workout.DropSetUiModel
import com.browntowndev.liftlab.ui.models.workout.LiftUiModel
import com.browntowndev.liftlab.ui.models.workout.MyoRepSetUiModel
import com.browntowndev.liftlab.ui.models.workout.StandardSetUiModel
import com.browntowndev.liftlab.ui.models.workout.StandardWorkoutLiftUiModel
import com.browntowndev.liftlab.ui.models.workout.WorkoutLiftUiModel
import com.browntowndev.liftlab.ui.models.workout.WorkoutUiModel

fun Workout.toUiModel(): WorkoutUiModel {
    return WorkoutUiModel(
        id = id,
        programId = programId,
        name = name,
        position = position,
        lifts = lifts.map { it.toUiModel() }
    )
}

fun WorkoutUiModel.toDomainModel(): Workout {
    return Workout(
        id = id,
        programId = programId,
        name = name,
        position = position,
        lifts = lifts.map { it.toDomainModel() }
    )
}

fun GenericWorkoutLift.toUiModel(): WorkoutLiftUiModel {
    return when (this) {
        is StandardWorkoutLift -> toUiModel()
        is CustomWorkoutLift -> toUiModel()
        else -> throw IllegalArgumentException("Unknown GenericWorkoutLift type")
    }
}

fun WorkoutLiftUiModel.toDomainModel(): GenericWorkoutLift {
    return when (this) {
        is StandardWorkoutLiftUiModel -> toDomainModel()
        is CustomWorkoutLiftUiModel -> toDomainModel()
        else -> throw IllegalArgumentException("Unknown WorkoutLiftUiModel type")
    }
}

fun StandardWorkoutLift.toUiModel(): StandardWorkoutLiftUiModel =
    StandardWorkoutLiftUiModel(
        id = id,
        workoutId = workoutId,
        liftId = liftId,
        liftName = liftName,
        liftMovementPattern = liftMovementPattern,
        liftVolumeTypes = liftVolumeTypes,
        liftSecondaryVolumeTypes = liftSecondaryVolumeTypes,
        liftNote = liftNote,
        position = position,
        setCount = setCount,
        progressionScheme = progressionScheme,
        deloadWeek = deloadWeek,
        incrementOverride = incrementOverride,
        restTime = restTime,
        restTimerEnabled = restTimerEnabled,
        rpeTarget = rpeTarget,
        repRangeBottom = repRangeBottom,
        repRangeTop = repRangeTop,
        stepSize = stepSize,
    )

fun StandardWorkoutLiftUiModel.toDomainModel(): StandardWorkoutLift =
    StandardWorkoutLift(
        id = id,
        workoutId = workoutId,
        liftId = liftId,
        liftName = liftName,
        liftMovementPattern = liftMovementPattern,
        liftVolumeTypes = liftVolumeTypes,
        liftSecondaryVolumeTypes = liftSecondaryVolumeTypes,
        liftNote = liftNote,
        position = position,
        setCount = setCount,
        progressionScheme = progressionScheme,
        deloadWeek = deloadWeek,
        incrementOverride = incrementOverride,
        restTime = restTime,
        restTimerEnabled = restTimerEnabled,
        rpeTarget = rpeTarget,
        repRangeBottom = repRangeBottom,
        repRangeTop = repRangeTop,
        stepSize = stepSize,
    )

fun CustomWorkoutLift.toUiModel(): CustomWorkoutLiftUiModel =
    CustomWorkoutLiftUiModel(
        id = id,
        workoutId = workoutId,
        liftId = liftId,
        liftName = liftName,
        liftMovementPattern = liftMovementPattern,
        liftVolumeTypes = liftVolumeTypes,
        liftSecondaryVolumeTypes = liftSecondaryVolumeTypes,
        liftNote = liftNote,
        position = position,
        setCount = setCount,
        progressionScheme = progressionScheme,
        deloadWeek = deloadWeek,
        incrementOverride = incrementOverride,
        restTime = restTime,
        restTimerEnabled = restTimerEnabled,
        customLiftSets = customLiftSets.fastMap { it.toUiModel() }
    )

fun CustomWorkoutLiftUiModel.toDomainModel(): CustomWorkoutLift =
    CustomWorkoutLift(
        id = id,
        workoutId = workoutId,
        liftId = liftId,
        liftName = liftName,
        liftMovementPattern = liftMovementPattern,
        liftVolumeTypes = liftVolumeTypes,
        liftSecondaryVolumeTypes = liftSecondaryVolumeTypes,
        liftNote = liftNote,
        position = position,
        progressionScheme = progressionScheme,
        deloadWeek = deloadWeek,
        incrementOverride = incrementOverride,
        restTime = restTime,
        restTimerEnabled = restTimerEnabled,
        customLiftSets = customLiftSets.fastMap { it.toDomainModel() }
    )

fun GenericLiftSet.toUiModel(): CustomLiftSetUiModel =
    when (this) {
        is StandardSet -> StandardSetUiModel(
            id = id,
            workoutLiftId = workoutLiftId,
            position = position,
            rpeTarget = rpeTarget,
            repRangeBottom = repRangeBottom,
            repRangeTop = repRangeTop
        )
        is MyoRepSet -> MyoRepSetUiModel(
            id = id,
            workoutLiftId = workoutLiftId,
            position = position,
            rpeTarget = rpeTarget,
            repRangeBottom = repRangeBottom,
            repRangeTop = repRangeTop,
            repFloor = repFloor,
            maxSets = maxSets,
            setMatching = setMatching,
            setGoal = setGoal
        )
        is DropSet -> DropSetUiModel(
            id = id,
            workoutLiftId = workoutLiftId,
            position = position,
            rpeTarget = rpeTarget,
            repRangeBottom = repRangeBottom,
            repRangeTop = repRangeTop,
            dropPercentage = dropPercentage
        )
        else -> throw IllegalArgumentException("Unknown GenericLiftSet type")
    }

fun CustomLiftSetUiModel.toDomainModel(): GenericLiftSet =
    when (this) {
        is StandardSetUiModel -> StandardSet(
            id = id,
            workoutLiftId = workoutLiftId,
            position = position,
            rpeTarget = rpeTarget,
            repRangeBottom = repRangeBottom,
            repRangeTop = repRangeTop
        )
        is MyoRepSetUiModel -> MyoRepSet(
            id = id,
            workoutLiftId = workoutLiftId,
            position = position,
            rpeTarget = rpeTarget,
            repRangeBottom = repRangeBottom,
            repRangeTop = repRangeTop,
            repFloor = repFloor,
            maxSets = maxSets,
            setMatching = setMatching,
            setGoal = setGoal
        )
        is DropSetUiModel -> DropSet(
            id = id,
            workoutLiftId = workoutLiftId,
            position = position,
            rpeTarget = rpeTarget,
            repRangeBottom = repRangeBottom,
            repRangeTop = repRangeTop,
            dropPercentage = dropPercentage
        )
        else -> throw IllegalArgumentException("Unknown CustomLiftSetUiModel type")
    }

fun Lift.toUiModel(): LiftUiModel {
    return LiftUiModel(
        id = this.id,
        name = this.name,
        movementPattern = this.movementPattern,
        volumeTypes = this.volumeTypesBitmask.toVolumeTypes(),
        secondaryVolumeTypes = this.secondaryVolumeTypesBitmask?.toVolumeTypes() ?: emptyList(),
        incrementOverride = this.incrementOverride,
        restTime = this.restTime,
        restTimerEnabled = this.restTimerEnabled,
        isBodyweight = this.isBodyweight,
        note = this.note
    )
}

fun LiftUiModel.toDomainModel(): Lift {
    return Lift(
        id = this.id,
        name = this.name,
        movementPattern = this.movementPattern,
        volumeTypesBitmask = this.volumeTypes.sumOf { it.bitMask },
        secondaryVolumeTypesBitmask = this.secondaryVolumeTypes.sumOf { it.bitMask  },
        incrementOverride = this.incrementOverride,
        restTime = this.restTime,
        restTimerEnabled = this.restTimerEnabled,
        isBodyweight = this.isBodyweight,
        note = this.note
    )
}
