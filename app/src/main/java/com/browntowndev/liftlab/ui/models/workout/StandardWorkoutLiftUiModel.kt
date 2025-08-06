package com.browntowndev.liftlab.ui.models.workout

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import kotlin.time.Duration

data class StandardWorkoutLiftUiModel(
    override val id: Long,
    override val workoutId: Long,
    override val liftId: Long,
    override val liftName: String,
    override val liftMovementPattern: MovementPattern,
    override val liftVolumeTypes: Int,
    override val liftSecondaryVolumeTypes: Int?,
    override val liftNote: String?,
    override val position: Int,
    override val setCount: Int,
    override val progressionScheme: ProgressionScheme,
    override val deloadWeek: Int?,
    override val incrementOverride: Float?,
    override val restTime: Duration?,
    override val restTimerEnabled: Boolean
): WorkoutLiftUiModel
