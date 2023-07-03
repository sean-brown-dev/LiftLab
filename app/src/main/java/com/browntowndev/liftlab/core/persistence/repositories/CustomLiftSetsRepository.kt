package com.browntowndev.liftlab.core.persistence.repositories

import androidx.room.Transaction
import com.browntowndev.liftlab.core.persistence.dao.CustomSetsDao
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericCustomLiftSet
import com.browntowndev.liftlab.core.persistence.mapping.CustomLiftSetMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CustomLiftSetsRepository(
    private val customLiftCustomSetsDao: CustomSetsDao,
    private val customLiftSetMapper: CustomLiftSetMapper,
): Repository {
    private val _dataFlow =
        MutableStateFlow<Map<Long, List<GenericCustomLiftSet>>>(mapOf())
    val dataFlow = _dataFlow.asStateFlow()

    suspend fun insert(newSet: GenericCustomLiftSet) {
        customLiftCustomSetsDao.insert(customLiftSetMapper.map(newSet))
    }

    suspend fun update(set: GenericCustomLiftSet) {
        customLiftCustomSetsDao.update(customLiftSetMapper.map(set))
    }

    suspend fun deleteAllForLift(workoutLiftId: Long) {
        customLiftCustomSetsDao.deleteAllForLift(workoutLiftId)

        if (_dataFlow.value.contains(workoutLiftId)) {
            _dataFlow.update {
                val mapCopy = it.toMutableMap()
                mapCopy.remove(workoutLiftId)
                mapCopy
            }
        }
    }

    @Transaction
    suspend fun deleteByPosition(workoutLiftId: Long, position: Int) {
        customLiftCustomSetsDao.deleteByPosition(workoutLiftId, position)
        customLiftCustomSetsDao.syncPositions(workoutLiftId, position)

        // TODO: Update the flow
    }

    suspend fun insertAll(customSets: List<GenericCustomLiftSet>): List<Long> {
        return customLiftCustomSetsDao.insertAll(customSets.map { customLiftSetMapper.map(it) })

        // TODO: Update the flow
    }
}