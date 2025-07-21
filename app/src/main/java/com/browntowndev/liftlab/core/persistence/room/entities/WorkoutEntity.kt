package com.browntowndev.liftlab.core.persistence.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.annotations.GenerateFirestoreMetadataExtensions

@GenerateFirestoreMetadataExtensions
@Entity(tableName = "workouts",
    indices = [Index("programId"), Index("position")],
    foreignKeys = [ForeignKey(entity = ProgramEntity::class,
        parentColumns = arrayOf("program_id"),
        childColumns = arrayOf("programId"),
        onDelete = ForeignKey.CASCADE)]
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "workout_id")
    val id: Long = 0,
    val programId: Long,
    val name: String,
    val position: Int,
): BaseEntity()
