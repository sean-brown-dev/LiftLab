package com.browntowndev.liftlab.ui.viewmodels

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.states.LiftLabTopAppBarState
import com.browntowndev.liftlab.ui.viewmodels.states.screens.EditWorkoutScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.HomeScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LabScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftDetailsScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftLibraryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.SettingsScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutBuilderScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutHistoryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutScreen
import com.browntowndev.liftlab.ui.views.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TopAppBarViewModel: ViewModel() {
    private lateinit var _mediaPlayer: MediaPlayer
    private val _state = MutableStateFlow(LiftLabTopAppBarState())
    val state = _state.asStateFlow()

    fun setScreen(screen: Screen) {
        _state.update {
            it.copy(currentScreen = screen)
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

    fun playRestTimerCompletionSound(context: Context) {
        _mediaPlayer = MediaPlayer.create(context, R.raw.boxing_bell)
        _mediaPlayer.setOnCompletionListener {
            it.release()
        }

        _mediaPlayer.start()
    }
}

