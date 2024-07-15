package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.BACKUP_DIRECTORY
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_BACKUP_DIRECTORY
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_PROMPT_FOR_DELOAD_WEEK
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_SCHEDULED_BACKUPS_ENABLED
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_SCHEDULED_BACKUP_TIME
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.PROMPT_FOR_DELOAD_WEEK
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.SCHEDULED_BACKUPS_ENABLED
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.SCHEDULED_BACKUP_TIME
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import java.time.LocalTime
import kotlin.time.Duration

data class SettingsState(
    val importConfirmationDialogShown: Boolean = false,
    val defaultRestTime: Duration? = null,
    val defaultIncrement: Float? = null,
    val isDonateScreenVisible: Boolean = false,
    val queriedForProgram: Boolean = false,
    val activeProgram: ProgramDto? = null,
    val liftSpecificDeloading: Boolean = SettingsManager.getSetting(LIFT_SPECIFIC_DELOADING, DEFAULT_LIFT_SPECIFIC_DELOADING),
    val promptOnDeloadStart: Boolean = SettingsManager.getSetting(PROMPT_FOR_DELOAD_WEEK, DEFAULT_PROMPT_FOR_DELOAD_WEEK),
    val useAllLiftDataForRecommendations: Boolean =
        SettingsManager.getSetting(USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS, DEFAULT_USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS),
    val useOnlyResultsFromLiftInSamePosition: Boolean =
        SettingsManager.getSetting(ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION, DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION),
    val scheduledBackupsEnabled: Boolean =
        SettingsManager.getSetting(SCHEDULED_BACKUPS_ENABLED, DEFAULT_SCHEDULED_BACKUPS_ENABLED),
    val scheduledBackupTime: LocalTime =
        LocalTime.ofNanoOfDay(SettingsManager.getSetting(SCHEDULED_BACKUP_TIME, DEFAULT_SCHEDULED_BACKUP_TIME)),
    val backupDirectory: String =
        SettingsManager.getSetting(BACKUP_DIRECTORY, DEFAULT_BACKUP_DIRECTORY)
)
