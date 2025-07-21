package com.browntowndev.liftlab.core.domain.repositories.standard

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.domain.mapping.LiftMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.domain.mapping.LiftMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.models.Lift
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.entities.room.LiftEntity
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncQueueEntry
import com.browntowndev.liftlab.core.persistence.room.dao.LiftsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

class LiftsRepositoryImpl(
    private val liftsDao: LiftsDao,
    private val firestoreSyncManager: FirestoreSyncManager,
) : LiftsRepository {
    override suspend fun insert(model: Lift): Long {
        val toInsert = model.toEntity()
        val id = liftsDao.insert(toInsert)

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.LIFTS_COLLECTION,
                roomEntityIds = listOf(id),
                SyncType.Upsert,
            )
        )
        return id
    }

    override suspend fun update(model: Lift) {
        val current = liftsDao.get(model.id)
        val toUpdate = model.toEntity().applyFirestoreMetadata(
                firestoreId = current?.firestoreId,
                lastUpdated = current?.lastUpdated,
                synced = false,
            )
        liftsDao.update(toUpdate)

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.LIFTS_COLLECTION,
                roomEntityIds = listOf(toUpdate.id),
                SyncType.Upsert,
            )
        )
    }

    override suspend fun getAll(): List<Lift> {
        return liftsDao.getAll().fastMap { it.toDomainModel() }
    }

    override fun getAllFlow(): Flow<List<Lift>> {
        return liftsDao.getAllAsFlow().map { lifts ->
            lifts.fastMap { it.toDomainModel() }
        }
    }

    override suspend fun updateRestTime(id: Long, enabled: Boolean, newRestTime: Duration?) {
        val current = liftsDao.get(id) ?: return
        val toUpdate = current.copy(restTimerEnabled = enabled, restTime = newRestTime)
            .applyFirestoreMetadata(
                firestoreId = current.firestoreId,
                lastUpdated = current.lastUpdated,
                synced = false,
            )
        updateWithoutRefetch(toUpdate)
    }

    override suspend fun updateIncrementOverride(id: Long, newIncrement: Float?) {
        val current = liftsDao.get(id) ?: return
        val toUpdate = current.copy(incrementOverride = newIncrement).applyFirestoreMetadata(
            firestoreId = current.firestoreId,
            lastUpdated = current.lastUpdated,
            synced = false,
        )
        updateWithoutRefetch(toUpdate)
    }

    override suspend fun updateNote(id: Long, note: String?) {
        val current = liftsDao.get(id) ?: return
        val toUpdate = current.copy(note = note).applyFirestoreMetadata(
            firestoreId = current.firestoreId,
            lastUpdated = current.lastUpdated,
            synced = false,
        )
        updateWithoutRefetch(toUpdate)
    }

    private suspend fun updateWithoutRefetch(liftEntity: LiftEntity) {
        liftsDao.update(liftEntity)

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.LIFTS_COLLECTION,
                roomEntityIds = listOf(liftEntity.id),
                SyncType.Upsert,
            )
        )
    }

    override suspend fun getById(id: Long): Lift? {
        return liftsDao.get(id)?.toDomainModel()
    }

    override suspend fun getMany(ids: List<Long>): List<Lift> {
        return liftsDao.getMany(ids).fastMap { it.toDomainModel() }
    }

    override suspend fun updateMany(models: List<Lift>) {
        val entities = models.fastMap { it.toEntity() }
        liftsDao.updateMany(entities)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.LIFTS_COLLECTION,
                roomEntityIds = entities.map { it.id },
                SyncType.Upsert
            )
        )
    }

    override suspend fun upsert(model: Lift): Long {
        val current = liftsDao.get(model.id)
        val toUpsert = model.toEntity().applyFirestoreMetadata(
            firestoreId = current?.firestoreId,
            lastUpdated = current?.lastUpdated,
            synced = false,
        )
        val id = liftsDao.upsert(toUpsert)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.LIFTS_COLLECTION,
                roomEntityIds = listOf(if (id == -1L) toUpsert.id else id),
                SyncType.Upsert
            )
        )
        return id
    }

    override suspend fun upsertMany(models: List<Lift>): List<Long> {
        var entities = models.map { it.toEntity() }
        val ids = liftsDao.upsertMany(entities)
        entities = entities.zip(ids).fastMap { (entity, id) ->
            if (id == -1L) entity else entity.copy(id = id)
        }
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.LIFTS_COLLECTION,
                roomEntityIds = entities.fastMap { it.id },
                SyncType.Upsert
            )
        )
        return ids
    }

    override suspend fun insertMany(models: List<Lift>): List<Long> {
        val entities = models.map { it.toEntity() }
        val ids = liftsDao.insertMany(entities)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.LIFTS_COLLECTION,
                roomEntityIds = ids,
                SyncType.Upsert
            )
        )
        return ids
    }

    override suspend fun delete(model: Lift): Int {
        val toDelete = model.toEntity()
        val count = liftsDao.delete(toDelete)
        if (toDelete.firestoreId != null && count > 0) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.LIFTS_COLLECTION,
                    roomEntityIds = listOf(toDelete.id),
                    SyncType.Delete
                )
            )
        }
        return count
    }

    override suspend fun deleteMany(models: List<Lift>): Int {
        val toDelete = models.fastMap { it.toEntity() }
        val count = liftsDao.deleteMany(toDelete)
        val firestoreIds = toDelete.mapNotNull { it.firestoreId }
        if (firestoreIds.isNotEmpty() && count > 0) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.LIFTS_COLLECTION,
                    roomEntityIds = toDelete.fastMap { it.id },
                    SyncType.Delete
                )
            )
        }
        return count
    }

    override suspend fun deleteById(id: Long): Int {
        val toDelete = liftsDao.get(id) ?: return 0
        val count = liftsDao.delete(toDelete)
        if (toDelete.firestoreId != null && count > 0) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.LIFTS_COLLECTION,
                    roomEntityIds = listOf(toDelete.id),
                    SyncType.Delete,
                )
            )
        }
        
        return count
    }
}