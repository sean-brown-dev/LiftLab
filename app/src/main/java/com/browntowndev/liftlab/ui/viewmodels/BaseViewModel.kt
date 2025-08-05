package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

abstract class BaseViewModel(
    private val eventBus: EventBus,
): ViewModel() {
    private val _userMessages = MutableSharedFlow<String>()
    val userMessages = _userMessages.asSharedFlow()

    fun emitUserMessage(message: String) {
        viewModelScope.launch {
            _userMessages.emit(message)
        }
    }

    fun registerEventBus() {
        if (!eventBus.isRegistered(this)) {
            eventBus.register(this)
        }
    }

    protected fun executeWithErrorHandling(errorMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (e: Exception) {
                Log.e("LiftLabViewModel", "Error in executeWithErrorHandling", e)
                FirebaseCrashlytics.getInstance().recordException(e)
                emitUserMessage(errorMessage)
            }
        }
    }
}