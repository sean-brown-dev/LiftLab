package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

data class StandardSetResultDto(
    override val workoutId: Long,
    override val liftId: Long,
    override val setPosition: Int,
    override val weight: Float,
    override val reps: Int,
    override val rpe: Float,
    override val microCycle: Int,
    val missedLpGoals: Int? = null,
): SetResult