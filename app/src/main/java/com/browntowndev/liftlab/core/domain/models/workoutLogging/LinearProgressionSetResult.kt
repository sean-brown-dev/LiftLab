package com.browntowndev.liftlab.core.domain.models.workoutLogging

import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.useCase.utils.WeightCalculationUtils

data class LinearProgressionSetResult(
    override val id: Long = 0L,
    override val workoutId: Long,
    override val liftId: Long,
    override val liftPosition: Int,
    override val setPosition: Int,
    override val weightRecommendation: Float?,
    override val weight: Float,
    override val reps: Int,
    override val rpe: Float,
    private val persistedOneRepMax: Int? = null,
    override val mesoCycle: Int,
    override val microCycle: Int,
    override val setType: SetType = SetType.STANDARD,
    override val isDeload: Boolean,
    val missedLpGoals: Int,
): SetResult {
    override val oneRepMax: Int by lazy {
        persistedOneRepMax ?: WeightCalculationUtils.getOneRepMax(
            weight = weight,
            reps = reps,
            rpe = rpe,
        )
    }

    override fun copyBase(
        id: Long,
        workoutId: Long,
        liftId: Long,
        liftPosition: Int,
        setPosition: Int,
        weightRecommendation: Float?,
        weight: Float,
        reps: Int,
        rpe: Float,
        mesoCycle: Int,
        microCycle: Int,
        setType: SetType,
        isDeload: Boolean
    ): SetResult {
        return copy(
            id = id,
            workoutId = workoutId,
            liftId = liftId,
            liftPosition = liftPosition,
            setPosition = setPosition,
            weightRecommendation = weightRecommendation,
            weight = weight,
            reps = reps,
            rpe = rpe,
            mesoCycle = mesoCycle,
            microCycle = microCycle,
            setType = setType,
            isDeload = isDeload,
            persistedOneRepMax = null,
        )
    }
}