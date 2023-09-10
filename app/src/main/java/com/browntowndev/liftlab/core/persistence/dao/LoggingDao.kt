package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.browntowndev.liftlab.core.persistence.dtos.queryable.LiftLogWithRelationships
import com.browntowndev.liftlab.core.persistence.dtos.queryable.WorkoutLogWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.SetLogEntry
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLogEntry

@Dao
interface LoggingDao {
    @Insert
    suspend fun insertMany(setLogEntry: List<SetLogEntry>)

    @Insert
    suspend fun insert(workoutLogEntry: WorkoutLogEntry): Long

    @Query("SELECT * FROM setLogEntries WHERE liftId = :liftId")
    suspend fun getForLift(liftId: Long): List<LiftLogWithRelationships>

    @Query("SELECT * FROM workoutLogEntries WHERE historicalWorkoutNameId = :historicalWorkoutNameId")
    suspend fun getForWorkout(historicalWorkoutNameId: Long): List<WorkoutLogWithRelationships>

    @Query("INSERT INTO setLogEntries " +
            "(workoutLogEntryId, liftId, customSetType, " +
            "setPosition, myoRepSetPosition, weight, " +
            "reps, rpe, mesoCycle, microCycle) " +
            "SELECT :workoutLogEntryId, liftId, setType, " +
            "setPosition, myoRepSetPosition, weight, " +
            "reps, rpe, mesoCycle, microCycle " +
            "FROM previousSetResults")
    suspend fun insertFromPreviousSetResults(workoutLogEntryId: Long)
}