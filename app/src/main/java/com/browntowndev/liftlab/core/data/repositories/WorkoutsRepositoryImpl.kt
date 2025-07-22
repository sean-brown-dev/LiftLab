package com.browntowndev.liftlab.core.data.repositories

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapIndexed
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.domain.models.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.Workout
import com.browntowndev.liftlab.core.data.mapping.WorkoutMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.data.mapping.WorkoutMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import com.browntowndev.liftlab.core.data.local.dao.WorkoutsDao
import com.browntowndev.liftlab.core.data.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.data.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.data.sync.SyncScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class WorkoutsRepositoryImpl(
    private val workoutLiftsRepositoryImpl: WorkoutLiftsRepositoryImpl,
    private val customLiftSetsRepositoryImpl: CustomLiftSetsRepositoryImpl,
    private val programsRepository: ProgramsRepository,
    private val workoutsDao: WorkoutsDao,
    private val syncScheduler: SyncScheduler,
): WorkoutsRepository {
    override suspend fun updateName(id: Long, newName: String) {
        val current = workoutsDao.getWithoutRelationships(id) ?: return
        val toUpdate = current.copy(name = newName).applyFirestoreMetadata(
            firestoreId = current.remoteId,
            lastUpdated = current.lastUpdated,
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
        val toDelete = workoutsDao.getWithoutRelationships(model.id) ?: return 0
        val deleteCount = workoutsDao.delete(toDelete)

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

        return deleteCount
    }

    override suspend fun deleteMany(models: List<Workout>): Int {
        val toDelete = workoutsDao.getManyWithoutRelationships(models.map { it.id })
        if (toDelete.isEmpty()) return 0

        val deletedCount = workoutsDao.deleteMany(toDelete)
        if (deletedCount == 0) return 0

        val affectedProgramIds = toDelete.map { it.programId }.toSet()
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
            workout.toEntity().copyWithFirestoreMetadata(
                firestoreId = current.remoteId,
                lastUpdated = current.lastUpdated,
                synced = false,
            )
        }
        if (toUpdate.isEmpty()) return
        workoutsDao.updateMany(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun upsert(model: Workout): Long {
        val current = workoutsDao.getWithoutRelationships(model.id)
        val toUpsert = model.toEntity().copyWithFirestoreMetadata(
            firestoreId = current?.remoteId,
            lastUpdated = current?.lastUpdated,
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
            workout.toEntity().copyWithFirestoreMetadata(
                firestoreId = current?.remoteId,
                lastUpdated = current?.lastUpdated,
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
        val updWorkout = model.toEntity().copyWithFirestoreMetadata(
            firestoreId = current.remoteId,
            lastUpdated = current.lastUpdated,
            synced = false,
        )
        val updSets = model.lifts
            .filterIsInstance<CustomWorkoutLift>()
            .flatMap { lift ->
                lift.customLiftSets
            }

        workoutsDao.update(updWorkout)
        workoutLiftsRepositoryImpl.updateMany(model.lifts)
        customLiftSetsRepositoryImpl.updateMany(updSets)

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
    override fun getByMicrocyclePosition(
        programId: Long,
        microcyclePosition: Int,
    ): Flow<Workout?> {
        return workoutsDao.getByMicrocyclePosition(
            programId = programId,
            microcyclePosition = microcyclePosition,
        ).map { workoutEntity ->
            workoutEntity?.toDomainModel()
        }
    }
}