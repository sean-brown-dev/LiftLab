package com.browntowndev.liftlab.core.data.mapping

import com.browntowndev.liftlab.core.data.local.entities.LiveWorkoutCompletedSetEntity
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.workoutLogging.StandardSetResult


fun SetLogEntry.toSetResult(workoutId: Long, isLinearProgression: Boolean): SetResult {
    return when (setType) {
        SetType.STANDARD,
        SetType.DROP_SET -> {
            if (isLinearProgression) {
                toLpSetResult(workoutId)
            } else {
                toStandardSetResult(workoutId)
            }
        }

        SetType.MYOREP -> toMyoRepSetResult(workoutId)
    }
}

fun LiveWorkoutCompletedSetEntity.toSetResult(): SetResult {
    return when (setType) {
        SetType.STANDARD,
        SetType.DROP_SET -> {
            if (missedLpGoals != null) {
                toLpSetResult()
            } else {
                toStandardSetResult()
            }
        }

        SetType.MYOREP -> toMyoRepSetResult()
    }
}

fun SetResult.toEntity(): LiveWorkoutCompletedSetEntity {
    return when (this) {
        is StandardSetResult -> toEntity()
        is MyoRepSetResult -> toEntity()
        is LinearProgressionSetResult -> toEntity()
        else -> throw Exception("${this::class.simpleName} is not defined.")
    }
}

private fun LiveWorkoutCompletedSetEntity.toLpSetResult(): LinearProgressionSetResult {
    return LinearProgressionSetResult(
        id = id,
        workoutId = workoutId,
        liftId = liftId,
        liftPosition = liftPosition,
        setPosition = setPosition,
        weight = weight,
        rpe = rpe,
        reps = reps,
        persistedOneRepMax = oneRepMax,
        missedLpGoals = missedLpGoals!!,
        isDeload = isDeload,
    )
}

private fun LiveWorkoutCompletedSetEntity.toMyoRepSetResult(): MyoRepSetResult {
    return MyoRepSetResult(
        id = id,
        workoutId = workoutId,
        liftId = liftId,
        liftPosition = liftPosition,
        setPosition = setPosition,
        myoRepSetPosition = myoRepSetPosition,
        weight = weight,
        rpe = rpe,
        reps = reps,
        persistedOneRepMax = oneRepMax,
        isDeload = isDeload,
    )
}

private fun LiveWorkoutCompletedSetEntity.toStandardSetResult(): StandardSetResult {
    return StandardSetResult(
        id = id,
        setType = setType,
        workoutId = workoutId,
        liftId = liftId,
        liftPosition = liftPosition,
        setPosition = setPosition,
        weight = weight,
        rpe = rpe,
        reps = reps,
        persistedOneRepMax = oneRepMax,
        isDeload = isDeload,
    )
}

private fun SetLogEntry.toLpSetResult(workoutId: Long): LinearProgressionSetResult {
    return LinearProgressionSetResult(
        id = id,
        workoutId = workoutId,
        liftId = liftId,
        liftPosition = liftPosition,
        setPosition = setPosition,
        weight = weight,
        rpe = rpe,
        reps = reps,
        persistedOneRepMax = oneRepMax,
        missedLpGoals = 0,
        isDeload = isDeload,
    )
}

private fun SetLogEntry.toMyoRepSetResult(workoutId: Long): MyoRepSetResult {
    return MyoRepSetResult(
        id = id,
        workoutId = workoutId,
        liftId = liftId,
        liftPosition = liftPosition,
        setPosition = setPosition,
        myoRepSetPosition = myoRepSetPosition,
        weight = weight,
        rpe = rpe,
        reps = reps,
        persistedOneRepMax = oneRepMax,
        isDeload = isDeload,
    )
}

private fun SetLogEntry.toStandardSetResult(workoutId: Long): StandardSetResult {
    return StandardSetResult(
        id = id,
        setType = setType,
        workoutId = workoutId,
        liftId = liftId,
        liftPosition = liftPosition,
        setPosition = setPosition,
        weight = weight,
        rpe = rpe,
        reps = reps,
        persistedOneRepMax = oneRepMax,
        isDeload = isDeload,
    )
}

private fun StandardSetResult.toEntity(): LiveWorkoutCompletedSetEntity {
    return LiveWorkoutCompletedSetEntity(
        id = id,
        setType = setType,
        workoutId = workoutId,
        liftId = liftId,
        liftPosition = liftPosition,
        setPosition = setPosition,
        weight = weight,
        rpe = rpe,
        reps = reps,
        oneRepMax = oneRepMax,
        isDeload = isDeload,
    )
}

private fun MyoRepSetResult.toEntity(): LiveWorkoutCompletedSetEntity {
    return LiveWorkoutCompletedSetEntity(
        id = id,
        setType = SetType.MYOREP,
        workoutId = workoutId,
        liftId = liftId,
        liftPosition = liftPosition,
        setPosition = setPosition,
        myoRepSetPosition = myoRepSetPosition,
        weight = weight,
        rpe = rpe,
        reps = reps,
        oneRepMax = oneRepMax,
        isDeload = isDeload,
    )
}

private fun LinearProgressionSetResult.toEntity(): LiveWorkoutCompletedSetEntity {
    return LiveWorkoutCompletedSetEntity(
        id = id,
        setType = SetType.STANDARD,
        workoutId = workoutId,
        liftId = liftId,
        liftPosition = liftPosition,
        setPosition = setPosition,
        weight = weight,
        rpe = rpe,
        reps = reps,
        oneRepMax = oneRepMax,
        missedLpGoals = missedLpGoals,
        isDeload = isDeload,
    )
}
