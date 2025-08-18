package com.browntowndev.liftlab.core.domain.models.workoutLogging

import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet

data class LoggingStandardSet(
    override val position: Int,
    override val rpeTarget: Float,
    override val repRangeBottom: Int,
    override val repRangeTop: Int,
    override val weightRecommendation: Float?,
    override val hadInitialWeightRecommendation: Boolean,
    override val previousSetResultLabel: String,
    override val repRangePlaceholder: String, // TODO: Move this into UI model and calculate it. Add isDeloadWeek to this model so calc can be done
    override val setNumberLabel: String = (position + 1).toString(),
    override val complete: Boolean = false,
    override val completedWeight: Float? = null,
    override val completedReps: Int? = null,
    override val completedRpe: Float? = null,
): GenericLoggingSet {
    override fun copyCompletionData(
        complete: Boolean,
        completedWeight: Float?,
        completedReps: Int?,
        completedRpe: Float?,
    ): GenericLoggingSet =
        this.copy(
            complete = complete,
            completedWeight = completedWeight,
            completedReps = completedReps,
            completedRpe = completedRpe,
        )
}
