package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import kotlin.time.Duration

data class StandardWorkoutLiftDto(
    override val id: Long = 0,
    override val workoutId: Long,
    override val liftId: Long,
    override val liftName: String,
    override val liftMovementPattern: MovementPattern,
    override val liftVolumeTypes: Int,
    override val liftSecondaryVolumeTypes: Int?,
    override val position: Int,
    override val setCount: Int,
    override val progressionScheme: ProgressionScheme,
    override val incrementOverride: Float?,
    override val restTime: Duration?,
    override val restTimerEnabled: Boolean,
    override val deloadWeek: Int?,
    override val liftNote: String?,
    val rpeTarget: Float,
    val repRangeBottom: Int,
    val repRangeTop: Int,
    val stepSize: Int? = null, // Only used for Wave Loading (and block in future)
) : GenericWorkoutLift
