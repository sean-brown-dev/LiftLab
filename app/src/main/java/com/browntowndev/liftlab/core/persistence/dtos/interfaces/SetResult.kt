package com.browntowndev.liftlab.core.persistence.dtos.interfaces

interface SetResult {
    val workoutId: Long
    val liftId: Long
    val setPosition: Int
    val weight: Float
    val reps: Int
    val rpe: Float
    val microCycle: Int
}