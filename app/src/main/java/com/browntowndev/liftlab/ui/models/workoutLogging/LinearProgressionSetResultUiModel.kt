package com.browntowndev.liftlab.ui.models.workoutLogging

import com.browntowndev.liftlab.core.domain.enums.SetType

data class LinearProgressionSetResultUiModel(
    override val id: Long = 0L,
    override val workoutId: Long,
    override val liftId: Long,
    override val liftPosition: Int,
    override val setPosition: Int,
    override val weight: Float,
    override val reps: Int,
    override val rpe: Float,
    override val oneRepMax: Int,
    override  val setType: SetType = SetType.STANDARD,
    override  val isDeload: Boolean,
    val missedLpGoals: Int,
): SetResultUiModel
