package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.browntowndev.liftlab.core.persistence.entities.CustomLiftSet

@Dao
interface CustomSetsDao {
    @Insert
    suspend fun insert(customLiftSet: CustomLiftSet): Long

    @Update
    suspend fun update(customLiftSet: CustomLiftSet)

    @Transaction
    @Insert
    suspend fun insertAll(customLiftSets: List<CustomLiftSet>): List<Long>

    @Transaction
    @Update(entity = CustomLiftSet::class)
    suspend fun updateMany(customLiftSets: List<CustomLiftSet>)

    @Query("DELETE FROM sets WHERE workoutLiftId = :workoutLiftId")
    suspend fun deleteAllForLift(workoutLiftId: Long)

    @Query("DELETE FROM sets WHERE workoutLiftId = :workoutLiftId AND position = :position")
    suspend fun deleteByPosition(workoutLiftId: Long, position: Int)
}