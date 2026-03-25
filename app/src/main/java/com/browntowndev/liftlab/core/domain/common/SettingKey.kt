package com.browntowndev.liftlab.core.domain.common

import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_PROMPT_FOR_DELOAD_WEEK
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_REST_TIME
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.PROMPT_FOR_DELOAD_WEEK
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.REST_TIME
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS


sealed class SettingKey<T>(val name: String, val defaultValue: T) {
    object RestTime: SettingKey<Long>(REST_TIME, DEFAULT_REST_TIME)
    object Increment: SettingKey<Float>(INCREMENT_AMOUNT, DEFAULT_INCREMENT_AMOUNT)
    object LiftSpecificDeload: SettingKey<Boolean>(LIFT_SPECIFIC_DELOADING, DEFAULT_LIFT_SPECIFIC_DELOADING)
    object PromptForDeloadWeek: SettingKey<Boolean>(PROMPT_FOR_DELOAD_WEEK, DEFAULT_PROMPT_FOR_DELOAD_WEEK)
    object UseAllLiftDataForRecommendations: SettingKey<Boolean>(USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS, DEFAULT_USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS)
    object UseOnlyResultsFromLiftInSamePosition: SettingKey<Boolean>(ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION, DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION)
}