package com.browntowndev.liftlab.core.persistence.repositories

import androidx.room.Transaction
import com.browntowndev.liftlab.core.persistence.dao.CustomSetsDao
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.persistence.mapping.CustomLiftSetMapper

class CustomLiftSetsRepository(
    private val customLiftCustomSetsDao: CustomSetsDao,
    private val customLiftSetMapper: CustomLiftSetMapper,
): Repository {
    suspend fun insert(newSet: GenericLiftSet): Long {
        return customLiftCustomSetsDao.insert(customLiftSetMapper.map(newSet))
    }

    suspend fun update(set: GenericLiftSet) {
        customLiftCustomSetsDao.update(customLiftSetMapper.map(set))
    }

    suspend fun updateMany(sets: List<GenericLiftSet>) {
        customLiftCustomSetsDao.updateMany(sets.map { customLiftSetMapper.map(it) })
    }

    suspend fun deleteAllForLift(workoutLiftId: Long) {
        customLiftCustomSetsDao.deleteAllForLift(workoutLiftId)
    }

    @Transaction
    suspend fun deleteByPosition(workoutLiftId: Long, position: Int) {
        customLiftCustomSetsDao.deleteByPosition(workoutLiftId, position)
        customLiftCustomSetsDao.syncPositions(workoutLiftId, position)
    }

    suspend fun insertAll(customSets: List<GenericLiftSet>): List<Long> {
        return customLiftCustomSetsDao.insertAll(customSets.map { customLiftSetMapper.map(it) })
    }
}