package com.browntowndev.liftlab.core.persistence.dtos.interfaces

import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import kotlin.time.Duration

interface GenericWorkoutLift {
    val id: Long
    val workoutId: Long
    val liftId: Long
    val liftName: String
    val liftMovementPattern: MovementPattern
    val liftVolumeTypes: Int
    val liftSecondaryVolumeTypes: Int?
    val position: Int
    val setCount: Int
    val progressionScheme: ProgressionScheme
    val deloadWeek: Int?
    val incrementOverride: Float?
    val restTime: Duration?
    val restTimerEnabled: Boolean
    val note: String?
}