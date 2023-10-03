package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.persistence.TransactionScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

abstract class LiftLabViewModel(
    private val transactionScope: TransactionScope,
    private val eventBus: EventBus,
): ViewModel() {

    fun registerEventBus() {
        if (!eventBus.isRegistered(this)) {
            eventBus.register(this)
            Log.d(Log.DEBUG.toString(), "Registered event bus for ${this::class.simpleName}")
        }
    }

    protected fun executeInTransactionScope(action: suspend () -> Unit) {
        viewModelScope.launch {
            transactionScope.execute {
                action()
            }
        }
    }
}