package com.browntowndev.liftlab.core.domain.models.workoutCalculation

import com.browntowndev.liftlab.core.domain.models.interfaces.CalculationCustomLiftSet

data class CalculationMyoRepSet(
    override val id: Long,
    override val position: Int,
    override val rpeTarget: Float,
    override val repRangeBottom: Int,
    override val repRangeTop: Int,
    val repFloor: Int? = null,
    val maxSets: Int? = null,
    val setGoal: Int,
    val setMatching: Boolean = false,
): CalculationCustomLiftSet
