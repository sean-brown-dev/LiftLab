package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.persistence.dtos.LiftDto
import com.browntowndev.liftlab.core.persistence.repositories.LiftsRepository
import com.browntowndev.liftlab.ui.viewmodels.states.LiftLibraryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class LiftLibraryViewModel(
    private val liftsRepository: LiftsRepository,
    private val eventBus: EventBus,
): ViewModel() {
    private val allLifts: MutableList<LiftDto> = mutableListOf()
    private val _state = MutableStateFlow(LiftLibraryState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getAllLifts()
        }
    }

    fun registerEventBus() {
        if (!eventBus.isRegistered(this)) {
            eventBus.register(this)
            Log.d(Log.DEBUG.toString(), "Registered event bus for ${this::class.simpleName}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        eventBus.unregister(this)
    }

    @Subscribe
    fun handleTopAppBarPayloadEvent(payloadEvent: TopAppBarEvent.PayloadActionEvent<String>) {
        when (payloadEvent.action) {
            TopAppBarAction.FilterTextChanged -> filterLifts(payloadEvent.payload)
            else -> {}
        }
    }

    private fun filterLifts(filter: String) {
        _state.update {
            it.copy(lifts = this.allLifts.filter { lift -> lift.name.contains(filter, true) }.toList())
        }
    }

    private suspend fun getAllLifts() {
        val lifts = liftsRepository.getAll().sortedBy { it.name }
        this.allLifts.addAll(lifts)

        _state.update {
            it.copy(lifts = lifts)
        }
    }
}