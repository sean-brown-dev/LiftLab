package com.browntowndev.liftlab.core.persistence.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.annotations.GenerateFirestoreMetadataExtensions
import java.util.Date

@GenerateFirestoreMetadataExtensions
@Entity("workoutLogEntries",
    indices = [Index("historicalWorkoutNameId")],
    foreignKeys = [
        ForeignKey(
            entity = HistoricalWorkoutNameEntity::class,
            parentColumns = arrayOf("historical_workout_name_id"),
            childColumns = arrayOf("historicalWorkoutNameId"),
            onDelete = ForeignKey.RESTRICT
        )])
data class WorkoutLogEntryEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "workout_log_entry_id")
    val id: Long = 0,
    val historicalWorkoutNameId: Long,
    val programWorkoutCount: Int,
    val programDeloadWeek: Int,
    val mesocycle: Int,
    val microcycle: Int,
    val microcyclePosition: Int,
    val date: Date,
    val durationInMillis: Long,
): BaseEntity()
