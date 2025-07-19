package com.browntowndev.liftlab.core.persistence.repositories

import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.persistence.dao.CustomSetsDao
import com.browntowndev.liftlab.core.persistence.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.mapping.CustomLiftSetMapper
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.browntowndev.liftlab.core.persistence.sync.BatchSyncQueueEntry
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.sync.SyncQueueEntry
import java.util.UUID

class CustomLiftSetsRepository(
    private val customSetsDao: CustomSetsDao,
    private val customLiftSetMapper: CustomLiftSetMapper,
    private val workoutLiftsDao: WorkoutLiftsDao,
    private val firestoreSyncManager: FirestoreSyncManager,
): Repository {
    suspend fun insert(newSet: GenericLiftSet): Long {
        val entity = customLiftSetMapper.map(newSet)
        val id = customSetsDao.insert(entity)

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                roomEntityIds = listOf(id),
                SyncType.Sync,
            )
        )
        return id
    }

    suspend fun update(set: GenericLiftSet) {
        val current = customSetsDao.get(set.id) ?: return
        val toUpdate = customLiftSetMapper.map(set)
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
                SyncType.Sync,
            )
        )
    }

    suspend fun updateMany(sets: List<GenericLiftSet>) {
        val currentById = customSetsDao.getMany(sets.map { it.id }).associateBy { it.id }
        if (currentById.isEmpty()) return

        val toUpdate = sets.fastMapNotNull {
            val current = currentById[it.id] ?: return@fastMapNotNull null
            customLiftSetMapper.map(it)
                .copyWithFirestoreMetadata(
                    firestoreId = current.firestoreId,
                    lastUpdated = current.lastUpdated,
                    synced = false,
                )
        }
        if (toUpdate.isEmpty()) return

        customSetsDao.updateMany(toUpdate)

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                roomEntityIds = toUpdate.fastMap { it.id },
                SyncType.Sync,
            )
        )
    }

    suspend fun deleteAllForLift(workoutLiftId: Long) {
        val toDelete = customSetsDao.getByWorkoutLiftId(workoutLiftId)
        if (toDelete.isEmpty()) return

        customSetsDao.deleteMany(toDelete)

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                roomEntityIds = toDelete.fastMap { it.id },
                SyncType.Delete,
            )
        )
    }

    suspend fun deleteByPosition(workoutLiftId: Long, position: Int) {
        Log.d("CustomLiftSetsRepository", "deleteByPosition: $workoutLiftId, $position")

        val setsForLift = customSetsDao.getByWorkoutLiftId(workoutLiftId)
        Log.d("CustomLiftSetsRepository", "deleteByPosition: $setsForLift")

        val toDelete = setsForLift.singleOrNull { it.position == position } ?: return
        Log.d("CustomLiftSetsRepository", "deleteByPosition: $toDelete")

        customSetsDao.delete(toDelete)
        customSetsDao.syncPositions(workoutLiftId, position)
        val entitiesToUpdate = customSetsDao.getByWorkoutLiftId(workoutLiftId).map { it.toFirestoreDto() }
        Log.d("CustomLiftSetsRepository", "deleteByPosition: $entitiesToUpdate")

        // Update set count of workout lift
        val currentWorkoutLift = workoutLiftsDao.get(workoutLiftId)!!
        val workoutLiftToUpdate = currentWorkoutLift.copy(setCount = entitiesToUpdate.size).applyFirestoreMetadata(
            firestoreId = currentWorkoutLift.firestoreId,
            lastUpdated = currentWorkoutLift.lastUpdated,
            synced = false,
        )
        workoutLiftsDao.update(workoutLiftToUpdate)
        Log.d("CustomLiftSetsRepository", "deleteByPosition: $workoutLiftToUpdate")

        firestoreSyncManager.enqueueBatchSyncRequest(
            BatchSyncQueueEntry(
                id = UUID.randomUUID().toString(),
                batch = listOf(
                    SyncQueueEntry(
                        collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                        roomEntityIds = entitiesToUpdate.fastMap { it.id },
                        syncType = SyncType.Sync,
                    ),
                    SyncQueueEntry(
                        collectionName = FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
                        roomEntityIds = listOf(workoutLiftToUpdate.id),
                        syncType = SyncType.Sync,
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

    suspend fun insertAll(customSets: List<GenericLiftSet>): List<Long> {
        val toInsert = customSets.map { customLiftSetMapper.map(it) }
        val insertedIds = customSetsDao.insertMany(toInsert)
        val insertedWithIds = toInsert.zip(insertedIds).map { (entity, id) ->
            entity.copy(id = id)
        }

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                roomEntityIds = insertedWithIds.fastMap { it.id },
                syncType = SyncType.Sync,
            )
        )

        return insertedIds
    }
}