package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.common.enums.displayNameShort

data class LoggingDropSetDto(
    override val position: Int,
    override val rpeTarget: Float,
    override val repRangeBottom: Int,
    override val repRangeTop: Int,
    override val weightRecommendation: Float?,
    override val previousSetResultLabel: String,
    override val repRangePlaceholder: String,
    override val setNumberLabel: String = SetType.DROP_SET.displayNameShort(),
    override val complete: Boolean = false,
    override val completedWeight: Float? = null,
    override val completedReps: Int? = null,
    override val completedRpe: Float? = null,
    val dropPercentage: Float,
): BaseLoggingSet(weightRecommendation)
