package com.browntowndev.liftlab.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.ui.viewmodels.states.SettingsState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.IOException

class SettingsViewModel(
    private val navHostController: NavHostController,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    fun exportDatabase(context: Context, uri: Uri) {
        _state.update {
            it.copy(isPerformingIo = true)
        }

        viewModelScope.launch {
            try {
                LiftLabDatabase.exportDatabase(context, uri)
            } catch(ioEx: IOException) {
                Log.e(Log.ERROR.toString(), ioEx.toString())
            } finally {
                _state.update {
                    it.copy(isPerformingIo = false)
                }
            }
        }
    }
}