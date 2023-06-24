package com.browntowndev.liftlab.core.persistence.dtos.interfaces

interface GenericCustomLiftSet {
    val id: Long
    val workoutLiftId: Long
    val position: Int
    val rpeTarget: Float
    val repRangeBottom: Int
    val repRangeTop: Int
}