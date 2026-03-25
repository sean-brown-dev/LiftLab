package com.browntowndev.liftlab.core.domain.models.workout

import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet

data class DropSet (
    override val id: Long = 0,
    override val workoutLiftId: Long,
    override val position: Int,
    override val rpeTarget: Float,
    override val repRangeBottom: Int,
    override val repRangeTop: Int,
    val dropPercentage: Float,
) : GenericLiftSet