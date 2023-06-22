package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericCustomLiftSet

data class DropSetDto (
    override val id: Long = 0,
    override val workoutLiftId: Long,
    override val position: Int,
    override val rpeTarget: Double,
    override val repRangeBottom: Int,
    override val repRangeTop: Int,
    val dropPercentage: Double,
) : GenericCustomLiftSet