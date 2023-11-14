package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

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
    val missedLpGoals: Int? = null,
): SetResult