package com.browntowndev.liftlab.core.domain.models.workoutLogging

import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.enums.displayNameShort
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet

data class LoggingDropSet(
    override val position: Int,
    override val rpeTarget: Float,
    override val repRangeBottom: Int,
    override val repRangeTop: Int,
    override val weightRecommendation: Float?,
    override val hadInitialWeightRecommendation: Boolean,
    override val previousSetResultLabel: String,
    override val repRangePlaceholder: String,
    override val setNumberLabel: String = SetType.DROP_SET.displayNameShort(),
    override val complete: Boolean = false,
    override val completedWeight: Float? = null,
    override val completedReps: Int? = null,
    override val completedRpe: Float? = null,
    override val isNew: Boolean = false,
    val dropPercentage: Float,
): GenericLoggingSet {
    override fun copyCompletionData(
        complete: Boolean,
        completedWeight: Float?,
        completedReps: Int?,
        completedRpe: Float?,
        weightRecommendation: Float?,
    ): GenericLoggingSet =
        this.copy(
            complete = complete,
            completedWeight = completedWeight,
            completedReps = completedReps,
            completedRpe = completedRpe,
            weightRecommendation = weightRecommendation,
        )
}
