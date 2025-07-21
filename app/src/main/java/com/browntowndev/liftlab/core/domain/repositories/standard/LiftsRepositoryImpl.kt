package com.browntowndev.liftlab.core.domain.repositories.standard

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
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
        val toInsert =
            LiftEntity(
                id = model.id,
                name = model.name,
                movementPattern = model.movementPattern,
                volumeTypesBitmask = model.volumeTypesBitmask,
                secondaryVolumeTypesBitmask = model.secondaryVolumeTypesBitmask,
                incrementOverride = model.incrementOverride,
                restTime = model.restTime,
                restTimerEnabled = model.restTimerEnabled,
                isHidden = model.isHidden,
                isBodyweight = model.isBodyweight,
                note = model.note,
            )
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
        val toUpdate =
            LiftEntity(
                id = model.id,
                name = model.name,
                movementPattern = model.movementPattern,
                volumeTypesBitmask = model.volumeTypesBitmask,
                secondaryVolumeTypesBitmask = model.secondaryVolumeTypesBitmask,
                incrementOverride = model.incrementOverride,
                restTime = model.restTime,
                restTimerEnabled = model.restTimerEnabled,
                isHidden = model.isHidden,
                isBodyweight = model.isBodyweight,
                note = model.note,
            ).applyFirestoreMetadata(
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
        return liftsDao.getAll().fastMap { lift ->
            Lift(
                id = lift.id,
                name = lift.name,
                movementPattern = lift.movementPattern,
                volumeTypesBitmask = lift.volumeTypesBitmask,
                secondaryVolumeTypesBitmask = lift.secondaryVolumeTypesBitmask,
                incrementOverride = lift.incrementOverride,
                restTime = lift.restTime,
                restTimerEnabled = lift.restTimerEnabled,
                isHidden = lift.isHidden,
                isBodyweight = lift.isBodyweight,
                note = lift.note,
            )
        }
    }

    override fun getAllFlow(): Flow<List<Lift>> {
        return liftsDao.getAllAsFlow().map { lifts ->
            lifts.fastMap {
                Lift(
                    id = it.id,
                    name = it.name,
                    movementPattern = it.movementPattern,
                    volumeTypesBitmask = it.volumeTypesBitmask,
                    secondaryVolumeTypesBitmask = it.secondaryVolumeTypesBitmask,
                    incrementOverride = it.incrementOverride,
                    restTime = it.restTime,
                    restTimerEnabled = it.restTimerEnabled,
                    isHidden = it.isHidden,
                    isBodyweight = it.isBodyweight,
                    note = it.note,
                )
            }
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
        return liftsDao.get(id)?.let {
            Lift(
                id = it.id,
                name = it.name,
                movementPattern = it.movementPattern,
                volumeTypesBitmask = it.volumeTypesBitmask,
                secondaryVolumeTypesBitmask = it.secondaryVolumeTypesBitmask,
                incrementOverride = it.incrementOverride,
                restTime = it.restTime,
                restTimerEnabled = it.restTimerEnabled,
                isHidden = it.isHidden,
                isBodyweight = it.isBodyweight,
                note = it.note,
            )
        }
    }

    override suspend fun getMany(ids: List<Long>): List<Lift> {
        return liftsDao.getMany(ids).fastMap {
            Lift(
                id = it.id,
                name = it.name,
                movementPattern = it.movementPattern,
                volumeTypesBitmask = it.volumeTypesBitmask,
                secondaryVolumeTypesBitmask = it.secondaryVolumeTypesBitmask,
                incrementOverride = it.incrementOverride,
                restTime = it.restTime,
                restTimerEnabled = it.restTimerEnabled,
                isHidden = it.isHidden,
                isBodyweight = it.isBodyweight,
                note = it.note,
            )
        }
    }

    override suspend fun updateMany(models: List<Lift>) {
        val entities = models.map {
            LiftEntity(
                id = it.id,
                name = it.name,
                movementPattern = it.movementPattern,
                volumeTypesBitmask = it.volumeTypesBitmask,
                secondaryVolumeTypesBitmask = it.secondaryVolumeTypesBitmask,
                incrementOverride = it.incrementOverride,
                restTime = it.restTime,
                restTimerEnabled = it.restTimerEnabled,
                isHidden = it.isHidden,
                isBodyweight = it.isBodyweight,
                note = it.note,
            )
        }
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
        val toUpsert = LiftEntity(
            id = model.id,
            name = model.name,
            movementPattern = model.movementPattern,
            volumeTypesBitmask = model.volumeTypesBitmask,
            secondaryVolumeTypesBitmask = model.secondaryVolumeTypesBitmask,
            incrementOverride = model.incrementOverride,
            restTime = model.restTime,
            restTimerEnabled = model.restTimerEnabled,
            isHidden = model.isHidden,
            isBodyweight = model.isBodyweight,
            note = model.note,
        ).applyFirestoreMetadata(
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
        var entities = models.map {
            LiftEntity(
                id = it.id,
                name = it.name,
                movementPattern = it.movementPattern,
                volumeTypesBitmask = it.volumeTypesBitmask,
                secondaryVolumeTypesBitmask = it.secondaryVolumeTypesBitmask,
                incrementOverride = it.incrementOverride,
                restTime = it.restTime,
                restTimerEnabled = it.restTimerEnabled,
                isHidden = it.isHidden,
                isBodyweight = it.isBodyweight,
                note = it.note,
            )
        }
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
        val entities = models.map {
            LiftEntity(
                id = it.id,
                name = it.name,
                movementPattern = it.movementPattern,
                volumeTypesBitmask = it.volumeTypesBitmask,
                secondaryVolumeTypesBitmask = it.secondaryVolumeTypesBitmask,
                incrementOverride = it.incrementOverride,
                restTime = it.restTime,
                restTimerEnabled = it.restTimerEnabled,
                isHidden = it.isHidden,
                isBodyweight = it.isBodyweight,
                note = it.note,
            )
        }
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
        val entity = LiftEntity(
            id = model.id,
            name = model.name,
            movementPattern = model.movementPattern,
            volumeTypesBitmask = model.volumeTypesBitmask,
            secondaryVolumeTypesBitmask = model.secondaryVolumeTypesBitmask,
            incrementOverride = model.incrementOverride,
            restTime = model.restTime,
            restTimerEnabled = model.restTimerEnabled,
            isHidden = model.isHidden,
            isBodyweight = model.isBodyweight,
            note = model.note,
        )
        val count = liftsDao.delete(entity)
        if (entity.firestoreId != null && count > 0) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.LIFTS_COLLECTION,
                    roomEntityIds = listOf(entity.id),
                    SyncType.Delete
                )
            )
        }
        return count
    }

    override suspend fun deleteMany(models: List<Lift>): Int {
        val entities = models.map {
            LiftEntity(
                id = it.id,
                name = it.name,
                movementPattern = it.movementPattern,
                volumeTypesBitmask = it.volumeTypesBitmask,
                secondaryVolumeTypesBitmask = it.secondaryVolumeTypesBitmask,
                incrementOverride = it.incrementOverride,
                restTime = it.restTime,
                restTimerEnabled = it.restTimerEnabled,
                isHidden = it.isHidden,
                isBodyweight = it.isBodyweight,
                note = it.note,
            )
        }
        val count = liftsDao.deleteMany(entities)
        val firestoreIds = entities.mapNotNull { it.firestoreId }
        if (firestoreIds.isNotEmpty() && count > 0) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.LIFTS_COLLECTION,
                    roomEntityIds = entities.fastMap { it.id },
                    SyncType.Delete
                )
            )
        }
        return count
    }

    override suspend fun deleteById(id: Long): Int {
        val toDelete = liftsDao.get(id)
        return if (toDelete != null) {
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
            count
        } else {
            0
        }
    }
}