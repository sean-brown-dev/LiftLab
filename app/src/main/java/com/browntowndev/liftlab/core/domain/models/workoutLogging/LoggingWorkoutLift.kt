package com.browntowndev.liftlab.core.domain.models.workoutLogging

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet
import kotlin.time.Duration

data class LoggingWorkoutLift(
    val id: Long,
    val liftId: Long,
    val liftName: String = "",
    val liftMovementPattern: MovementPattern = MovementPattern.CHEST_ISO,
    val liftVolumeTypes: Int = 0,
    val liftSecondaryVolumeTypes: Int? = null,
    val note: String? = null,
    val position: Int,
    val progressionScheme: ProgressionScheme,
    val deloadWeek: Int?,
    val incrementOverride: Float?,
    val restTime: Duration? = null,
    val restTimerEnabled: Boolean = false,
    val sets: List<GenericLoggingSet>,
    val isCustom: Boolean,
) {
    val setCount get() = sets.size
}
