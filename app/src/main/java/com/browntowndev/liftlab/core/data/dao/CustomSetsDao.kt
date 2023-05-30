package com.browntowndev.liftlab.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import com.browntowndev.liftlab.core.data.entities.CustomLiftSet

@Dao
interface CustomSetsDao {
    @Insert
    suspend fun insert(customLiftSet: CustomLiftSet): Long

    @Insert
    suspend fun insertAll(customLiftSets: List<CustomLiftSet>): List<Long>
}