package com.browntowndev.liftlab.ui.viewmodels.states.workout

interface WorkoutScreenInitializationState {
    data object Loading : WorkoutScreenInitializationState
    data object Empty : WorkoutScreenInitializationState
    data class Ready(val workoutState: WorkoutState) : WorkoutScreenInitializationState
}