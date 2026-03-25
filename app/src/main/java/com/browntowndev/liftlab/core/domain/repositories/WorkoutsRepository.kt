package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.metadata.WorkoutMetadata
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationWorkout
import kotlinx.coroutines.flow.Flow

interface WorkoutsRepository : ReadOnlyRepository<Workout, Long> {
    fun getMetadataFlow(id: Long): Flow<WorkoutMetadata>
    fun getFlow(workoutId: Long): Flow<Workout?>
    fun getByMicrocyclePositionForCalculation(
        programId: Long,
        microcyclePosition: Int,
    ): Flow<CalculationWorkout?>

    suspend fun getAllForProgramWithoutLiftsPopulated(programId: Long): List<Workout>
}
