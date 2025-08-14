package com.browntowndev.liftlab.ui.viewmodels.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StartupViewModel(
    private val liftsRepository: LiftsRepository,
): ViewModel() {
    private val _initialized = MutableStateFlow(false)
    val initialized: StateFlow<Boolean> = _initialized.asStateFlow()

    fun beginInitializationCheck(
        onInitializationComplete: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            with(Dispatchers.IO) {
                try {
                    while (liftsRepository.getAll().isEmpty()) {
                        delay(50)
                    }
                } catch(_: Exception)  {
                    delay(50)
                }

                onInitializationComplete()
                _initialized.value = true
            }
        }
    }
}