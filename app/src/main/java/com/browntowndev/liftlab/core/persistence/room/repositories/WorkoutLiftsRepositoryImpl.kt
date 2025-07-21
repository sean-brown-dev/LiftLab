package com.browntowndev.liftlab.core.persistence.room.repositories

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.persistence.room.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.domain.mapping.WorkoutLiftMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.domain.mapping.WorkoutLiftMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncQueueEntry

class WorkoutLiftsRepositoryImpl (
    private val workoutLiftsDao: WorkoutLiftsDao,
    private val firestoreSyncManager: FirestoreSyncManager,
): WorkoutLiftsRepository {

    override suspend fun insert(model: GenericWorkoutLift): Long {
        val toInsert = model.toEntity()
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

    override suspend fun insertMany(models: List<GenericWorkoutLift>): List<Long> {
        var toInsert = models.map { it.toEntity() }
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

    override suspend fun updateLiftId(workoutLiftId: Long, newLiftId: Long) {
        val current = workoutLiftsDao.getWithoutRelationships(workoutLiftId) ?: return
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

    override suspend fun getAll(): List<GenericWorkoutLift> {
        return workoutLiftsDao.getAll().map { it.toDomainModel() }
    }

    override suspend fun getById(id: Long): GenericWorkoutLift? {
        return workoutLiftsDao.get(id)?.toDomainModel()
    }

    override suspend fun getMany(ids: List<Long>): List<GenericWorkoutLift> {
        return workoutLiftsDao.getMany(ids).map { it.toDomainModel() }
    }

    override suspend fun update(model: GenericWorkoutLift) {
        val current = workoutLiftsDao.getWithoutRelationships(model.id) ?: return
        val toUpdate = model.toEntity().applyFirestoreMetadata(
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
        val currentEntities = workoutLiftsDao.getManyWithoutRelationships(workoutLifts.map { it.id }).associateBy { it.id }
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

    override suspend fun updateMany(models: List<GenericWorkoutLift>) {
        val syncQueueEntry = updateManyAndGetSyncQueueEntry(models) ?: return
        firestoreSyncManager.enqueueSyncRequest(syncQueueEntry)
    }

    override suspend fun upsert(model: GenericWorkoutLift): Long {
        val current = workoutLiftsDao.getWithoutRelationships(model.id)
        val toUpsert = model.toEntity().applyFirestoreMetadata(
            firestoreId = current?.firestoreId,
            lastUpdated = current?.lastUpdated,
            synced = false,
        )
        val id = workoutLiftsDao.upsert(toUpsert)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
                roomEntityIds = listOf(if (id == -1L) toUpsert.id else id),
                SyncType.Upsert,
            )
        )
        return if (id == -1L) toUpsert.id else id
    }

    override suspend fun upsertMany(models: List<GenericWorkoutLift>): List<Long> {
        val currentEntities = workoutLiftsDao.getManyWithoutRelationships(models.map { it.id }).associateBy { it.id }
        val toUpsert = models.map { workoutLift ->
            val current = currentEntities[workoutLift.id]
            workoutLift.toEntity().applyFirestoreMetadata(
                firestoreId = current?.firestoreId,
                lastUpdated = current?.lastUpdated,
                synced = false,
            )
        }
        val ids = workoutLiftsDao.upsertMany(toUpsert)
        val entityIds = toUpsert.zip(ids).map { (entity, returnedId) ->
            if (returnedId == -1L) entity else entity.copy(id = returnedId)
        }.fastMap { it.id }

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
                roomEntityIds = entityIds,
                SyncType.Upsert
            )
        )
        return entityIds
    }

    override suspend fun delete(model: GenericWorkoutLift): Int {
        val toDelete = workoutLiftsDao.getWithoutRelationships(model.id) ?: return 0
        val count = workoutLiftsDao.delete(toDelete)

        if (count > 0 && toDelete.firestoreId != null) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
                    roomEntityIds = listOf(toDelete.id),
                    SyncType.Delete,
                )
            )
        }
        return count
    }

    override suspend fun deleteMany(models: List<GenericWorkoutLift>): Int {
        val toDelete = models.map { it.toEntity() }
        val count = workoutLiftsDao.deleteMany(toDelete)
        val firestoreIds = toDelete.mapNotNull { it.firestoreId }
        if (firestoreIds.isNotEmpty() && count > 0) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
                    roomEntityIds = toDelete.map { it.id },
                    SyncType.Delete
                )
            )
        }
        return count
    }

    override suspend fun deleteById(id: Long): Int {
        val toDelete = workoutLiftsDao.get(id) ?: return 0
        return delete(toDelete.toDomainModel())
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