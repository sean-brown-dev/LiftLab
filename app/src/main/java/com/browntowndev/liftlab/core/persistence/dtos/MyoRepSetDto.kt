package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericCustomLiftSet

data class MyoRepSetDto (
    override val id: Long = 0,
    override val workoutLiftId: Long,
    override val position: Int,
    override val rpeTarget: Double,
    override val repRangeBottom: Int,
    override val repRangeTop: Int,
    val repFloor: Int? = null,
    val maxSets: Int? = null,
    val setMatching: Boolean = false,
    val matchSetGoal: Int? = null,
) : GenericCustomLiftSet