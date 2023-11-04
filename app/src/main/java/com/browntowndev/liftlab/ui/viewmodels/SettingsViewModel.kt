package com.browntowndev.liftlab.ui.viewmodels

import android.content.Context
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.persistence.TransactionScope
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class SettingsViewModel(
    private val roomBackup: RoomBackup,
    private val navHostController: NavHostController,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {

    @Subscribe
    fun handleTopAppBarActionEvent(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.NavigatedBack -> navHostController.popBackStack()
            else -> { }
        }
    }

    fun exportDatabase(context: Context) {
        roomBackup.backup()
    }

    fun importDatabase(context: Context) {
        roomBackup.restore()
    }
}