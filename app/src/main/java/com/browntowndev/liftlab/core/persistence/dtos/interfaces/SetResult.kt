package com.browntowndev.liftlab.core.persistence.dtos.interfaces

import com.browntowndev.liftlab.core.common.enums.SetType

interface SetResult {
    val id: Long
    val workoutId: Long
    val liftId: Long
    val liftPosition: Int
    val setPosition: Int
    val weightRecommendation: Float?
    val weight: Float
    val reps: Int
    val rpe: Float
    val oneRepMax: Int
    val mesoCycle: Int
    val microCycle: Int
    val setType: SetType
    val isDeload: Boolean
}