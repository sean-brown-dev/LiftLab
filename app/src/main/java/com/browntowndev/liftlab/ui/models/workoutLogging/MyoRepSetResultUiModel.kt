package com.browntowndev.liftlab.ui.models.workoutLogging

import com.browntowndev.liftlab.core.domain.enums.SetType

data class MyoRepSetResultUiModel(
    override val id: Long = 0L,
    override val workoutId: Long,
    override val liftId: Long,
    override val liftPosition: Int,
    override  val setPosition: Int,
    override val weight: Float,
    override val reps: Int,
    override val rpe: Float,
    override val oneRepMax: Int,
    override val setType: SetType = SetType.MYOREP,
    override val isDeload: Boolean,
    val myoRepSetPosition: Int? = null,
): SetResultUiModel
