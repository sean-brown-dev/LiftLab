package com.browntowndev.liftlab.ui.mapping

import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculatedWorkoutData
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutState

object WorkoutStateMappingExtensions {
    fun CalculatedWorkoutData.toUiModel() =
        WorkoutState(
            completedSets = this.completedSetsForSession,
            personalRecords = this.personalRecords,
            workout = this.calculatedWorkoutPlan,
        )
}