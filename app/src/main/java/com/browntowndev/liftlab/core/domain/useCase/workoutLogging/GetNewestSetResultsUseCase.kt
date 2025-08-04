package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository

class GetNewestSetResultsUseCase(
    private val workoutLogRepository: WorkoutLogRepository,
) {
    suspend operator fun invoke(
        workout: Workout,
        liftIdsToSearchFor: List<Long>,
        existingResultsForOtherLifts: List<SetResult>,
        includeDeload: Boolean,
    ): List<SetResult> {
        return if (liftIdsToSearchFor.isNotEmpty()) {
            val linearProgressionLiftIds = workout.lifts
                .filter {
                    it.progressionScheme == ProgressionScheme.LINEAR_PROGRESSION
                }.map { it.liftId }
                .toHashSet()

            existingResultsForOtherLifts.toMutableList().apply {
                val resultsFromOtherWorkouts =
                    workoutLogRepository.getMostRecentSetResultsForLiftIds(
                        liftIds = liftIdsToSearchFor,
                        linearProgressionLiftIds = linearProgressionLiftIds,
                        includeDeload = includeDeload,
                    )

                addAll(resultsFromOtherWorkouts)
            }
        } else existingResultsForOtherLifts
    }
}