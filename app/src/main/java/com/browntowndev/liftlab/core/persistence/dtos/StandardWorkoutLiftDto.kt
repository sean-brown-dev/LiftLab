package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift

data class StandardWorkoutLiftDto (
    override val id: Long = 0,
    override val workoutId: Long,
    override val liftId: Long,
    override val liftName: String,
    override val liftMovementPattern: MovementPattern,
    override val position: Int,
    override val setCount: Int,
    override val progressionScheme: ProgressionScheme,
    override val deloadWeek: Int?,
    val rpeTarget: Double,
    val repRangeBottom: Int,
    val repRangeTop: Int,
) : GenericWorkoutLift
