package com.browntowndev.liftlab.core.domain.models.interfaces

interface CalculationCustomLiftSet {
    val id: Long
    val position: Int
    val rpeTarget: Float
    val repRangeBottom: Int
    val repRangeTop: Int
}