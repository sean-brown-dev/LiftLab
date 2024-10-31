package com.browntowndev.liftlab.ui.views.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Home: Route

    @Serializable
    data object Settings: Route

    @Serializable
    data object WorkoutHistory: Route

    @Serializable
    data object Lab: Route

    @Serializable
    data class WorkoutBuilder(val workoutId: Long): Route

    @Serializable
    data class EditWorkout(val workoutLogEntryId: Long): Route

    @Serializable
    data class LiftDetails(val liftId: Long? = null): Route

    @Serializable
    data class Workout(val showLog: Boolean? = null): Route

    @Serializable
    data class LiftLibrary(
        val callerRoute: String? = null,
        val workoutId: Long? = null,
        val workoutLiftId: Long? = null,
        val movementPattern: String? = null,
        val addAtPosition: Int? = null
    ): Route
}