package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.fireAndForgetSync
import com.browntowndev.liftlab.core.persistence.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLift
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLiftMapper
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager
import kotlinx.coroutines.CoroutineScope

class WorkoutLiftsRepository (
    private val workoutLiftsDao: WorkoutLiftsDao,
    private val workoutLiftMapper: WorkoutLiftMapper,
    private val firestoreSyncManager: FirestoreSyncManager,
    private val syncScope: CoroutineScope,
): Repository {

    suspend fun getAll(): List<WorkoutLift> {
        return workoutLiftsDao.getAll()
    }

    suspend fun insert(workoutLift: GenericWorkoutLift): Long {
        val toInsert = workoutLiftMapper.map(workoutLift)
        val id = workoutLiftsDao.insert(toInsert)

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncSingle(
                collectionName = FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
                entity = toInsert.toFirestoreDto().copy(id = id),
                onSynced = {
                    workoutLiftsDao.update(it.toEntity())
                }
            )
        }
        
        return id
    }

    suspend fun insertAll(workoutLifts: List<GenericWorkoutLift>): List<Long> {
        var toInsert = workoutLifts.map { workoutLiftMapper.map(it) }
        val insertIds = workoutLiftsDao.insertMany(toInsert)

        toInsert = toInsert.zip(insertIds).fastMap { (workoutLift, id) ->
            workoutLift.copy(id = id)
        }

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncMany(
                collectionName = FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
                entities = toInsert.map { it.toFirestoreDto() },
                onSynced = { firestoreEntities ->
                    workoutLiftsDao.updateMany(firestoreEntities.map { it.toEntity() })
                }
            )
        }

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

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncSingle(
                collectionName = FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
                entity = toUpdate.toFirestoreDto(),
                onSynced = {
                    workoutLiftsDao.update(it.toEntity())
                }
            )
        }
    }

    suspend fun update(workoutLift: GenericWorkoutLift) {
        val current = workoutLiftsDao.get(workoutLift.id) ?: return
        val toUpdate = workoutLiftMapper.map(workoutLift).applyFirestoreMetadata(
            firestoreId = current.firestoreId,
            lastUpdated = current.lastUpdated,
            synced = false,
        )
        workoutLiftsDao.update(toUpdate)

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncSingle(
                collectionName = FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
                entity = toUpdate.toFirestoreDto(),
                onSynced = {
                    workoutLiftsDao.update(it.toEntity())
                }
            )
        }
    }

    suspend fun updateMany(workoutLifts: List<GenericWorkoutLift>) {
        val currentEntities = workoutLiftsDao.getMany(workoutLifts.map { it.id }).associateBy { it.id }
        if (currentEntities.isEmpty()) return

        val toUpdate = workoutLifts.fastMapNotNull { workoutLift ->
            val current = currentEntities[workoutLift.id] ?: return@fastMapNotNull null
            workoutLiftMapper.map(workoutLift).applyFirestoreMetadata(
                firestoreId = current.firestoreId,
                lastUpdated = current.lastUpdated,
                synced = false,
            )
        }
        if (toUpdate.isEmpty()) return
        workoutLiftsDao.updateMany(toUpdate)

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncMany(
                collectionName = FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
                entities = toUpdate.map { it.toFirestoreDto() },
                onSynced = { firestoreEntities ->
                    workoutLiftsDao.updateMany(firestoreEntities.map { it.toEntity() })
                }
            )
        }
    }

    suspend fun delete(workoutLift: GenericWorkoutLift) {
        val toDelete = workoutLiftsDao.get(workoutLift.id) ?: return
        workoutLiftsDao.delete(toDelete)

        if (toDelete.firestoreId != null) {
            syncScope.fireAndForgetSync {
                firestoreSyncManager.deleteSingle(
                    collectionName = FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
                    firestoreId = toDelete.firestoreId!!,
                )
            }
        }
    }

    suspend fun getLiftIdsForWorkout(workoutId: Long): List<Long> {
        return workoutLiftsDao.getLiftIdsForWorkout(workoutId)
    }

    suspend fun getForWorkout(workoutId: Long): List<GenericWorkoutLift> {
        return workoutLiftsDao.getForWorkout(workoutId).map { liftEntity ->
            workoutLiftMapper.map(liftEntity)
        }
    }
}