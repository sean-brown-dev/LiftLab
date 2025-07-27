package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

abstract class LiftLabViewModel(
    private val transactionScope: TransactionScope,
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

    protected fun executeInTransactionScope(action: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch {
            transactionScope.execute {
                action()
            }
        }
    }

    protected fun executeWithErrorHandling(errorMessage: String, action: () -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            Log.e("LiftLabViewModel", "Error in executeWithErrorHandling", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            emitUserMessage(errorMessage)
        }
    }

    protected suspend fun executeSuspendWithErrorHandling(errorMessage: String, action: suspend () -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            Log.e("LiftLabViewModel", "Error in executeWithErrorHandling", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            emitUserMessage(errorMessage)
        }
    }

    protected suspend fun<T> executeSuspendWithErrorHandling(errorMessage: String, default: T, action: suspend () -> T): T {
        try {
            return action()
        } catch (e: Exception) {
            Log.e("LiftLabViewModel", "Error in executeWithErrorHandling", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            emitUserMessage(errorMessage)
            return default
        }
    }
}