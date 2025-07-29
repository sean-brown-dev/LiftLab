package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.Workout
import com.browntowndev.liftlab.core.domain.models.metadata.WorkoutMetadata
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationWorkout
import kotlinx.coroutines.flow.Flow

interface WorkoutsRepository : Repository<Workout, Long> {
    suspend fun getMetadataFlow(id: Long): Flow<WorkoutMetadata>
    suspend fun updateName(id: Long, newName: String)
    fun getFlow(workoutId: Long): Flow<Workout?>
    fun getByMicrocyclePositionForCalculation(
        programId: Long,
        microcyclePosition: Int,
    ): Flow<CalculationWorkout?>
}
