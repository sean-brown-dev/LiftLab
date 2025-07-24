package com.browntowndev.liftlab.core.domain.models

import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet

data class StandardSet (
    override val id: Long = 0,
    override val workoutLiftId: Long,
    override val position: Int,
    override val rpeTarget: Float,
    override val repRangeBottom: Int,
    override val repRangeTop: Int,
) : GenericLiftSet