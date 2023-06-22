package com.browntowndev.liftlab.core.persistence.dtos.interfaces

interface GenericCustomLiftSet {
    val id: Long
    val workoutLiftId: Long
    val position: Int
    val rpeTarget: Double
    val repRangeBottom: Int
    val repRangeTop: Int
}