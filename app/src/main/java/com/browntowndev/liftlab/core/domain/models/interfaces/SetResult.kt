package com.browntowndev.liftlab.core.domain.models.interfaces

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

    fun copyBase(
        id: Long = this.id,
        workoutId: Long = this.workoutId,
        liftId: Long = this.liftId,
        liftPosition: Int = this.liftPosition,
        setPosition: Int = this.setPosition,
        weightRecommendation: Float? = this.weightRecommendation,
        weight: Float = this.weight,
        reps: Int = this.reps,
        rpe: Float = this.rpe,
        mesoCycle: Int = this.mesoCycle,
        microCycle: Int = this.microCycle,
        setType: SetType = this.setType,
        isDeload: Boolean = this.isDeload,
    ): SetResult
}