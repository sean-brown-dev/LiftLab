package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift

interface WorkoutLiftsRepository : ReadOnlyRepository<GenericWorkoutLift, Long> {
    suspend fun getLiftIdsForWorkout(workoutId: Long): List<Long>
    suspend fun getForWorkout(workoutId: Long): List<GenericWorkoutLift>
    suspend fun changeFromLiftsToNewLift(newLiftId: Long, existingLiftIds: List<Long>)
}
