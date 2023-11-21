package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLoggingSet

data class LoggingStandardSetDto(
    override val position: Int,
    override val rpeTarget: Float,
    override val repRangeBottom: Int,
    override val repRangeTop: Int,
    override val weightRecommendation: Float?,
    override val hadInitialWeightRecommendation: Boolean,
    override val previousSetResultLabel: String,
    override val repRangePlaceholder: String,
    override val setNumberLabel: String = (position + 1).toString(),
    override val complete: Boolean = false,
    override val completedWeight: Float? = null,
    override val completedReps: Int? = null,
    override val completedRpe: Float? = null,
): GenericLoggingSet
