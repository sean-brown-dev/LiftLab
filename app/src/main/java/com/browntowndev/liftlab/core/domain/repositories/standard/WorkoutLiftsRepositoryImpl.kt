package com.browntowndev.liftlab.core.domain.repositories.standard

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.persistence.room.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.domain.mapping.WorkoutLiftMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.domain.mapping.WorkoutLiftMappingExtensions.toEntity
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncQueueEntry

class WorkoutLiftsRepositoryImpl (
    private val workoutLiftsDao: WorkoutLiftsDao,
    private val firestoreSyncManager: FirestoreSyncManager,
): Repository {

    suspend fun insert(workoutLift: GenericWorkoutLift): Long {
        val toInsert = workoutLift.toEntity()
        val id = workoutLiftsDao.insert(toInsert)

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
                roomEntityIds = listOf(id),
                SyncType.Upsert,
            )
        )
        
        return id
    }

    suspend fun insertAll(workoutLifts: List<GenericWorkoutLift>): List<Long> {
        var toInsert = workoutLifts.map { it.toEntity() }
        val insertIds = workoutLiftsDao.insertMany(toInsert)

        toInsert = toInsert.zip(insertIds).fastMap { (workoutLift, id) ->
            workoutLift.copy(id = id)
        }

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
                roomEntityIds = toInsert.fastMap { it.id },
                SyncType.Upsert,
            )
        )

        return insertIds
    }

    suspend fun updateLiftId(workoutLiftId: Long, newLiftId: Long) {
        val current = workoutLiftsDao.get(workoutLiftId) ?: return
        val toUpdate = current.copy(liftId = newLiftId).applyFirestoreMetadata(
            firestoreId = current.firestoreId,
            lastUpdated = current.lastUpdated,
            synced = false,
        )
        workoutLiftsDao.update(toUpdate)

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
                roomEntityIds = listOf(toUpdate.id),
                SyncType.Upsert,
            )
        )
    }

    suspend fun update(workoutLift: GenericWorkoutLift) {
        val current = workoutLiftsDao.get(workoutLift.id) ?: return
        val toUpdate = workoutLift.toEntity().applyFirestoreMetadata(
            firestoreId = current.firestoreId,
            lastUpdated = current.lastUpdated,
            synced = false,
        )
        workoutLiftsDao.update(toUpdate)

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
                roomEntityIds = listOf(toUpdate.id),
                SyncType.Upsert,
            )
        )
    }

    suspend fun updateManyAndGetSyncQueueEntry(workoutLifts: List<GenericWorkoutLift>): SyncQueueEntry? {
        val currentEntities = workoutLiftsDao.getMany(workoutLifts.map { it.id }).associateBy { it.id }
        if (currentEntities.isEmpty()) return null

        val toUpdate = workoutLifts.fastMapNotNull { workoutLift ->
            val current = currentEntities[workoutLift.id] ?: return@fastMapNotNull null
            workoutLift.toEntity().applyFirestoreMetadata(
                firestoreId = current.firestoreId,
                lastUpdated = current.lastUpdated,
                synced = false,
            )
        }
        if (toUpdate.isEmpty()) return null
        workoutLiftsDao.updateMany(toUpdate)

        return SyncQueueEntry(
            collectionName = FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
            roomEntityIds = toUpdate.fastMap { it.id },
            SyncType.Upsert,
        )
    }

    suspend fun updateMany(workoutLifts: List<GenericWorkoutLift>) {
        val syncQueueEntry = updateManyAndGetSyncQueueEntry(workoutLifts) ?: return
        firestoreSyncManager.enqueueSyncRequest(syncQueueEntry)
    }

    suspend fun delete(workoutLift: GenericWorkoutLift) {
        val toDelete = workoutLiftsDao.get(workoutLift.id) ?: return
        workoutLiftsDao.delete(toDelete)

        if (toDelete.firestoreId != null) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
                    roomEntityIds = listOf(toDelete.id),
                    SyncType.Delete,
                )
            )
        }
    }

    suspend fun getLiftIdsForWorkout(workoutId: Long): List<Long> {
        return workoutLiftsDao.getLiftIdsForWorkout(workoutId)
    }

    suspend fun getForWorkout(workoutId: Long): List<GenericWorkoutLift> {
        return workoutLiftsDao.getForWorkout(workoutId).map { liftEntity ->
            liftEntity.toDomainModel()
        }
    }
}