package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import kotlin.time.Duration

data class StandardWorkoutLiftDto (
    override val id: Long = 0,
    override val workoutId: Long,
    override val liftId: Long,
    override val liftName: String,
    override val liftMovementPattern: MovementPattern,
    override val liftVolumeTypes: Int,
    override val liftRestTime: Duration?,
    override val liftIncrementOverride: Float?,
    override val position: Int,
    override val setCount: Int,
    override val progressionScheme: ProgressionScheme,
    override val incrementOverride: Float?,
    override val restTime: Duration?,
    override val deloadWeek: Int?,
    val rpeTarget: Float,
    val repRangeBottom: Int,
    val repRangeTop: Int,
) : GenericWorkoutLift
