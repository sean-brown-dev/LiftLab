package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import com.browntowndev.liftlab.core.persistence.entities.PreviousSetResult

@Dao
interface PreviousSetResultDao {
    @Query("SELECT * FROM previousSetResults WHERE workoutId = :workoutId")
    fun getByWorkoutId(workoutId: Long): List<PreviousSetResult>
}