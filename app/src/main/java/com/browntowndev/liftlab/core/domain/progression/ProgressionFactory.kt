package com.browntowndev.liftlab.core.domain.progression

import com.browntowndev.liftlab.core.domain.models.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.Workout
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult

interface ProgressionFactory {
    fun calculate(
        workout: Workout,
        previousSetResults: List<SetResult>,
        previousResultsForDisplay: List<SetResult>,
        inProgressSetResults: Map<String, SetResult>,
        microCycle: Int,
        programDeloadWeek: Int,
        useLiftSpecificDeloading: Boolean,
        onlyUseResultsForLiftsInSamePosition: Boolean,
    ): LoggingWorkout
}