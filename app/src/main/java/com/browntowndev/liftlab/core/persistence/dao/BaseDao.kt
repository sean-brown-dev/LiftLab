package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert

@Dao
interface BaseDao<T> {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMany(items: List<T>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: T): Long

    @Upsert
    suspend fun upsert(item: T): Long

    @Transaction
    @Upsert
    suspend fun upsertMany(items: List<T>): List<Long>

    @Update
    suspend fun update(item: T)

    @Transaction
    @Update
    suspend fun updateMany(items: List<T>)

    @Delete
    suspend fun delete(item: T)

    @Transaction
    @Delete
    suspend fun deleteMany(items: List<T>)
}
