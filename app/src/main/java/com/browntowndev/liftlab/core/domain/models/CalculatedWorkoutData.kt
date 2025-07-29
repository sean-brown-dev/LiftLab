package com.browntowndev.liftlab.core.domain.models

import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout

/**
 * Represents the core, calculated data for the active workout session,
 * derived from the program plan, historical results, and in-progress sets.
 * This is a domain model returned by GetWorkoutStateFlowUseCase.
 */
data class CalculatedWorkoutData(
    /**
     * The workout plan for the current session, potentially hydrated with
     * recommended weights/reps based on previous performance and settings.
     */
    val calculatedWorkoutPlan: LoggingWorkout? = null,

    /**
     * A list of SetResults that have been completed during the *current* workout session.
     */
    val completedSetsForSession: List<SetResult> = emptyList(),

    /**
     * A map of personal records relevant to the lifts in the current workout plan.
     */
    val personalRecords: Map<Long, PersonalRecord> = emptyMap(),
)