package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_REST_TIME
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.REST_TIME
import com.browntowndev.liftlab.core.common.Utils.StepSize.Companion.getAllLiftsWithRecalculatedStepSize
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.ui.viewmodels.states.SettingsState
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SettingsViewModel(
    private val programsRepository: ProgramsRepository,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val roomBackup: RoomBackup,
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

    fun exportDatabase() {
        roomBackup.backup()
    }

    fun toggleImportConfirmationDialog() {
        _state.update {
            it.copy(
                importConfirmationDialogShown = !it.importConfirmationDialogShown
            )
        }
    }

    fun importDatabase() {
        toggleImportConfirmationDialog()
        roomBackup.restore()
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
            }
        }
    }
}