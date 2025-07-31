package com.browntowndev.liftlab.core.domain.models.workoutLogging

import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult

class BuildSetResultUseCase {
    operator fun invoke(
        id: Long = 0L,
        workoutId: Long,
        currentMesocycle: Int,
        currentMicrocycle: Int,
        weightRecommendation: Float?,
        liftId: Long,
        setType: SetType,
        progressionScheme: ProgressionScheme? = null,
        liftPosition: Int,
        setPosition: Int,
        myoRepSetPosition: Int?,
        weight: Float,
        reps: Int,
        rpe: Float,
        isDeload: Boolean,
    ): SetResult = when (setType) {
        SetType.STANDARD,
        SetType.DROP_SET -> {
            if (progressionScheme != ProgressionScheme.LINEAR_PROGRESSION) {
                StandardSetResult(
                    id = id,
                    workoutId = workoutId,
                    setType = setType,
                    liftId = liftId,
                    mesoCycle = currentMesocycle,
                    microCycle = currentMicrocycle,
                    liftPosition = liftPosition,
                    setPosition = setPosition,
                    weightRecommendation = weightRecommendation,
                    weight = weight,
                    reps = reps,
                    rpe = rpe,
                    isDeload = isDeload,
                )
            } else {
                // LP can only be standard liftEntity, so no myo
                LinearProgressionSetResult(
                    id = id,
                    workoutId = workoutId,
                    liftId = liftId,
                    mesoCycle = currentMesocycle,
                    microCycle = currentMicrocycle,
                    liftPosition = liftPosition,
                    setPosition = setPosition,
                    weightRecommendation = weightRecommendation,
                    weight = weight,
                    reps = reps,
                    rpe = rpe,
                    missedLpGoals = 0, // assigned on completion
                    isDeload = isDeload,
                )
            }
        }

        SetType.MYOREP ->
            MyoRepSetResult(
                id = id,
                workoutId = workoutId,
                liftId = liftId,
                mesoCycle = currentMesocycle,
                microCycle = currentMicrocycle,
                liftPosition = liftPosition,
                setPosition = setPosition,
                weightRecommendation = weightRecommendation,
                weight = weight,
                reps = reps,
                rpe = rpe,
                myoRepSetPosition = myoRepSetPosition,
                isDeload = isDeload,
            )
    }
}
