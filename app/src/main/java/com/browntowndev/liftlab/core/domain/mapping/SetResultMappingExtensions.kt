package com.browntowndev.liftlab.core.domain.mapping

import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.domain.models.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.StandardSetResult
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.entities.room.PreviousSetResultEntity

object SetResultMappingExtensions {

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

    fun PreviousSetResultEntity.toSetResult(): SetResult {
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

    fun SetResult.toEntity(): PreviousSetResultEntity {
        return when (this) {
            is StandardSetResult -> toEntity()
            is MyoRepSetResult -> toEntity()
            is LinearProgressionSetResult -> toEntity()
            else -> throw Exception("${this::class.simpleName} is not defined.")
        }
    }

    private fun PreviousSetResultEntity.toLpSetResult(): LinearProgressionSetResult {
        return LinearProgressionSetResult(
            id = id,
            workoutId = workoutId,
            liftId = liftId,
            mesoCycle = mesoCycle,
            microCycle = microCycle,
            liftPosition = liftPosition,
            setPosition = setPosition,
            weightRecommendation = weightRecommendation,
            weight = weight,
            rpe = rpe,
            reps = reps,
            persistedOneRepMax = oneRepMax,
            missedLpGoals = missedLpGoals!!,
            isDeload = isDeload,
        )
    }

    private fun PreviousSetResultEntity.toMyoRepSetResult(): MyoRepSetResult {
        return MyoRepSetResult(
            id = id,
            workoutId = workoutId,
            liftId = liftId,
            mesoCycle = mesoCycle,
            microCycle = microCycle,
            liftPosition = liftPosition,
            setPosition = setPosition,
            myoRepSetPosition = myoRepSetPosition,
            weightRecommendation = weightRecommendation,
            weight = weight,
            rpe = rpe,
            reps = reps,
            persistedOneRepMax = oneRepMax,
            isDeload = isDeload,
        )
    }

    private fun PreviousSetResultEntity.toStandardSetResult(): StandardSetResult {
        return StandardSetResult(
            id = id,
            setType = setType,
            workoutId = workoutId,
            liftId = liftId,
            mesoCycle = mesoCycle,
            microCycle = microCycle,
            liftPosition = liftPosition,
            setPosition = setPosition,
            weightRecommendation = weightRecommendation,
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
            mesoCycle = mesoCycle,
            microCycle = microCycle,
            liftPosition = liftPosition,
            setPosition = setPosition,
            weightRecommendation = weightRecommendation,
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
            mesoCycle = mesoCycle,
            microCycle = microCycle,
            liftPosition = liftPosition,
            setPosition = setPosition,
            myoRepSetPosition = myoRepSetPosition,
            weightRecommendation = weightRecommendation,
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
            mesoCycle = mesoCycle,
            microCycle = microCycle,
            liftPosition = liftPosition,
            setPosition = setPosition,
            weightRecommendation = weightRecommendation,
            weight = weight,
            rpe = rpe,
            reps = reps,
            persistedOneRepMax = oneRepMax,
            isDeload = isDeload,
        )
    }

    private fun StandardSetResult.toEntity(): PreviousSetResultEntity {
        return PreviousSetResultEntity(
            id = id,
            setType = setType,
            workoutId = workoutId,
            liftId = liftId,
            mesoCycle = mesoCycle,
            microCycle = microCycle,
            liftPosition = liftPosition,
            setPosition = setPosition,
            weightRecommendation = weightRecommendation,
            weight = weight,
            rpe = rpe,
            reps = reps,
            oneRepMax = oneRepMax,
            isDeload = isDeload,
        )
    }

    private fun MyoRepSetResult.toEntity(): PreviousSetResultEntity {
        return PreviousSetResultEntity(
            id = id,
            setType = SetType.MYOREP,
            workoutId = workoutId,
            liftId = liftId,
            mesoCycle = mesoCycle,
            microCycle = microCycle,
            liftPosition = liftPosition,
            setPosition = setPosition,
            myoRepSetPosition = myoRepSetPosition,
            weightRecommendation = weightRecommendation,
            weight = weight,
            rpe = rpe,
            reps = reps,
            oneRepMax = oneRepMax,
            isDeload = isDeload,
        )
    }

    private fun LinearProgressionSetResult.toEntity(): PreviousSetResultEntity {
        return PreviousSetResultEntity(
            id = id,
            setType = SetType.STANDARD,
            workoutId = workoutId,
            liftId = liftId,
            mesoCycle = mesoCycle,
            microCycle = microCycle,
            liftPosition = liftPosition,
            setPosition = setPosition,
            weightRecommendation = weightRecommendation,
            weight = weight,
            rpe = rpe,
            reps = reps,
            oneRepMax = oneRepMax,
            missedLpGoals = missedLpGoals,
            isDeload = isDeload,
        )
    }
}