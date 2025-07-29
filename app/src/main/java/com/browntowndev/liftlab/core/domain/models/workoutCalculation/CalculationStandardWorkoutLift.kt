package com.browntowndev.liftlab.core.domain.models.workoutCalculation

import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.CalculationWorkoutLift

data class CalculationStandardWorkoutLift(
    override val id: Long,
    override val liftId: Long,
    override val position: Int,
    override val setCount: Int,
    override val progressionScheme: ProgressionScheme,
    override val deloadWeek: Int?,
    override val incrementOverride: Float?,
    val repRangeTop: Int,
    val repRangeBottom: Int,
    val rpeTarget: Float,
    val stepSize: Int? = null, // Only used for Wave Loading (and block in future)
): CalculationWorkoutLift
