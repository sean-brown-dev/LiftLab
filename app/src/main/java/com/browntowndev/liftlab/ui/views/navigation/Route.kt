package com.browntowndev.liftlab.ui.views.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {

    val id: Long

    @Serializable
    data object Home: Route {
        override val id: Long
            get() = 0L
    }

    @Serializable
    data object Settings: Route {
        override val id: Long
            get() = 1L
    }

    @Serializable
    data object WorkoutHistory: Route {
        override val id: Long
            get() = 2L
    }

    @Serializable
    data object Lab: Route {
        override val id: Long
            get() = 2L
    }

    @Serializable
    data class WorkoutBuilder(val workoutId: Long): Route {
        companion object {
            val id: Long
                get() = 4L
        }

        override val id: Long
            get() = WorkoutBuilder.id
    }

    @Serializable
    data class EditWorkout(val workoutLogEntryId: Long): Route {
        companion object {
            val id: Long
                get() = 5L
        }

        override val id: Long
            get() = EditWorkout.id
    }

    @Serializable
    data class LiftDetails(val liftId: Long? = null): Route {
        companion object {
            val id: Long
                get() = 6L
        }

        override val id: Long
            get() = LiftDetails.id
    }

    @Serializable
    data class Workout(val showLog: Boolean? = null): Route {
        companion object {
            val id: Long
                get() = 7L
        }

        override val id: Long
            get() = Workout.id
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

        override val id: Long
            get() = LiftLibrary.id
    }
}