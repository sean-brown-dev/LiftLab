package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.progression.CalculationEngine

data class LinearProgressionSetResultDto(
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
        persistedOneRepMax ?: CalculationEngine.getOneRepMax(
            weight = weight,
            reps = reps,
            rpe = rpe,
        )
    }
}