package com.browntowndev.liftlab.ui.models.workout

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import kotlin.time.Duration

data class CustomWorkoutLiftUiModel(
    override val id: Long = 0,
    override val workoutId: Long,
    override val liftId: Long,
    override val liftName: String,
    override val liftMovementPattern: MovementPattern,
    override val liftVolumeTypes: Int,
    override val liftSecondaryVolumeTypes: Int?,
    override val position: Int,
    override val progressionScheme: ProgressionScheme,
    override val deloadWeek: Int?,
    override val incrementOverride: Float?,
    override val restTime: Duration?,
    override val restTimerEnabled: Boolean,
    override val liftNote: String?,
    val customLiftSets: List<CustomLiftSetUiModel>
) : WorkoutLiftUiModel {
    override val setCount
        get() = customLiftSets.size
}