package com.browntowndev.liftlab.core.domain.models

import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet

data class MyoRepSet(
    override val id: Long = 0,
    override val workoutLiftId: Long,
    override val position: Int,
    override val rpeTarget: Float,
    override val repRangeBottom: Int,
    override val repRangeTop: Int,
    val repFloor: Int? = null,
    val maxSets: Int? = null,
    val setMatching: Boolean = false,
    val setGoal: Int,
) : GenericLiftSet