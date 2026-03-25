package com.browntowndev.liftlab.core.data.local.repositories

import com.browntowndev.liftlab.core.data.local.dao.WorkoutsDao
import com.browntowndev.liftlab.core.data.mapping.toCalculationDomainModel
import com.browntowndev.liftlab.core.data.mapping.toDomainModel
import com.browntowndev.liftlab.core.domain.models.metadata.WorkoutMetadata
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationWorkout
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutsRepositoryImpl(
    private val workoutsDao: WorkoutsDao,
): WorkoutsRepository {
    override fun getMetadataFlow(id: Long): Flow<WorkoutMetadata> =
        workoutsDao.getMetadataFlow(id).map { it.toDomainModel() }

    override suspend fun getAll(): List<Workout> {
        return workoutsDao.getAll().map { it.toDomainModel() }
    }

    override fun getAllFlow(): Flow<List<Workout>> {
        return workoutsDao.getAllFlow().map { workouts ->
            workouts.map { it.toDomainModel() }
        }
    }

    override suspend fun getById(id: Long): Workout? {
        return workoutsDao.get(id)?.toDomainModel()
    }

    override suspend fun getMany(ids: List<Long>): List<Workout> {
        return workoutsDao.getMany(ids).map { it.toDomainModel() }
    }

    override fun getFlow(workoutId: Long): Flow<Workout?> {
        return workoutsDao.getByIdFlow(workoutId).map { workout ->
            workout?.toDomainModel()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getByMicrocyclePositionForCalculation(
        programId: Long,
        microcyclePosition: Int,
    ): Flow<CalculationWorkout?> {
        return workoutsDao.getByMicrocyclePosition(
            programId = programId,
            microcyclePosition = microcyclePosition,
        ).map { workoutEntity ->
            workoutEntity?.toCalculationDomainModel()
        }
    }

    override suspend fun getAllForProgramWithoutLiftsPopulated(programId: Long): List<Workout> {
        return workoutsDao.getAllForProgramWithoutRelationships(programId).map { it.toDomainModel() }
    }
}
