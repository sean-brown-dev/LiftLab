package com.browntowndev.liftlab.core.data.local.repositories

import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.data.common.CommandType
import com.browntowndev.liftlab.core.data.local.dao.CustomSetsDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.mapping.toDomainModel
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.remote.SyncScheduler
import com.browntowndev.liftlab.core.domain.extensions.copyId
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutLiftsRepositoryImpl (
    private val workoutLiftsDao: WorkoutLiftsDao,
    private val customSetsDao: CustomSetsDao,
    private val syncScheduler: SyncScheduler,
): WorkoutLiftsRepository {
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

    override suspend fun getLiftIdsForWorkout(workoutId: Long): List<Long> {
        return workoutLiftsDao.getLiftIdsForWorkout(workoutId)
    }

    override suspend fun getForWorkout(workoutId: Long): List<GenericWorkoutLift> {
        return workoutLiftsDao.getForWorkout(workoutId).map { liftEntity ->
            liftEntity.toDomainModel()
        }
    }

    override suspend fun insert(model: GenericWorkoutLift): Long {
        val toInsert = model.toEntity()
        val id = workoutLiftsDao.insert(toInsert)
        performCommandForChildren(model.copyId(id = id), CommandType.INSERT)
        syncScheduler.scheduleSync()

        return id
    }

    override suspend fun insertMany(models: List<GenericWorkoutLift>): List<Long> {
        val toInsert = models.map { it.toEntity() }
        val insertIds = workoutLiftsDao.insertMany(toInsert)
        models.zip(insertIds).fastForEach { (model, id) ->
            performCommandForChildren(model.copyId(id = id), CommandType.INSERT)
        }
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

    override suspend fun update(model: GenericWorkoutLift) {
        val current = workoutLiftsDao.getWithoutRelationships(model.id) ?: return
        val toUpdate = model.toEntity().applyRemoteStorageMetadata(
            remoteId = current.remoteId,
            remoteLastUpdated = current.remoteLastUpdated,
            synced = false,
        )
        workoutLiftsDao.update(toUpdate)
        performCommandForChildren(model, CommandType.UPDATE)
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
        models.fastForEach { workoutLift ->
            performCommandForChildren(workoutLift, CommandType.UPDATE)
        }
        syncScheduler.scheduleSync()
    }

    override suspend fun upsert(model: GenericWorkoutLift): Long {
        val current = workoutLiftsDao.getWithoutRelationships(model.id)
        val toUpsert = model.toEntity().applyRemoteStorageMetadata(
            remoteId = current?.remoteId,
            remoteLastUpdated = current?.remoteLastUpdated,
            synced = false,
        )
        val id = workoutLiftsDao.upsert(toUpsert).let { if (it == -1L) toUpsert.id else it }
        performCommandForChildren(model.copyId(id = id), CommandType.UPSERT)
        syncScheduler.scheduleSync()

        return id
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
        val upsertedModels = models.zip(ids).fastMap { (entity, returnedId) ->
            if (returnedId == -1L) entity else entity.copyId(id = returnedId)
        }
        upsertedModels.fastForEach { workoutLift ->
            performCommandForChildren(workoutLift, CommandType.UPSERT)
        }

        syncScheduler.scheduleSync()

        return upsertedModels.fastMap { it.id }
    }

    override suspend fun delete(model: GenericWorkoutLift): Int {
        val count = workoutLiftsDao.softDelete(model.id)
        if (count > 0) {
            customSetsDao.softDeleteByWorkoutLiftId(model.id)
            syncScheduler.scheduleSync()
        }
        return count
    }

    override suspend fun deleteMany(models: List<GenericWorkoutLift>): Int {
        val ids = models.map { it.id }
        if (ids.isEmpty()) return 0
        val count = workoutLiftsDao.softDeleteMany(ids)
        if (count > 0) {
            ids.fastForEach { workoutLiftId ->
                customSetsDao.softDeleteByWorkoutLiftId(workoutLiftId)
            }
            syncScheduler.scheduleSync()
        }
        return count
    }

    override suspend fun deleteById(id: Long): Int {
        val count = workoutLiftsDao.softDelete(id)
        if (count > 0) {
            customSetsDao.softDeleteByWorkoutLiftId(id)
            syncScheduler.scheduleSync()
        }
        return count
    }

    private suspend fun performCommandForChildren(workoutLift: GenericWorkoutLift, commandType: CommandType) {
        // Load what currently exists under this lift (for metadata reuse + diff-delete)
        val existingSetEntities = customSetsDao.getByWorkoutLiftId(workoutLift.id)
        val existingSetsById = existingSetEntities.associateBy { it.id }

        when (workoutLift) {
            is CustomWorkoutLift -> {
                // Persist sets (FK -> workoutLift.id; carry remote metadata; synced=false)
                val setEntities = workoutLift.customLiftSets.fastMap { set ->
                    set.toEntity()
                        .copy(workoutLiftId = workoutLift.id)
                        .applyRemoteStorageMetadata(
                            remoteId = existingSetsById[set.id]?.remoteId,
                            remoteLastUpdated = existingSetsById[set.id]?.remoteLastUpdated,
                            synced = false
                        )
                }

                when (commandType) {
                    CommandType.UPSERT -> customSetsDao.upsertMany(setEntities)
                    CommandType.UPDATE -> customSetsDao.updateMany(setEntities.fastFilter { it.id != 0L })
                    CommandType.INSERT -> customSetsDao.insertMany(setEntities)
                }

                // Diff-delete sets that were removed from this CUSTOM lift
                val keepIds = workoutLift.customLiftSets.fastMap { it.id }.fastFilter { it != 0L }.toSet()
                val removedIds = existingSetEntities.fastMap { it.id }.fastFilter { it !in keepIds }
                if (removedIds.isNotEmpty()) {
                    customSetsDao.softDeleteMany(removedIds)
                }
            }

            is StandardWorkoutLift -> {
                // Standard lifts should not have sets — purge anything currently attached
                if (existingSetEntities.isNotEmpty()) {
                    customSetsDao.softDeleteMany(existingSetEntities.fastMap { it.id })
                }
            }
        }
    }
}