package com.browntowndev.liftlab.core.domain.models.interfaces

import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme

interface CalculationWorkoutLift {
    val id: Long
    val liftId: Long
    val position: Int
    val setCount: Int
    val progressionScheme: ProgressionScheme
    val deloadWeek: Int?
    val incrementOverride: Float?
}