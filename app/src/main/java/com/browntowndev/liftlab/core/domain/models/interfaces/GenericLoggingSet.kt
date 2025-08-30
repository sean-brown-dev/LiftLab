package com.browntowndev.liftlab.core.domain.models.interfaces

interface GenericLoggingSet {
    val position: Int
    val rpeTarget: Float
    val repRangeBottom: Int?
    val repRangeTop: Int?
    val initialWeightRecommendation: Float?
    val weightRecommendation: Float?
    val hadInitialWeightRecommendation: Boolean get() = initialWeightRecommendation != null
    val previousSetResultLabel: String
    val setNumberLabel: String
    val completedWeight: Float?
    val completedReps: Int?
    val completedRpe: Float?
    val complete: Boolean
    val isNew: Boolean

    fun copyCompletionData(
        complete: Boolean,
        completedWeight: Float?,
        completedReps: Int?,
        completedRpe: Float?,
        weightRecommendation: Float? = this.weightRecommendation,
    ): GenericLoggingSet
}

fun GenericLoggingSet.isCompleteWithSameDataAs(result: SetResult): Boolean {
    // Only return true if the set is already complete AND all values match.
    return this.complete &&
            this.completedWeight == result.weight &&
            this.completedReps == result.reps &&
            this.completedRpe == result.rpe
}
