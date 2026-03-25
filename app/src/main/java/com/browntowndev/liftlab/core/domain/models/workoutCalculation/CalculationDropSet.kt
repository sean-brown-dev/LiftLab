package com.browntowndev.liftlab.core.domain.models.workoutCalculation

import com.browntowndev.liftlab.core.domain.models.interfaces.CalculationCustomLiftSet

data class CalculationDropSet(
    override val id: Long,
    override val position: Int,
    override val rpeTarget: Float,
    override val repRangeBottom: Int,
    override val repRangeTop: Int,
    val dropPercentage: Float,
): CalculationCustomLiftSet
