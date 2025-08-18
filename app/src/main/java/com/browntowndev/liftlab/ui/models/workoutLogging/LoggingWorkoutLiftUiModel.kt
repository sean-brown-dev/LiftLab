package com.browntowndev.liftlab.ui.models.workoutLogging

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import kotlin.time.Duration

data class LoggingWorkoutLiftUiModel(
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
    val restTime: Duration,
    val restTimerEnabled: Boolean = false,
    val sets: List<LoggingSetUiModel>,
) {
    val setCount = sets.size
}
