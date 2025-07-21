package com.browntowndev.liftlab.core.persistence.room.repositories

import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.domain.mapping.CustomLiftSetMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.domain.mapping.CustomLiftSetMappingExtensions.toEntity
import com.browntowndev.liftlab.core.persistence.room.dao.CustomSetsDao
import com.browntowndev.liftlab.core.persistence.room.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toFirestoreDto
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.persistence.room.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.persistence.firestore.sync.BatchSyncQueueEntry
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncQueueEntry
import java.util.UUID

class CustomLiftSetsRepositoryImpl(
    private val customSetsDao: CustomSetsDao,
    private val workoutLiftsDao: WorkoutLiftsDao,
    private val firestoreSyncManager: FirestoreSyncManager,
): CustomLiftSetsRepository {
    override suspend fun getAll(): List<GenericLiftSet> =
        customSetsDao.getAll().map { it.toDomainModel() }

    override suspend fun getById(id: Long): GenericLiftSet? =
        customSetsDao.get(id)?.let { return it.toDomainModel() }

    override suspend fun getMany(ids: List<Long>): List<GenericLiftSet> =
        customSetsDao.getMany(ids).map { it.toDomainModel() }

    override suspend fun update(model: GenericLiftSet) {
        val current = customSetsDao.get(model.id) ?: return
        val toUpdate = model.toEntity()
            .applyFirestoreMetadata(
                firestoreId = current.firestoreId,
                lastUpdated = current.lastUpdated,
                synced = false,
            )
        customSetsDao.update(toUpdate)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                roomEntityIds = listOf(toUpdate.id),
                SyncType.Upsert,
            )
        )
    }

    override suspend fun updateManyAndGetSyncQueueEntry(sets: List<GenericLiftSet>): SyncQueueEntry? {
        val currentById = customSetsDao.getMany(sets.map { it.id }).associateBy { it.id }
        if (currentById.isEmpty()) return null

        val toUpdate = sets.fastMapNotNull {
            val current = currentById[it.id] ?: return@fastMapNotNull null
            it.toEntity().copyWithFirestoreMetadata(
                firestoreId = current.firestoreId,
                lastUpdated = current.lastUpdated,
                synced = false,
            )
        }
        if (toUpdate.isEmpty()) return null

        customSetsDao.updateMany(toUpdate)

        return SyncQueueEntry(
            collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
            roomEntityIds = toUpdate.fastMap { it.id },
            SyncType.Upsert,
        )
    }

    override suspend fun updateMany(models: List<GenericLiftSet>) {
        val syncQueueEntry = updateManyAndGetSyncQueueEntry(models) ?: return
        firestoreSyncManager.enqueueSyncRequest(syncQueueEntry)
    }

    override suspend fun upsert(model: GenericLiftSet): Long {
        val toUpsert = model.toEntity()
        val id = customSetsDao.upsert(model.toEntity())
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                roomEntityIds = listOf(if (id == -1L) toUpsert.id else id),
                SyncType.Upsert,
            )
        )

        return id
    }

    override suspend fun upsertMany(models: List<GenericLiftSet>): List<Long> {
        var toUpsert = models.map { it.toEntity() }
        val upsertIds = customSetsDao.upsertMany(toUpsert)

        toUpsert = toUpsert.zip(upsertIds).map { (entity, id) ->
            if (id == -1L) entity else entity.copy(id = id)
        }
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                roomEntityIds = toUpsert.fastMap { it.id },
                SyncType.Upsert,
            )
        )

        return upsertIds
    }

    override suspend fun insertMany(models: List<GenericLiftSet>): List<Long> {
        val toInsert = models.map { it.toEntity() }
        val insertedIds = customSetsDao.insertMany(toInsert)
        val insertedWithIds = toInsert.zip(insertedIds).map { (entity, id) ->
            entity.copy(id = id)
        }

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                roomEntityIds = insertedWithIds.fastMap { it.id },
                syncType = SyncType.Upsert,
            )
        )

        return insertedIds
    }

    override suspend fun insert(model: GenericLiftSet): Long {
        val entity = model.toEntity()
        val id = customSetsDao.insert(entity)

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                roomEntityIds = listOf(id),
                SyncType.Upsert,
            )
        )
        return id
    }

    override suspend fun deleteAllForLift(workoutLiftId: Long) {
        val toDelete = customSetsDao.getByWorkoutLiftId(workoutLiftId)
        if (toDelete.isEmpty()) return

        customSetsDao.deleteMany(toDelete)
        toDelete
            .fastMapNotNull { it.firestoreId?.let { _ -> it.id } }
            .takeIf { it.isNotEmpty() }
            ?.let { ids ->
                firestoreSyncManager.enqueueSyncRequest(
                    SyncQueueEntry(
                        collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                        roomEntityIds = toDelete.fastMap { it.id },
                        SyncType.Delete,
                    )
                )
            }
    }

    override suspend fun deleteByPosition(workoutLiftId: Long, position: Int) {
        Log.d("CustomLiftSetsRepositoryImpl", "deleteByPosition: $workoutLiftId, $position")

        val setsForLift = customSetsDao.getByWorkoutLiftId(workoutLiftId)
        Log.d("CustomLiftSetsRepositoryImpl", "deleteByPosition: $setsForLift")

        val toDelete = setsForLift.singleOrNull { it.position == position } ?: return
        Log.d("CustomLiftSetsRepositoryImpl", "deleteByPosition: $toDelete")

        customSetsDao.delete(toDelete)
        customSetsDao.syncPositions(workoutLiftId, position)
        val entitiesToUpdate = customSetsDao.getByWorkoutLiftId(workoutLiftId).map { it.toFirestoreDto() }
        Log.d("CustomLiftSetsRepositoryImpl", "deleteByPosition: $entitiesToUpdate")

        // Update set count of workoutEntity liftEntity
        val currentWorkoutLift = workoutLiftsDao.get(workoutLiftId)!!
        val workoutLiftToUpdate = currentWorkoutLift.copy(setCount = entitiesToUpdate.size).applyFirestoreMetadata(
            firestoreId = currentWorkoutLift.firestoreId,
            lastUpdated = currentWorkoutLift.lastUpdated,
            synced = false,
        )
        workoutLiftsDao.update(workoutLiftToUpdate)
        Log.d("CustomLiftSetsRepositoryImpl", "deleteByPosition: $workoutLiftToUpdate")

        firestoreSyncManager.enqueueBatchSyncRequest(
            BatchSyncQueueEntry(
                id = UUID.randomUUID().toString(),
                batch = listOf(
                    SyncQueueEntry(
                        collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                        roomEntityIds = entitiesToUpdate.fastMap { it.id },
                        syncType = SyncType.Upsert,
                    ),
                    SyncQueueEntry(
                        collectionName = FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
                        roomEntityIds = listOf(workoutLiftToUpdate.id),
                        syncType = SyncType.Upsert,
                    ),
                ).let { batches ->
                    val batchesMaybeWithDelete = batches.toMutableList()
                    if (toDelete.firestoreId != null) {
                        val deleteEntry = SyncQueueEntry(
                            collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                            roomEntityIds = listOf(toDelete.id),
                            syncType = SyncType.Delete,
                        )
                        batchesMaybeWithDelete.add(0, deleteEntry)
                    }
                    batchesMaybeWithDelete
                }
            )
        )
    }

    override suspend fun delete(model: GenericLiftSet): Int {
        val toDelete = customSetsDao.get(model.id) ?: return 0
        return deleteWithoutRefetch(toDelete)
    }

    override suspend fun deleteMany(models: List<GenericLiftSet>): Int {
        val toDelete = customSetsDao.getMany(models.map { it.id })
        if (toDelete.isEmpty()) return 0
        val deleteCount = customSetsDao.deleteMany(toDelete)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                roomEntityIds = toDelete.fastMap { it.id },
                SyncType.Delete,
            )
        )

        return deleteCount
    }

    override suspend fun deleteById(id: Long): Int {
        val toDelete = customSetsDao.get(id) ?: return 0
        return deleteWithoutRefetch(toDelete)
    }

    private suspend fun deleteWithoutRefetch(entity: CustomLiftSetEntity): Int {
        val deleteCount = customSetsDao.delete(entity)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                roomEntityIds = listOf(entity.id),
                SyncType.Delete,
            )
        )

        return deleteCount
    }
}