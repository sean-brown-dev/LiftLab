package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericCustomLiftSet

data class MyoRepSetDto (
    override val id: Long? = null,
    override val position: Int,
    override val repRangeBottom: Int,
    override val repRangeTop: Int,
    val repFloor: Int,
    val maxSets: Int? = null,
    val setMatching: Boolean = false,
    val matchSetGoal: Int? = null,
) : GenericCustomLiftSet