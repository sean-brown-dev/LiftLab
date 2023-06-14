package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import com.browntowndev.liftlab.core.persistence.entities.CustomLiftSet

@Dao
interface CustomSetsDao {
    @Insert
    suspend fun insert(customLiftSet: CustomLiftSet): Long

    @Insert
    suspend fun insertAll(customLiftSets: List<CustomLiftSet>): List<Long>
}