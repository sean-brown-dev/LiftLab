package com.browntowndev.liftlab.core.domain.models.settings

import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import kotlin.time.Duration

data class SettingsConfigurationState(
    val activeProgram: Program? = null,
    val defaultIncrement: Float,
    val defaultRestTime: Duration,
    val useAllLiftDataForRecommendations: Boolean,
    val useOnlyResultsFromLiftInSamePosition: Boolean,
    val promptOnDeloadStart: Boolean,
    val liftSpecificDeloading: Boolean
)
