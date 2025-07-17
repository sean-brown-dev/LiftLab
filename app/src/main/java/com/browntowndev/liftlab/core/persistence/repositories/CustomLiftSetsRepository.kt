package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.common.fireAndForgetSync
import com.browntowndev.liftlab.core.persistence.dao.CustomSetsDao
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.mapping.CustomLiftSetMapper
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.sync.SyncQueueEntry
import kotlinx.coroutines.CoroutineScope

class CustomLiftSetsRepository(
    private val customSetsDao: CustomSetsDao,
    private val customLiftSetMapper: CustomLiftSetMapper,
    private val firestoreSyncManager: FirestoreSyncManager,
    private val syncScope: CoroutineScope,
): Repository {
    suspend fun insert(newSet: GenericLiftSet): Long {
        val entity = customLiftSetMapper.map(newSet)
        val id = customSetsDao.insert(entity)

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncSingle(
                collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                entity = entity.toFirestoreDto(),
                onSynced = { firestoreEntity ->
                    customSetsDao.update(firestoreEntity.toEntity())
                },
            )
        }
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
        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncSingle(
                collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                entity = toUpdate.toFirestoreDto(),
                onSynced = { firestoreEntity ->
                    customSetsDao.update(firestoreEntity.toEntity())
                },
            )
        }
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
        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncMany(
                collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                entities = toUpdate.map { it.toFirestoreDto() },
                onSynced = { firebaseEntities ->
                    customSetsDao.updateMany(firebaseEntities.map { it.toEntity() })
                },
            )
        }
    }

    suspend fun deleteAllForLift(workoutLiftId: Long) {
        val toDelete = customSetsDao.getByWorkoutLiftId(workoutLiftId)
        if (toDelete.isEmpty()) return

        customSetsDao.deleteMany(toDelete)
        syncScope.fireAndForgetSync {
            firestoreSyncManager.deleteMany(
                collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                firestoreIds = toDelete.mapNotNull { it.firestoreId },
            )
        }
    }

    suspend fun deleteByPosition(workoutLiftId: Long, position: Int) {
        val toDelete = customSetsDao.getByWorkoutLiftId(workoutLiftId)
            .singleOrNull { it.position == position } ?: return

        customSetsDao.delete(toDelete)
        if (toDelete.firestoreId != null) {
            syncScope.fireAndForgetSync {
                firestoreSyncManager.deleteSingle(
                    collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                    firestoreId = toDelete.firestoreId!!,
                )
            }
        }

        customSetsDao.syncPositions(workoutLiftId, position)
        val entitiesToUpdate = customSetsDao.getByWorkoutLiftId(workoutLiftId).map { it.toFirestoreDto() }
        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncMany(
                collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                entities = entitiesToUpdate,
                onSynced = { firebaseEntities ->
                    customSetsDao.updateMany(firebaseEntities.map { it.toEntity() })
                },
            )
        }
    }

    suspend fun insertAll(customSets: List<GenericLiftSet>): List<Long> {
        val toInsert = customSets.map { customLiftSetMapper.map(it) }
        val insertedIds = customSetsDao.insertMany(toInsert)
        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncMany(
                collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                entities = toInsert.map { it.toFirestoreDto() },
                onSynced = { firebaseEntities ->
                    customSetsDao.updateMany(firebaseEntities.map { it.toEntity() })
                },
            )
        }

        return insertedIds
    }
}