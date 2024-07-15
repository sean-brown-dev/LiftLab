package com.browntowndev.liftlab.ui.viewmodels

import android.content.Context
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.BACKUP_DIRECTORY
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_REST_TIME
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.REST_TIME
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.SCHEDULED_BACKUPS_ENABLED
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.SCHEDULED_BACKUP_TIME
import com.browntowndev.liftlab.core.common.Utils.StepSize.Companion.getAllLiftsWithRecalculatedStepSize
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.scheduledBackup.BackupScheduler
import com.browntowndev.liftlab.ui.viewmodels.states.SettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.time.LocalTime
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SettingsViewModel(
    private val programsRepository: ProgramsRepository,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val onNavigateBack: () -> Unit,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    init {
        _state.update {
            it.copy(
                defaultIncrement = SettingsManager
                    .getSetting(INCREMENT_AMOUNT, DEFAULT_INCREMENT_AMOUNT),
                defaultRestTime = SettingsManager
                    .getSetting(REST_TIME, DEFAULT_REST_TIME)
                    .toDuration(DurationUnit.MILLISECONDS)
            )
        }
    }

    @Subscribe
    fun handleTopAppBarActionEvent(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.NavigatedBack -> {
                handleBackButtonPress()
            }
            else -> { }
        }
    }

    private fun handleBackButtonPress() {
        if (_state.value.isDonateScreenVisible) {
            toggleDonationScreen()
        } else {
            onNavigateBack()
        }
    }

    fun toggleDonationScreen() {
        _state.update {
            it.copy(isDonateScreenVisible = !it.isDonateScreenVisible)
        }
    }

    fun toggleImportConfirmationDialog() {
        _state.update {
            it.copy(
                importConfirmationDialogShown = !it.importConfirmationDialogShown
            )
        }
    }

    fun updateDefaultRestTime(restTime: Duration) {
        SettingsManager.setSetting(REST_TIME, restTime.inWholeMilliseconds)
        _state.update {
            it.copy(defaultRestTime = restTime)
        }
    }

    fun updateIncrement(increment: Float) {
        SettingsManager.setSetting(INCREMENT_AMOUNT, increment)
        _state.update {
            it.copy(defaultIncrement = increment)
        }
    }

    fun handleUseAllDataForRecommendationsChange(useOnlyFromPreviousWorkout: Boolean) {
        SettingsManager.setSetting(
            SettingsManager.SettingNames.USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS,
            !useOnlyFromPreviousWorkout
        )
        _state.update {
            it.copy(useAllLiftDataForRecommendations = !useOnlyFromPreviousWorkout)
        }
    }

    fun handleUseOnlyLiftsFromSamePositionChange(useOnlyLiftsFromSamePosition: Boolean) {
        SettingsManager.setSetting(
            SettingsManager.SettingNames.ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION,
            useOnlyLiftsFromSamePosition
        )
        _state.update {
            it.copy(useOnlyResultsFromLiftInSamePosition = useOnlyLiftsFromSamePosition)
        }
    }

    fun handlePromptForDeloadWeekChange(promptOnDeloadStart: Boolean) {
        SettingsManager.setSetting(
            SettingsManager.SettingNames.PROMPT_FOR_DELOAD_WEEK,
            promptOnDeloadStart
        )
        _state.update {
            it.copy(promptOnDeloadStart = promptOnDeloadStart)
        }
    }

    fun handleLiftSpecificDeloadChange(useLiftLevel: Boolean) {
        executeInTransactionScope {
            if (!_state.value.queriedForProgram) {
                _state.update {
                    it.copy(
                        activeProgram = programsRepository.getActiveNotAsLiveData(),
                        queriedForProgram = true,
                    )
                }
            }

            val liftsWithNewStepSizes = _state.value.activeProgram
                ?.let { program ->
                    getAllLiftsWithRecalculatedStepSize(
                        workouts = program.workouts,
                        deloadToUseInsteadOfLiftLevel = if (useLiftLevel) null else program.deloadWeek,
                    )
                } ?: mapOf()

            if (liftsWithNewStepSizes.isNotEmpty()) {
                workoutLiftsRepository.updateMany(liftsWithNewStepSizes.values.toList())
                SettingsManager.setSetting(
                    SettingsManager.SettingNames.LIFT_SPECIFIC_DELOADING,
                    useLiftLevel
                )
                _state.update {
                    it.copy(
                        liftSpecificDeloading = useLiftLevel,
                        activeProgram = it.activeProgram!!.let { program ->
                            program.copy(
                                workouts = program.workouts.fastMap { workout ->
                                    workout.copy(
                                        lifts = workout.lifts.fastMap { lift ->
                                            if(liftsWithNewStepSizes.containsKey(lift.id)) {
                                                liftsWithNewStepSizes[lift.id]!!
                                            } else lift
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            } else {
                SettingsManager.setSetting(
                    SettingsManager.SettingNames.LIFT_SPECIFIC_DELOADING,
                    useLiftLevel
                )
                _state.update {
                    it.copy(liftSpecificDeloading = useLiftLevel)
                }
            }
        }
    }

    fun updateAreScheduledBackupsEnabled(context: Context, enabled: Boolean) {
        if (enabled) {
            BackupScheduler.scheduleNew(context, state.value.scheduledBackupTime)
        } else {
            BackupScheduler.cancel(context)
        }

        SettingsManager.setSetting(SCHEDULED_BACKUPS_ENABLED, enabled)
        _state.update { it.copy(scheduledBackupsEnabled = enabled) }
    }

    fun updateScheduledBackupTime(context: Context, hour: Int, minute: Int) {
        val newTime = LocalTime.of(hour, minute)

        BackupScheduler.scheduleNew(context, newTime)
        SettingsManager.setSetting(SCHEDULED_BACKUP_TIME, newTime.toNanoOfDay())
        _state.update { it.copy(scheduledBackupTime = newTime) }
    }

    fun updateBackupDirectory(newDirectory: String) {
        SettingsManager.setSetting(BACKUP_DIRECTORY, newDirectory)
        _state.update { it.copy(backupDirectory = newDirectory) }
    }
}