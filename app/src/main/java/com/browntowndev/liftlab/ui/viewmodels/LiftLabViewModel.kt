package com.browntowndev.liftlab.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.data.common.TransactionScope
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
    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents = _toastEvents.asSharedFlow()

    fun showToast(message: String) {
        viewModelScope.launch {
            _toastEvents.emit(message)
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
}