package com.browntowndev.liftlab.ui.viewmodels.settings

import androidx.compose.runtime.Stable
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_PROMPT_FOR_DELOAD_WEEK
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import kotlin.time.Duration

@Stable
data class SettingsState(
    val defaultRestTime: Duration? = null,
    val defaultIncrement: Float? = null,
    val isDonateScreenVisible: Boolean = false,
    val activeProgram: Program? = null,
    val liftSpecificDeloading: Boolean = DEFAULT_LIFT_SPECIFIC_DELOADING,
    val promptOnDeloadStart: Boolean = DEFAULT_PROMPT_FOR_DELOAD_WEEK,
    val useAllLiftDataForRecommendations: Boolean = DEFAULT_USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS,
    val useOnlyResultsFromLiftInSamePosition: Boolean = DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION,
)
