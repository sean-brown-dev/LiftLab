package com.browntowndev.liftlab.ui.models.workoutLogging

import com.browntowndev.liftlab.core.domain.enums.SetType

data class StandardSetResultUiModel(
    override val id: Long = 0L,
    override val workoutId: Long,
    override val liftId: Long,
    override val liftPosition: Int,
    override val setPosition: Int,
    override val weight: Float,
    override val reps: Int,
    override  val rpe: Float,
    override val setType: SetType,
    override val isDeload: Boolean,
    override val oneRepMax: Int,
    val missedLpGoals: Int? = null,
): SetResultUiModel
