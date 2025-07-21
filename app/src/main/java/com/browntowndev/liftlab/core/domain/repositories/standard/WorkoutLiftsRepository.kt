package com.browntowndev.liftlab.core.domain.repositories.standard

import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift

interface WorkoutLiftsRepository : Repository<GenericWorkoutLift, Long> {
    suspend fun updateLiftId(workoutLiftId: Long, newLiftId: Long)
    suspend fun getLiftIdsForWorkout(workoutId: Long): List<Long>
    suspend fun getForWorkout(workoutId: Long): List<GenericWorkoutLift>
}
