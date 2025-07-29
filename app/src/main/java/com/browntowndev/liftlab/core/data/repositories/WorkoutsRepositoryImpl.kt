package com.browntowndev.liftlab.core.data.repositories

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapIndexed
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.data.local.dao.CustomSetsDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.domain.models.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.Workout
import com.browntowndev.liftlab.core.data.mapping.WorkoutMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.data.mapping.WorkoutMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import com.browntowndev.liftlab.core.data.local.dao.WorkoutsDao
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.mapping.CustomLiftSetMappingExtensions.toEntity
import com.browntowndev.liftlab.core.data.mapping.WorkoutLiftMappingExtensions.toEntity
import com.browntowndev.liftlab.core.data.mapping.WorkoutMappingExtensions.toCalculationDomainModel
import com.browntowndev.liftlab.core.data.remote.SyncScheduler
import com.browntowndev.liftlab.core.domain.models.metadata.WorkoutMetadata
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationWorkout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutsRepositoryImpl(
    private val workoutLiftsDao: WorkoutLiftsDao,
    private val customSetsDao: CustomSetsDao,
    private val programsRepository: ProgramsRepository,
    private val workoutsDao: WorkoutsDao,
    private val syncScheduler: SyncScheduler,
): WorkoutsRepository {
    override suspend fun getMetadataFlow(id: Long): Flow<WorkoutMetadata> =
        workoutsDao.getMetadataFlow(id).map { it.toDomainModel() }

    override suspend fun updateName(id: Long, newName: String) {
        val current = workoutsDao.getWithoutRelationships(id) ?: return
        val toUpdate = current.copy(name = newName).applyRemoteStorageMetadata(
            remoteId = current.remoteId,
            remoteLastUpdated = current.remoteLastUpdated,
            synced = false,
        )
        workoutsDao.update(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun insert(model: Workout): Long {
        val toInsert = model.toEntity()
        val id = workoutsDao.insert(toInsert)
        syncScheduler.scheduleSync()

        return id
    }

    override suspend fun insertMany(models: List<Workout>): List<Long> {
        val toInsert = models.map { it.toEntity() }
        val ids = workoutsDao.insertMany(toInsert)
        syncScheduler.scheduleSync()

        return ids
    }

    override suspend fun delete(model: Workout): Int {
        val deleteCount = workoutsDao.softDelete(model.id)
        if (deleteCount > 0) {
            // Update workoutEntity positions
            val workoutsWithNewPositions = workoutsDao.getAllForProgramWithoutRelationships(model.programId)
                .sortedBy { it.position }
                .fastMapIndexed { index, workoutEntity ->
                    workoutEntity.copy(position = index)
                }
            workoutsDao.updateMany(workoutsWithNewPositions)


            // If current microcycle position is now greater than the number of workouts
            // set it to the last workoutEntity index
            programsRepository.getActive()?.let { program ->
                if (program.currentMicrocyclePosition > workoutsWithNewPositions.lastIndex) {
                    programsRepository.updateMesoAndMicroCycle(
                        id = program.id,
                        mesoCycle = program.currentMesocycle,
                        microCycle = program.currentMicrocycle,
                        microCyclePosition = workoutsWithNewPositions.lastIndex
                    )
                }
            }

            syncScheduler.scheduleSync()
        }

        return deleteCount
    }

    override suspend fun deleteMany(models: List<Workout>): Int {
        val toDeleteIds = models.map { it.id }
        if (toDeleteIds.isEmpty()) return 0

        val deletedCount = workoutsDao.softDeleteMany(toDeleteIds)
        if (deletedCount > 0) {
            val affectedProgramIds = models.map { it.programId }.toSet()
            for (programId in affectedProgramIds) {
                val workoutsWithNewPositions = workoutsDao.getAllForProgramWithoutRelationships(programId)
                    .sortedBy { it.position }
                    .fastMapIndexed { index, workoutEntity ->
                        workoutEntity.copy(position = index)
                    }
                workoutsDao.updateMany(workoutsWithNewPositions)
            }

            programsRepository.getActive()?.let { program ->
                if (affectedProgramIds.contains(program.id)) {
                    val workoutCount = workoutsDao.getAllForProgram(program.id).size
                    if (workoutCount > 0 && program.currentMicrocyclePosition >= workoutCount) {
                        programsRepository.updateMesoAndMicroCycle(
                            id = program.id,
                            mesoCycle = program.currentMesocycle,
                            microCycle = program.currentMicrocycle,
                            microCyclePosition = workoutCount - 1
                        )
                    }
                }
            }

            syncScheduler.scheduleSync()
        }

        return deletedCount
    }

    override suspend fun deleteById(id: Long): Int {
        val model = getById(id) ?: return 0
        return delete(model)
    }

    override suspend fun updateMany(models: List<Workout>) {
        val currentEntities = workoutsDao.getManyWithoutRelationships(models.map { it.id }).associateBy { it.id }
        if (currentEntities.isEmpty()) return

        val toUpdate = models.fastMapNotNull { workout ->
            val current = currentEntities[workout.id] ?: return@fastMapNotNull null
            workout.toEntity().applyRemoteStorageMetadata(
                remoteId = current.remoteId,
                remoteLastUpdated = current.remoteLastUpdated,
                synced = false,
            )
        }
        if (toUpdate.isEmpty()) return
        workoutsDao.updateMany(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun upsert(model: Workout): Long {
        val current = workoutsDao.getWithoutRelationships(model.id)
        val toUpsert = model.toEntity().applyRemoteStorageMetadata(
            remoteId = current?.remoteId,
            remoteLastUpdated = current?.remoteLastUpdated,
            synced = false,
        )
        val id = workoutsDao.upsert(toUpsert)
        syncScheduler.scheduleSync()

        return if (id == -1L) toUpsert.id else id
    }

    override suspend fun upsertMany(models: List<Workout>): List<Long> {
        val currentEntities = workoutsDao.getManyWithoutRelationships(models.map { it.id }).associateBy { it.id }
        val toUpsert = models.map { workout ->
            val current = currentEntities[workout.id]
            workout.toEntity().applyRemoteStorageMetadata(
                remoteId = current?.remoteId,
                remoteLastUpdated = current?.remoteLastUpdated,
                synced = false,
            )
        }
        val ids = workoutsDao.upsertMany(toUpsert)
        val entityIds = toUpsert.zip(ids).map { (entity, returnedId) ->
            if (returnedId == -1L) entity else entity.copy(id = returnedId)
        }.fastMap { it.id }

        syncScheduler.scheduleSync()

        return entityIds
    }

    override suspend fun update(model: Workout) {
        val current = workoutsDao.getWithoutRelationships(model.id) ?: return
        val updWorkout = model.toEntity().applyRemoteStorageMetadata(
            remoteId = current.remoteId,
            remoteLastUpdated = current.remoteLastUpdated,
            synced = false,
        )
        val updSets = model.lifts
            .filterIsInstance<CustomWorkoutLift>()
            .flatMap { lift ->
                lift.customLiftSets
            }

        workoutsDao.update(updWorkout)
        workoutLiftsDao.updateMany(model.lifts.fastMap { it.toEntity() })
        customSetsDao.updateMany(updSets.fastMap { it.toEntity() })

        syncScheduler.scheduleSync()
    }

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
}