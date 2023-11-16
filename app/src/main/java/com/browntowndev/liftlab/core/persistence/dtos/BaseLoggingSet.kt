package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLoggingSet

abstract class BaseLoggingSet(
    weightRecommendation: Float?,
    final override var hadInitialWeightRecommendation: Boolean? = null,
): GenericLoggingSet {
    init {
        hadInitialWeightRecommendation = hadInitialWeightRecommendation ?:
                (weightRecommendation != null)
    }
}