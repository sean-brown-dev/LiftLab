package com.browntowndev.liftlab.core.data.repositories

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.data.local.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.mapping.WorkoutLiftMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.data.mapping.WorkoutLiftMappingExtensions.toEntity
import com.browntowndev.liftlab.core.data.remote.SyncScheduler
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutLiftsRepositoryImpl (
    private val workoutLiftsDao: WorkoutLiftsDao,
    private val syncScheduler: SyncScheduler,
): WorkoutLiftsRepository {

    override suspend fun insert(model: GenericWorkoutLift): Long {
        val toInsert = model.toEntity()
        val id = workoutLiftsDao.insert(toInsert)
        syncScheduler.scheduleSync()

        return id
    }

    override suspend fun insertMany(models: List<GenericWorkoutLift>): List<Long> {
        val toInsert = models.map { it.toEntity() }
        val insertIds = workoutLiftsDao.insertMany(toInsert)
        syncScheduler.scheduleSync()

        return insertIds
    }

    override suspend fun updateLiftId(workoutLiftId: Long, newLiftId: Long) {
        val current = workoutLiftsDao.getWithoutRelationships(workoutLiftId) ?: return
        val toUpdate = current.copy(liftId = newLiftId).applyRemoteStorageMetadata(
            remoteId = current.remoteId,
            remoteLastUpdated = current.remoteLastUpdated,
            synced = false,
        )
        workoutLiftsDao.update(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun getAll(): List<GenericWorkoutLift> {
        return workoutLiftsDao.getAll().map { it.toDomainModel() }
    }

    override fun getAllFlow(): Flow<List<GenericWorkoutLift>> {
        return workoutLiftsDao.getAllFlow().map { it.map { entity -> entity.toDomainModel() } }
    }

    override suspend fun getById(id: Long): GenericWorkoutLift? {
        return workoutLiftsDao.get(id)?.toDomainModel()
    }

    override suspend fun getMany(ids: List<Long>): List<GenericWorkoutLift> {
        return workoutLiftsDao.getMany(ids).map { it.toDomainModel() }
    }

    override suspend fun update(model: GenericWorkoutLift) {
        val current = workoutLiftsDao.getWithoutRelationships(model.id) ?: return
        val toUpdate = model.toEntity().applyRemoteStorageMetadata(
            remoteId = current.remoteId,
            remoteLastUpdated = current.remoteLastUpdated,
            synced = false,
        )
        workoutLiftsDao.update(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun updateMany(models: List<GenericWorkoutLift>) {
        val currentEntities = workoutLiftsDao.getManyWithoutRelationships(models.map { it.id }).associateBy { it.id }
        if (currentEntities.isEmpty()) return

        val toUpdate = models.fastMapNotNull { workoutLift ->
            val current = currentEntities[workoutLift.id] ?: return@fastMapNotNull null
            workoutLift.toEntity().applyRemoteStorageMetadata(
                remoteId = current.remoteId,
                remoteLastUpdated = current.remoteLastUpdated,
                synced = false,
            )
        }
        if (toUpdate.isEmpty()) return
        workoutLiftsDao.updateMany(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun upsert(model: GenericWorkoutLift): Long {
        val current = workoutLiftsDao.getWithoutRelationships(model.id)
        val toUpsert = model.toEntity().applyRemoteStorageMetadata(
            remoteId = current?.remoteId,
            remoteLastUpdated = current?.remoteLastUpdated,
            synced = false,
        )
        val id = workoutLiftsDao.upsert(toUpsert)
        syncScheduler.scheduleSync()

        return if (id == -1L) toUpsert.id else id
    }

    override suspend fun upsertMany(models: List<GenericWorkoutLift>): List<Long> {
        val currentEntities = workoutLiftsDao.getManyWithoutRelationships(models.map { it.id }).associateBy { it.id }
        val toUpsert = models.map { workoutLift ->
            val current = currentEntities[workoutLift.id]
            workoutLift.toEntity().applyRemoteStorageMetadata(
                remoteId = current?.remoteId,
                remoteLastUpdated = current?.remoteLastUpdated,
                synced = false,
            )
        }
        val ids = workoutLiftsDao.upsertMany(toUpsert)
        val entityIds = toUpsert.zip(ids).map { (entity, returnedId) ->
            if (returnedId == -1L) entity else entity.copy(id = returnedId)
        }.fastMap { it.id }

        syncScheduler.scheduleSync()

        return entityIds
    }

    override suspend fun delete(model: GenericWorkoutLift): Int {
        val count = workoutLiftsDao.softDelete(model.id)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }

    override suspend fun deleteMany(models: List<GenericWorkoutLift>): Int {
        val ids = models.map { it.id }
        if (ids.isEmpty()) return 0
        val count = workoutLiftsDao.softDeleteMany(ids)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }

    override suspend fun deleteById(id: Long): Int {
        val count = workoutLiftsDao.softDelete(id)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }

    override suspend fun getLiftIdsForWorkout(workoutId: Long): List<Long> {
        return workoutLiftsDao.getLiftIdsForWorkout(workoutId)
    }

    override suspend fun getForWorkout(workoutId: Long): List<GenericWorkoutLift> {
        return workoutLiftsDao.getForWorkout(workoutId).map { liftEntity ->
            liftEntity.toDomainModel()
        }
    }
}