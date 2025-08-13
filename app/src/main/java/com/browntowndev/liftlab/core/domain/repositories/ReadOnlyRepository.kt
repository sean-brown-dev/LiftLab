package com.browntowndev.liftlab.core.domain.repositories

import kotlinx.coroutines.flow.Flow

interface ReadOnlyRepository<T, K> {
    suspend fun getAll(): List<T>

    fun getAllFlow(): Flow<List<T>>

    suspend fun getById(id: K): T?

    suspend fun getMany(ids: List<K>): List<T>
}