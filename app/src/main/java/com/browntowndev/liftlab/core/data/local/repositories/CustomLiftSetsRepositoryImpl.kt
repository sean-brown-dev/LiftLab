package com.browntowndev.liftlab.core.data.local.repositories

import com.browntowndev.liftlab.core.data.local.dao.CustomSetsDao
import com.browntowndev.liftlab.core.data.mapping.toDomainModel
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomLiftSetsRepositoryImpl(
    private val customSetsDao: CustomSetsDao,
): CustomLiftSetsRepository {
    override suspend fun getAll(): List<GenericLiftSet> =
        customSetsDao.getAll().map { it.toDomainModel() }

    override fun getAllFlow(): Flow<List<GenericLiftSet>> =
        customSetsDao.getAllFlow().map { it.map { entity -> entity.toDomainModel() } }

    override suspend fun getById(id: Long): GenericLiftSet? =
        customSetsDao.get(id)?.let { return it.toDomainModel() }

    override suspend fun getMany(ids: List<Long>): List<GenericLiftSet> =
        customSetsDao.getMany(ids).map { it.toDomainModel() }
}