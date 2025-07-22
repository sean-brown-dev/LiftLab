package com.browntowndev.liftlab.core.data.repositories

import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.data.mapping.CustomLiftSetMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.data.mapping.CustomLiftSetMappingExtensions.toEntity
import com.browntowndev.liftlab.core.data.local.dao.CustomSetsDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.collections.map

class CustomLiftSetsRepositoryImpl(
    private val customSetsDao: CustomSetsDao,
    private val workoutLiftsDao: WorkoutLiftsDao,
    private val syncScheduler: SyncScheduler,
): CustomLiftSetsRepository {
    override suspend fun getAll(): List<GenericLiftSet> =
        customSetsDao.getAll().map { it.toDomainModel() }

    override fun getAllFlow(): Flow<List<GenericLiftSet>> =
        customSetsDao.getAllFlow().map { it.map { entity -> entity.toDomainModel() } }

    override suspend fun getById(id: Long): GenericLiftSet? =
        customSetsDao.get(id)?.let { return it.toDomainModel() }

    override suspend fun getMany(ids: List<Long>): List<GenericLiftSet> =
        customSetsDao.getMany(ids).map { it.toDomainModel() }

    override suspend fun update(model: GenericLiftSet) {
        val current = customSetsDao.get(model.id) ?: return
        val toUpdate = model.toEntity()
            .applyRemoteStorageMetadata(
                remoteId = current.remoteId,
                remoteLastUpdated = current.lastUpdated,
                synced = false,
            )
        customSetsDao.update(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun updateMany(models: List<GenericLiftSet>) {
        val currentById = customSetsDao.getMany(models.map { it.id }).associateBy { it.id }
        if (currentById.isEmpty()) return

        val toUpdate = models.fastMapNotNull {
            val current = currentById[it.id] ?: return@fastMapNotNull null
            it.toEntity().applyRemoteStorageMetadata(
                remoteId = current.remoteId,
                remoteLastUpdated = current.lastUpdated,
                synced = false,
            )
        }
        if (toUpdate.isEmpty()) return
        customSetsDao.updateMany(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun upsert(model: GenericLiftSet): Long {
        val toUpsert = model.toEntity()
        val id = customSetsDao.upsert(model.toEntity())
        syncScheduler.scheduleSync()

        return if (id == -1L) toUpsert.id else id
    }

    override suspend fun upsertMany(models: List<GenericLiftSet>): List<Long> {
        val toUpsert = models.map { it.toEntity() }
        val upsertResultIds = customSetsDao.upsertMany(toUpsert)
        val entityIds = toUpsert.zip(upsertResultIds).map { (entity, id) ->
            if (id == -1L) entity else entity.copy(id = id)
        }.fastMap { it.id }

        syncScheduler.scheduleSync()

        return entityIds
    }

    override suspend fun insertMany(models: List<GenericLiftSet>): List<Long> {
        val toInsert = models.map { it.toEntity() }
        val insertedIds = customSetsDao.insertMany(toInsert)
        syncScheduler.scheduleSync()

        return insertedIds
    }

    override suspend fun insert(model: GenericLiftSet): Long {
        val entity = model.toEntity()
        val id = customSetsDao.insert(entity)
        syncScheduler.scheduleSync()

        return id
    }

    override suspend fun deleteAllForLift(workoutLiftId: Long) {
        val toDelete = customSetsDao.getByWorkoutLiftId(workoutLiftId)
        if (toDelete.isEmpty()) return

        val deletedCount = customSetsDao.softDeleteMany(toDelete.map { it.id })
        if (deletedCount > 0) {
            syncScheduler.scheduleSync()
        }
    }

    override suspend fun deleteByPosition(workoutLiftId: Long, position: Int) {
        Log.d("CustomLiftSetsRepositoryImpl", "deleteByPosition: $workoutLiftId, $position")

        val setsForLift = customSetsDao.getByWorkoutLiftId(workoutLiftId)
        Log.d("CustomLiftSetsRepositoryImpl", "deleteByPosition: $setsForLift")

        val toDelete = setsForLift.singleOrNull { it.position == position } ?: return
        Log.d("CustomLiftSetsRepositoryImpl", "deleteByPosition: $toDelete")

        val deletedCount = customSetsDao.softDelete(toDelete.id)
        if (deletedCount > 0) {
            customSetsDao.syncPositions(workoutLiftId, position)
            val entitiesToUpdate = customSetsDao.getByWorkoutLiftId(workoutLiftId)
            Log.d("CustomLiftSetsRepositoryImpl", "deleteByPosition: $entitiesToUpdate")

            // Update set count of workoutEntity liftEntity
            val currentWorkoutLift = workoutLiftsDao.getWithoutRelationships(workoutLiftId)!!
            val workoutLiftToUpdate = currentWorkoutLift.copy(setCount = entitiesToUpdate.size).applyRemoteStorageMetadata(
                remoteId = currentWorkoutLift.remoteId,
                remoteLastUpdated = currentWorkoutLift.lastUpdated,
                synced = false,
            )
            workoutLiftsDao.update(workoutLiftToUpdate)
            Log.d("CustomLiftSetsRepositoryImpl", "deleteByPosition: $workoutLiftToUpdate")

            syncScheduler.scheduleSync()
        }
    }

    override suspend fun delete(model: GenericLiftSet): Int {
        return deleteWithoutRefetch(model.id)
    }

    override suspend fun deleteMany(models: List<GenericLiftSet>): Int {
        val toDeleteIds = models.map { it.id }
        if (toDeleteIds.isEmpty()) return 0
        val deleteCount = customSetsDao.softDeleteMany(toDeleteIds)
        if (deleteCount > 0) {
            syncScheduler.scheduleSync()
        }

        return deleteCount
    }

    override suspend fun deleteById(id: Long): Int {
        return deleteWithoutRefetch(id)
    }

    private suspend fun deleteWithoutRefetch(id: Long): Int {
        val deleteCount = customSetsDao.softDelete(id)
        if (deleteCount > 0) {
            syncScheduler.scheduleSync()
        }

        return deleteCount
    }
}