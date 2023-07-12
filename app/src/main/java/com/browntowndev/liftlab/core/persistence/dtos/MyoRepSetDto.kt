package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLiftSet

data class MyoRepSetDto(
    override val id: Long = 0,
    override val workoutLiftId: Long,
    override val setPosition: Int,
    override val rpeTarget: Float,
    override val repRangeBottom: Int,
    override val repRangeTop: Int,
    val repFloor: Int? = null,
    val maxSets: Int? = null,
    val setMatching: Boolean = false,
    val setGoal: Int,
) : GenericLiftSet