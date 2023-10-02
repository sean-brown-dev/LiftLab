package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

data class MyoRepSetResultDto(
    override val id: Long = 0L,
    override val workoutId: Long,
    override val liftId: Long,
    override val setPosition: Int,
    override val weight: Float,
    override val reps: Int,
    override val rpe: Float,
    override val mesoCycle: Int,
    override val microCycle: Int,
    val myoRepSetPosition: Int? = null,
): SetResult