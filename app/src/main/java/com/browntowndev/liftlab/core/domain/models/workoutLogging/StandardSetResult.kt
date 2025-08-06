package com.browntowndev.liftlab.core.domain.models.workoutLogging

import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.useCase.utils.WeightCalculationUtils

data class StandardSetResult(
    override val id: Long = 0L,
    override val workoutId: Long,
    override val liftId: Long,
    override val liftPosition: Int,
    override val setPosition: Int,
    override val weight: Float,
    override val reps: Int,
    override val rpe: Float,
    override val setType: SetType,
    override val isDeload: Boolean,
    val missedLpGoals: Int? = null,
    private val persistedOneRepMax: Int? = null,
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
        weight: Float,
        reps: Int,
        rpe: Float,
        setType: SetType,
        isDeload: Boolean
    ): SetResult {
        return copy(
            id = id,
            workoutId = workoutId,
            liftId = liftId,
            liftPosition = liftPosition,
            setPosition = setPosition,
            weight = weight,
            reps = reps,
            rpe = rpe,
            setType = setType,
            isDeload = isDeload,
            persistedOneRepMax = null,
        )
    }
}