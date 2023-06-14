package com.browntowndev.liftlab.core.persistence.dtos.queryable

import androidx.room.Embedded
import androidx.room.Relation
import com.browntowndev.liftlab.core.persistence.entities.HistoricalWorkoutName
import com.browntowndev.liftlab.core.persistence.entities.SetLogEntry
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLogEntry

data class LiftLogWithRelationships (
    @Embedded
    val entry: SetLogEntry,
    @Relation(parentColumn = "workoutLogEntryId", entityColumn = "workout_log_entry_id", entity = WorkoutLogEntry::class)
    val workoutLogEntry: WorkoutLogNoSetLogWithRelationships
)

data class WorkoutLogNoSetLogWithRelationships(
    @Embedded
    val entry: WorkoutLogEntry,
    @Relation(parentColumn = "historicalWorkoutNameId", entityColumn = "historical_workout_name_id", entity = HistoricalWorkoutName::class)
    val historicalWorkoutName: HistoricalWorkoutName
)

data class WorkoutLogWithRelationships (
    @Embedded
    val entry: WorkoutLogEntry,
    @Relation(parentColumn = "historicalWorkoutNameId", entityColumn = "historical_workout_name_id", entity = HistoricalWorkoutName::class)
    val historicalWorkoutName: HistoricalWorkoutName,
    @Relation(parentColumn = "workout_log_entry_id", entityColumn = "workoutLogEntryId", entity = SetLogEntry::class)
    val setLogEntries: List<SetLogEntry>
)
