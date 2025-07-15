package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.FirebaseConstants
import com.browntowndev.liftlab.core.persistence.dao.CustomSetsDao
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.mapping.CustomLiftSetMapper
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager

class CustomLiftSetsRepository(
    private val customSetsDao: CustomSetsDao,
    private val customLiftSetMapper: CustomLiftSetMapper,
    private val firestoreSyncManager: FirestoreSyncManager,
): Repository {
    suspend fun insert(newSet: GenericLiftSet): Long {
        val entity = customLiftSetMapper.map(newSet)
        val id = customSetsDao.insert(entity)
        firestoreSyncManager.syncSingle(
            collectionName = FirebaseConstants.CUSTOM_LIFT_SETS_COLLECTION,
            entity = entity.toFirebaseDto(),
            onSynced = { firestoreEntity ->
                customSetsDao.update(firestoreEntity.toEntity())
            },
        )
        return id
    }

    suspend fun update(set: GenericLiftSet) {
        val current = customSetsDao.get(set.id)
        val toUpdate = customLiftSetMapper.map(set)
            .copyWithFirestoreMetadata(
                firestoreId = current?.firestoreId,
                lastUpdated = current?.lastUpdated,
                synced = false,
            )
        customSetsDao.update(toUpdate)
        firestoreSyncManager.syncSingle(
            collectionName = FirebaseConstants.CUSTOM_LIFT_SETS_COLLECTION,
            entity = toUpdate.toFirebaseDto(),
            onSynced = { firestoreEntity ->
                customSetsDao.update(firestoreEntity.toEntity())
            },
        )
    }

    suspend fun updateMany(sets: List<GenericLiftSet>) {
        val currentById = customSetsDao.getMany(sets.map { it.id }).associateBy { it.id }
        val toUpdate = sets.fastMap {
            val current = currentById[it.id]
            customLiftSetMapper.map(it)
                .copyWithFirestoreMetadata(
                    firestoreId = current?.firestoreId,
                    lastUpdated = current?.lastUpdated,
                    synced = false,
                )
        }
        customSetsDao.updateMany(toUpdate)

        firestoreSyncManager.syncMany(
            collectionName = FirebaseConstants.CUSTOM_LIFT_SETS_COLLECTION,
            entities = toUpdate.map { it.toFirebaseDto() },
            onSynced = { firebaseEntities ->
                customSetsDao.updateMany(firebaseEntities.map { it.toEntity() })
            },
        )
    }

    suspend fun deleteAllForLift(workoutLiftId: Long) {
        val firestoreIdsToDelete = customSetsDao.getByWorkoutLiftId(workoutLiftId).mapNotNull { it.firestoreId }

        customSetsDao.deleteAllForLift(workoutLiftId)
        firestoreSyncManager.deleteMany(
            collectionName = FirebaseConstants.CUSTOM_LIFT_SETS_COLLECTION,
            firestoreIds = firestoreIdsToDelete,
        )
    }

    suspend fun deleteByPosition(workoutLiftId: Long, position: Int) {
        val firestoreIdToDelete = customSetsDao.getByWorkoutLiftId(workoutLiftId)
            .singleOrNull { it.position == position }?.firestoreId

        customSetsDao.deleteByPosition(workoutLiftId, position)
        if (firestoreIdToDelete != null) {
            firestoreSyncManager.deleteSingle(
                collectionName = FirebaseConstants.CUSTOM_LIFT_SETS_COLLECTION,
                firestoreId = firestoreIdToDelete,
            )
        }

        customSetsDao.syncPositions(workoutLiftId, position)
        val entitiesToUpdate = customSetsDao.getByWorkoutLiftId(workoutLiftId).map { it.toFirebaseDto() }
        firestoreSyncManager.syncMany(
            collectionName = FirebaseConstants.CUSTOM_LIFT_SETS_COLLECTION,
            entities = entitiesToUpdate,
            onSynced = { firebaseEntities ->
                customSetsDao.updateMany(firebaseEntities.map { it.toEntity() })
            },
        )
    }

    suspend fun insertAll(customSets: List<GenericLiftSet>): List<Long> {
        val toInsert = customSets.map { customLiftSetMapper.map(it) }
        val insertedIds = customSetsDao.insertMany(toInsert)
        firestoreSyncManager.syncMany(
            collectionName = FirebaseConstants.CUSTOM_LIFT_SETS_COLLECTION,
            entities = toInsert.map { it.toFirebaseDto() },
            onSynced = { firebaseEntities ->
                customSetsDao.updateMany(firebaseEntities.map { it.toEntity() })
            },
        )

        return insertedIds
    }
}