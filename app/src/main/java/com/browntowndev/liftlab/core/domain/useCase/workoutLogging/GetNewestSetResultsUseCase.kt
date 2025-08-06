package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.mapping.SetResultMappingExtensions.toSetResult
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
            existingResultsForOtherLifts.toMutableList().apply {
                val resultsFromOtherWorkouts =
                    workoutLogRepository.getMostRecentSetResultsForLiftIds(
                        liftIds = liftIdsToSearchFor,
                        includeDeloads = includeDeload,
                    ).fastMap { setLogEntry ->
                        setLogEntry.toSetResult(
                            workoutId = workout.id,
                            isLinearProgression = setLogEntry.progressionScheme == ProgressionScheme.LINEAR_PROGRESSION,
                        )
                    }

                addAll(resultsFromOtherWorkouts)
            }
        } else existingResultsForOtherLifts
    }
}