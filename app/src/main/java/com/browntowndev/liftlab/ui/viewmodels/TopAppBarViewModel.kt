package com.browntowndev.liftlab.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.states.topAppBar.LabScreen
import com.browntowndev.liftlab.ui.viewmodels.states.topAppBar.LiftLabTopAppBarState
import com.browntowndev.liftlab.ui.viewmodels.states.topAppBar.LiftLibraryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.topAppBar.Screen
import com.browntowndev.liftlab.ui.viewmodels.states.topAppBar.WorkoutHistoryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.topAppBar.WorkoutScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TopAppBarViewModel: ViewModel() {
    private val _state = MutableStateFlow(LiftLabTopAppBarState())
    val state = _state.asStateFlow()

    fun setScreen(route: String?) {
        _state.update {
            it.copy(currentScreen = getScreen(route))
        }
    }

    fun <T> mutateControlValue(request: AppBarMutateControlRequest<T>) {
        _state.update { it.copy(currentScreen = it.currentScreen?.mutateControlValue(request)) }
    }

    fun toggleControlVisibility(controlName: String) {
        _state.update { it.copy(currentScreen = it.currentScreen?.toggleControlVisibility(controlName)) }
    }

    private fun getScreen(route: String?): Screen? = when (route) {
        LiftLibraryScreen.navigation.route -> LiftLibraryScreen()
        WorkoutScreen.navigation.route -> WorkoutScreen()
        LabScreen.navigation.route -> LabScreen()
        WorkoutHistoryScreen.navigation.route -> WorkoutHistoryScreen()
        else -> null
    }
}

