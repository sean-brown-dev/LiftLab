package com.browntowndev.liftlab.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.states.LiftLabTopAppBarState
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LabScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftDetailsScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftLibraryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutBuilderScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutHistoryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutScreen
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

    fun setCollapsed(collapsed: Boolean) {
        if (collapsed != _state.value.isCollapsed) {
            _state.update {
                it.copy(isCollapsed = collapsed)
            }
        }
    }

    fun <T> mutateControlValue(request: AppBarMutateControlRequest<T>) {
        _state.update { it.copy(currentScreen = it.currentScreen?.mutateControlValue(request)) }
    }

    fun setControlVisibility(controlName: String, isVisible: Boolean) {
        _state.update { it.copy(currentScreen = it.currentScreen?.setControlVisibility(controlName, isVisible = isVisible)) }
    }

    private fun getScreen(route: String?): Screen? = when (route) {
        LiftLibraryScreen.navigation.route-> LiftLibraryScreen()
        LiftDetailsScreen.navigation.route -> LiftDetailsScreen()
        WorkoutScreen.navigation.route -> WorkoutScreen()
        LabScreen.navigation.route -> LabScreen()
        WorkoutHistoryScreen.navigation.route -> WorkoutHistoryScreen()
        WorkoutBuilderScreen.navigation.route -> WorkoutBuilderScreen()
        else -> null
    }
}

