package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericCustomLiftSet

data class DropSetDto (
    override val id: Long? = null,
    override val position: Int,
    val dropPercentage: Double,
    val rpeTarget: Double,
    override val repRangeBottom: Int?,
    override val repRangeTop: Int?,
) : GenericCustomLiftSet