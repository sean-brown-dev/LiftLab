package com.browntowndev.liftlab.ui.models.workoutLogging

import com.browntowndev.liftlab.core.domain.enums.SetType

interface SetResultUiModel {
    val id: Long
    val workoutId: Long
    val liftId: Long
    val liftPosition: Int
    val setPosition: Int
    val weight: Float
    val reps: Int
    val rpe: Float
    val oneRepMax: Int
    val setType: SetType
    val isDeload: Boolean
}
