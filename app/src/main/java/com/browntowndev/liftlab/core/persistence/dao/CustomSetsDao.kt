package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.browntowndev.liftlab.core.persistence.entities.CustomLiftSet
import java.util.Date

@Dao
interface CustomSetsDao: BaseDao<CustomLiftSet> {
    @Query("UPDATE sets SET firestoreId = :firestoreId, lastUpdated = :lastUpdated, synced = 1 WHERE set_id = :id")
    suspend fun updateFirestoreMetadata(id: Long, firestoreId: String, lastUpdated: Date)

    @Transaction
    @Query("SELECT * FROM sets WHERE set_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<CustomLiftSet>

    @Query("SELECT * FROm sets WHERE set_id = :id")
    suspend fun get(id: Long): CustomLiftSet?

    @Transaction
    @Query("SELECT * FROM sets")
    suspend fun getAll(): List<CustomLiftSet>

    @Query("DELETE FROM sets")
    suspend fun deleteAll()

    @Query("DELETE FROM sets WHERE workoutLiftId = :workoutLiftId")
    suspend fun deleteAllForLift(workoutLiftId: Long)

    @Query("DELETE FROM sets WHERE workoutLiftId = :workoutLiftId AND position = :position")
    suspend fun deleteByPosition(workoutLiftId: Long, position: Int)

    @Query("UPDATE sets SET position = position - 1 WHERE workoutLiftId = :workoutLiftId AND position > :afterPosition")
    suspend fun syncPositions(workoutLiftId: Long, afterPosition: Int)

    @Query("SELECT * FROM sets WHERE workoutLiftId = :workoutLiftId")
    suspend fun getByWorkoutLiftId(workoutLiftId: Long): List<CustomLiftSet>
}