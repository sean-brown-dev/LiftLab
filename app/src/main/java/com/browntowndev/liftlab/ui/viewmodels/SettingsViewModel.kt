package com.browntowndev.liftlab.ui.viewmodels

import android.content.Context
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_REST_TIME
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.REST_TIME
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.persistence.TransactionScope
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
    private val roomBackup: RoomBackup,
    private val navHostController: NavHostController,
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
                defaultRestTimeString = SettingsManager
                    .getSetting(REST_TIME, DEFAULT_REST_TIME)
                    .toDuration(DurationUnit.MILLISECONDS)
            )
        }
    }

    @Subscribe
    fun handleTopAppBarActionEvent(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.NavigatedBack -> navHostController.popBackStack()
            else -> { }
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
            it.copy(defaultRestTimeString = restTime)
        }
    }

    fun updateIncrement(increment: Float) {
        SettingsManager.setSetting(INCREMENT_AMOUNT, increment)
        _state.update {
            it.copy(defaultIncrement = increment)
        }
    }
}