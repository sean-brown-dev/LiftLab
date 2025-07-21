package com.browntowndev.liftlab.core.persistence.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.persistence.entities.room.CustomLiftSetEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface CustomSetsDao: BaseDao<CustomLiftSetEntity> {
    @Query("UPDATE sets SET firestoreId = :firestoreId, lastUpdated = :lastUpdated, synced = 1 WHERE set_id = :id")
    suspend fun updateFirestoreMetadata(id: Long, firestoreId: String, lastUpdated: Date)

    @Transaction
    @Query("SELECT * FROM sets WHERE set_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<CustomLiftSetEntity>

    @Query("SELECT * FROm sets WHERE set_id = :id")
    suspend fun get(id: Long): CustomLiftSetEntity?

    @Transaction
    @Query("SELECT * FROM sets")
    suspend fun getAll(): List<CustomLiftSetEntity>

    @Transaction
    @Query("SELECT * FROM sets")
    fun getAllFlow(): Flow<List<CustomLiftSetEntity>>

    @Query("DELETE FROM sets")
    suspend fun deleteAll()

    @Query("UPDATE sets SET position = position - 1 WHERE workoutLiftId = :workoutLiftId AND position > :afterPosition")
    suspend fun syncPositions(workoutLiftId: Long, afterPosition: Int)

    @Query("SELECT * FROM sets WHERE workoutLiftId = :workoutLiftId")
    suspend fun getByWorkoutLiftId(workoutLiftId: Long): List<CustomLiftSetEntity>
}