package com.browntowndev.liftlab.ui.models.workoutLogging

interface LoggingSetUiModel {
    val position: Int
    val rpeTarget: Float
    val rpeTargetPlaceholder: String
    val repRangeBottom: Int?
    val repRangeTop: Int?
    val weightRecommendation: Float?
    val hadInitialWeightRecommendation: Boolean
    val previousSetResultLabel: String
    val repRangePlaceholder: String
    val setNumberLabel: String
    val completedWeight: Float?
    val completedReps: Int?
    val completedRpe: Float?
    val complete: Boolean
}
