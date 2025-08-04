package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import com.browntowndev.liftlab.core.domain.models.workout.FilterableLiftsState
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetFilterableLiftsStateFlowUseCase(
    private val liftsRepository: LiftsRepository,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend operator fun invoke(workoutId: Long?): Flow<FilterableLiftsState> {
        val liftIdsForWorkout = getLiftIdFilters(workoutId)
        return liftsRepository.getAllFlow()
            .map { lifts ->
                FilterableLiftsState(
                    lifts = lifts,
                    liftIdsForWorkout = liftIdsForWorkout,
                )
            }
    }

    private suspend fun getLiftIdFilters(
        workoutId: Long?,
    ): HashSet<Long> {
        return if (workoutId != null) {
            workoutLiftsRepository.getLiftIdsForWorkout(workoutId).toHashSet()
        } else hashSetOf()
    }
}