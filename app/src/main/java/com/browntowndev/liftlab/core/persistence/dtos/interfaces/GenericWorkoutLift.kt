package com.browntowndev.liftlab.core.persistence.dtos.interfaces

import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme

interface GenericWorkoutLift {
    val id: Long
    val workoutId: Long
    val liftId: Long
    val liftName: String
    val liftMovementPattern: MovementPattern
    val position: Int
    val setCount: Int
    val progressionScheme: ProgressionScheme
    val deloadWeek: Int?
}