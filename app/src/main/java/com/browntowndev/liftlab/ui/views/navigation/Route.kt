package com.browntowndev.liftlab.ui.views.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Home: Route {
        val id: Long
            get() = 0L
    }

    @Serializable
    data object Settings: Route {
        val id: Long
            get() = 1L
    }

    @Serializable
    data object WorkoutHistory: Route {
        val id: Long
            get() = 2L
    }

    @Serializable
    data object Lab: Route {
        val id: Long
            get() = 2L
    }

    @Serializable
    data class WorkoutBuilder(val workoutId: Long): Route {
        companion object {
            val id: Long
                get() = 4L
        }
    }

    @Serializable
    data class EditWorkout(val workoutLogEntryId: Long): Route {
        companion object {
            val id: Long
                get() = 5L
        }
    }

    @Serializable
    data class LiftDetails(val liftId: Long? = null): Route {
        companion object {
            val id: Long
                get() = 6L
        }
    }

    @Serializable
    data class Workout(val showLog: Boolean? = null): Route {
        companion object {
            val id: Long
                get() = 7L
        }
    }

    @Serializable
    data class LiftLibrary(
        val callerRouteId: Long? = null,
        val workoutId: Long? = null,
        val workoutLiftId: Long? = null,
        val movementPattern: String? = null,
        val addAtPosition: Int? = null
    ): Route {
        companion object {
            val id: Long
                get() = 8L
        }
    }
}