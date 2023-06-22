package com.browntowndev.liftlab.core.persistence.repositories

import com.browntowndev.liftlab.core.persistence.dao.CustomSetsDao
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericCustomLiftSet
import com.browntowndev.liftlab.core.persistence.mapping.CustomLiftSetMapper

class CustomLiftSetsRepository(
    private val customLiftCustomSetsDao: CustomSetsDao,
    private val customLiftSetMapper: CustomLiftSetMapper,
): Repository {

    suspend fun insert(newSet: GenericCustomLiftSet) {
        customLiftCustomSetsDao.insert(customLiftSetMapper.map(newSet))
    }

    suspend fun update(set: GenericCustomLiftSet) {
        customLiftCustomSetsDao.update(customLiftSetMapper.map(set))
    }

    suspend fun deleteAllForLift(workoutLiftId: Long) {
        customLiftCustomSetsDao.deleteAllForLift(workoutLiftId)
    }

    suspend fun deleteByPosition(workoutLiftId: Long, position: Int) {
        customLiftCustomSetsDao.deleteByPosition(workoutLiftId, position)
    }

    suspend fun insertAll(customSets: List<GenericCustomLiftSet>): List<Long> {
        return customLiftCustomSetsDao.insertAll(customSets.map { customLiftSetMapper.map(it) })
    }
}