package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLiftSet

data class DropSetDto (
    override val id: Long = 0,
    override val workoutLiftId: Long,
    override val position: Int,
    override val rpeTarget: Float,
    override val repRangeBottom: Int,
    override val repRangeTop: Int,
    val dropPercentage: Float,
) : GenericLiftSet