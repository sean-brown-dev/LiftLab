package com.browntowndev.liftlab.core.domain.models.interfaces

interface GenericLiftSet {
    val id: Long
    val workoutLiftId: Long
    val position: Int
    val rpeTarget: Float
    val repRangeBottom: Int
    val repRangeTop: Int
}
