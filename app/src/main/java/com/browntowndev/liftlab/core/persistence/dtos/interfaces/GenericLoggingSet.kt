package com.browntowndev.liftlab.core.persistence.dtos.interfaces

interface GenericLoggingSet {
    val setPosition: Int
    val rpeTarget: Float
    val repRangeBottom: Int
    val repRangeTop: Int
    val weightRecommendation: Float?
    val previousSetResultLabel: String
    val repRangePlaceholder: String
    val setNumberLabel: String
    val completedWeight: Float?
    val completedReps: Int?
    val completedRpe: Float?
    val complete: Boolean
}