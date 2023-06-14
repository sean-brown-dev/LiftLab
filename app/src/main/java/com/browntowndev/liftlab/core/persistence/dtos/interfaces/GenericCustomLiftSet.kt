package com.browntowndev.liftlab.core.persistence.dtos.interfaces

interface GenericCustomLiftSet {
    val id: Long?
    val position: Int
    val repRangeBottom: Int?
    val repRangeTop: Int?
}