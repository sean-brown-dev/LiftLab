package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLoggingSet
import kotlin.time.Duration

data class LoggingWorkoutLiftDto(
    val id: Long,
    val liftId: Long,
    val liftName: String,
    val liftMovementPattern: MovementPattern,
    val liftVolumeTypes: Int,
    val liftSecondaryVolumeTypes: Int?,
    val note: String? = "",
    val position: Int,
    val setCount: Int,
    val progressionScheme: ProgressionScheme,
    val deloadWeek: Int?,
    val incrementOverride: Float?,
    val restTime: Duration?,
    val restTimerEnabled: Boolean,
    val sets: List<GenericLoggingSet>,
)
