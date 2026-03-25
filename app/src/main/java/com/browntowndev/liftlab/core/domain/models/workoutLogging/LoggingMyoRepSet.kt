package com.browntowndev.liftlab.core.domain.models.workoutLogging

import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.enums.displayNameShort
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet

data class LoggingMyoRepSet(
    override val position: Int,
    override val rpeTarget: Float,
    override val repRangeBottom: Int?,
    override val repRangeTop: Int?,
    override val initialWeightRecommendation: Float?,
    override val weightRecommendation: Float? = initialWeightRecommendation,
    override val previousSetResultLabel: String,
    override val setNumberLabel: String = SetType.MYOREP.displayNameShort(),
    override val complete: Boolean = false,
    override val completedWeight: Float? = null,
    override val completedReps: Int? = null,
    override val completedRpe: Float? = null,
    override val isNew: Boolean = true,
    val myoRepSetPosition: Int? = null,
    val setMatching: Boolean = false,
    val maxSets: Int? = null,
    val repFloor: Int? = null,
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
