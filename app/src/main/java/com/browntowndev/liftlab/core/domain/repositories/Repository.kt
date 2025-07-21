package com.browntowndev.liftlab.core.domain.repositories


/**
 * A generic base interface for repositories providing common CRUD operations.
 * @param T The type of the domain model this repository manages.
 * @param K The type of the primary key for the model (e.g., Long, String).
 */
interface Repository<T, K> {
    suspend fun getAll(): List<T>

    suspend fun getById(id: K): T?

    suspend fun getMany(ids: List<K>): List<T>

    suspend fun update(model: T)

    suspend fun updateMany(models: List<T>)

    suspend fun upsert(model: T): K

    suspend fun upsertMany(models: List<T>): List<K>

    suspend fun insert(model: T): K

    suspend fun insertMany(models: List<T>): List<K>

    suspend fun delete(model: T): Int

    suspend fun deleteMany(models: List<T>): Int

    suspend fun deleteById(id: K): Int
}