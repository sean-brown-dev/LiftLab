package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import androidx.room.Transaction
import com.browntowndev.liftlab.core.persistence.dao.CustomSetsDao
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.mapping.CustomLiftSetMapper

class CustomLiftSetsRepository(
    private val customSetsDao: CustomSetsDao,
    private val customLiftSetMapper: CustomLiftSetMapper,
): Repository {
    suspend fun insert(newSet: GenericLiftSet): Long {
        return customSetsDao.insert(customLiftSetMapper.map(newSet))
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
    }

    @Transaction
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
    }

    suspend fun deleteAllForLift(workoutLiftId: Long) {
        customSetsDao.deleteAllForLift(workoutLiftId)
    }

    @Transaction
    suspend fun deleteByPosition(workoutLiftId: Long, position: Int) {
        customSetsDao.deleteByPosition(workoutLiftId, position)
        customSetsDao.syncPositions(workoutLiftId, position)
    }

    suspend fun insertAll(customSets: List<GenericLiftSet>): List<Long> {
        return customSetsDao.insertMany(customSets.map { customLiftSetMapper.map(it) })
    }
}