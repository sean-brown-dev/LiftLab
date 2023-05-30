package com.browntowndev.liftlab.core.data.dtos

import androidx.room.Embedded
import androidx.room.Relation
import com.browntowndev.liftlab.core.data.entities.HistoricalWorkoutName
import com.browntowndev.liftlab.core.data.entities.SetLogEntry
import com.browntowndev.liftlab.core.data.entities.WorkoutLogEntry

data class LiftLogDto (
    @Embedded
    val entry: SetLogEntry,
    @Relation(parentColumn = "workoutLogEntryId", entityColumn = "workout_log_entry_id", entity = WorkoutLogEntry::class)
    val workoutLogEntry: WorkoutLogNoSetLogDto
)

data class WorkoutLogNoSetLogDto(
    @Embedded
    val entry: WorkoutLogEntry,
    @Relation(parentColumn = "historicalWorkoutNameId", entityColumn = "historical_workout_name_id", entity = HistoricalWorkoutName::class)
    val historicalWorkoutName: HistoricalWorkoutName
)

data class WorkoutLogDto (
    @Embedded
    val entry: WorkoutLogEntry,
    @Relation(parentColumn = "historicalWorkoutNameId", entityColumn = "historical_workout_name_id", entity = HistoricalWorkoutName::class)
    val historicalWorkoutName: HistoricalWorkoutName,
    @Relation(parentColumn = "workout_log_entry_id", entityColumn = "workoutLogEntryId", entity = SetLogEntry::class)
    val setLogEntries: List<SetLogEntry>
)
