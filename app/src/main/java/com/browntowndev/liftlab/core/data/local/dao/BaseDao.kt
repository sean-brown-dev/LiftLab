package com.browntowndev.liftlab.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import androidx.room.Upsert

@Dao
interface BaseDao<T> {
    @Insert
    suspend fun insertMany(items: List<T>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertManyIgnoreConflicts(items: List<T>): List<Long>

    @Insert
    suspend fun insert(item: T): Long

    @Upsert
    suspend fun upsert(item: T): Long

    @Upsert
    suspend fun upsertMany(items: List<T>): List<Long>

    @Update
    suspend fun update(item: T)

    @Update
    suspend fun updateMany(items: List<T>)

    @Delete
    suspend fun delete(item: T): Int

    @Delete
    suspend fun deleteMany(items: List<T>): Int
}
