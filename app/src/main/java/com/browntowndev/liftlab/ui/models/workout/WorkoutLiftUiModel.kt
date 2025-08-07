package com.browntowndev.liftlab.ui.models.workout

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import kotlin.time.Duration

interface WorkoutLiftUiModel {
    val id: Long
    val workoutId: Long
    val liftId: Long
    val liftName: String
    val liftMovementPattern: MovementPattern
    val liftVolumeTypes: Int
    val liftSecondaryVolumeTypes: Int?
    val liftNote: String?
    val position: Int
    val setCount: Int
    val progressionScheme: DisplayProgressionScheme
    val deloadWeek: Int?
    val incrementOverride: Float?
    val restTime: Duration?
    val restTimerEnabled: Boolean
}