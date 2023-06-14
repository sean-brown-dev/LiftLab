package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericCustomLiftSet
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift

data class CustomWorkoutLiftDto (
    override val id: Long = 0,
    override val liftId: Long,
    override val liftName: String,
    override val liftMovementPattern: MovementPattern,
    override val position: Int,
    override val setCount: Int,
    override val useReversePyramidSets: Boolean = false,
    override val progressionScheme: ProgressionScheme,
    val customLiftSets: List<GenericCustomLiftSet>
) : GenericWorkoutLift