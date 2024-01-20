package com.browntowndev.liftlab.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.persistence.TransactionScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

abstract class LiftLabViewModel(
    private val transactionScope: TransactionScope,
    private val eventBus: EventBus,
): ViewModel() {
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