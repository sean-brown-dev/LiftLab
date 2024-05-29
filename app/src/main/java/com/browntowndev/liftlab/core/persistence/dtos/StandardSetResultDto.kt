package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.progression.CalculationEngine

data class StandardSetResultDto(
    override val id: Long = 0L,
    override val workoutId: Long,
    override val liftId: Long,
    override val liftPosition: Int,
    override val setPosition: Int,
    override val weightRecommendation: Float?,
    override val weight: Float,
    override val reps: Int,
    override val rpe: Float,
    override val mesoCycle: Int,
    override val microCycle: Int,
    override val setType: SetType,
    override val isDeload: Boolean,
    val missedLpGoals: Int? = null,
    private val persistedOneRepMax: Int? = null,
): SetResult {
    override val oneRepMax: Int by lazy {
        persistedOneRepMax ?: CalculationEngine.getOneRepMax(
            weight = weight,
            reps = reps,
            rpe = rpe,
        )
    }
}