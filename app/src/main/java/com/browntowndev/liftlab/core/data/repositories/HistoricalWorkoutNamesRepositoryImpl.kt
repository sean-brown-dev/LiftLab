package com.browntowndev.liftlab.core.data.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.data.common.SyncType
import com.browntowndev.liftlab.core.data.mapping.HistoricalWorkoutNameMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.data.mapping.HistoricalWorkoutNameMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.models.HistoricalWorkoutName
import com.browntowndev.liftlab.core.domain.repositories.HistoricalWorkoutNamesRepository
import com.browntowndev.liftlab.core.data.local.dao.HistoricalWorkoutNamesDao
import com.browntowndev.liftlab.core.data.remote.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.data.remote.sync.SyncQueueEntry

class HistoricalWorkoutNamesRepositoryImpl(
    private val historicalWorkoutNamesDao: HistoricalWorkoutNamesDao,
    private val firestoreSyncManager: FirestoreSyncManager,
) : HistoricalWorkoutNamesRepository {
    override suspend fun getAll(): List<HistoricalWorkoutName> {
        return historicalWorkoutNamesDao.getAll().map { it.toDomainModel() }
    }

    override suspend fun getById(id: Long): HistoricalWorkoutName? {
        return historicalWorkoutNamesDao.get(id)?.toDomainModel()
    }

    override suspend fun getMany(ids: List<Long>): List<HistoricalWorkoutName> {
        return historicalWorkoutNamesDao.getMany(ids).map { it.toDomainModel() }
    }

    override suspend fun update(model: HistoricalWorkoutName) {
        val toUpdate = model.toEntity()
        historicalWorkoutNamesDao.update(toUpdate)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.HISTORICAL_WORKOUT_NAMES_COLLECTION,
                roomEntityIds = listOf(toUpdate.id),
                SyncType.Upsert
            )
        )
    }

    override suspend fun updateMany(models: List<HistoricalWorkoutName>) {
        val toUpdate = models.map { it.toEntity() }
        historicalWorkoutNamesDao.updateMany(toUpdate)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.HISTORICAL_WORKOUT_NAMES_COLLECTION,
                roomEntityIds = toUpdate.map { it.id },
                SyncType.Upsert
            )
        )
    }

    override suspend fun upsert(model: HistoricalWorkoutName): Long {
        val toUpsert = model.toEntity()
        val id = historicalWorkoutNamesDao.upsert(toUpsert)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.HISTORICAL_WORKOUT_NAMES_COLLECTION,
                roomEntityIds = listOf(if (id == -1L) toUpsert.id else id),
                SyncType.Upsert
            )
        )
        return if (id == -1L) toUpsert.id else id
    }

    override suspend fun upsertMany(models: List<HistoricalWorkoutName>): List<Long> {
        val toUpsert = models.map { it.toEntity() }
        val ids = historicalWorkoutNamesDao.upsertMany(toUpsert)
        val entityIds = toUpsert.zip(ids).map { (entity, returnedId) ->
            if (returnedId == -1L) entity else entity.copy(id = returnedId)
        }.fastMap { it.id }

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.HISTORICAL_WORKOUT_NAMES_COLLECTION,
                roomEntityIds = entityIds,
                SyncType.Upsert
            )
        )
        return entityIds
    }

    override suspend fun insert(model: HistoricalWorkoutName): Long {
        val toInsert = model.toEntity()
        val id = historicalWorkoutNamesDao.insert(toInsert)

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.HISTORICAL_WORKOUT_NAMES_COLLECTION,
                roomEntityIds = listOf(id),
                SyncType.Upsert,
            )
        )

        return id
    }

    override suspend fun insertMany(models: List<HistoricalWorkoutName>): List<Long> {
        val toInsert = models.map { it.toEntity() }
        val ids = historicalWorkoutNamesDao.insertMany(toInsert)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.HISTORICAL_WORKOUT_NAMES_COLLECTION,
                roomEntityIds = ids,
                SyncType.Upsert
            )
        )
        return ids
    }

    override suspend fun delete(model: HistoricalWorkoutName): Int {
        val toDelete = model.toEntity()
        val count = historicalWorkoutNamesDao.delete(toDelete)
        if (count > 0 && toDelete.remoteId != null) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.HISTORICAL_WORKOUT_NAMES_COLLECTION,
                    roomEntityIds = listOf(toDelete.id),
                    SyncType.Delete
                )
            )
        }
        return count
    }

    override suspend fun deleteMany(models: List<HistoricalWorkoutName>): Int {
        val toDelete = models.map { it.toEntity() }
        val count = historicalWorkoutNamesDao.deleteMany(toDelete)
        val firestoreIds = toDelete.mapNotNull { it.remoteId }
        if (firestoreIds.isNotEmpty() && count > 0) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.HISTORICAL_WORKOUT_NAMES_COLLECTION,
                    roomEntityIds = toDelete.map { it.id },
                    SyncType.Delete
                )
            )
        }
        return count
    }

    override suspend fun deleteById(id: Long): Int {
        val toDelete = historicalWorkoutNamesDao.get(id) ?: return 0
        val count = historicalWorkoutNamesDao.delete(toDelete)
        if (count > 0 && toDelete.remoteId != null) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.HISTORICAL_WORKOUT_NAMES_COLLECTION,
                    roomEntityIds = listOf(toDelete.id),
                    SyncType.Delete
                )
            )
        }
        return count
    }

    override suspend fun getIdByProgramAndWorkoutId(programId: Long, workoutId: Long): Long? {
        return historicalWorkoutNamesDao.getByProgramAndWorkoutId(programId, workoutId)?.id
    }
}