package com.browntowndev.liftlab.core.domain.models.interfaces

import com.browntowndev.liftlab.core.domain.enums.SetType

interface SetResult {
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

    fun copyBase(
        id: Long = this.id,
        workoutId: Long = this.workoutId,
        liftId: Long = this.liftId,
        liftPosition: Int = this.liftPosition,
        setPosition: Int = this.setPosition,
        weight: Float = this.weight,
        reps: Int = this.reps,
        rpe: Float = this.rpe,
        setType: SetType = this.setType,
        isDeload: Boolean = this.isDeload,
    ): SetResult
}