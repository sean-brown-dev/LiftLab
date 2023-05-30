package com.browntowndev.liftlab.core.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity("workouts",
    indices = [Index("programId")],
    foreignKeys = [ForeignKey(entity = Program::class,
        parentColumns = arrayOf("program_id"),
        childColumns = arrayOf("programId"),
        onDelete = ForeignKey.CASCADE)]
)
data class Workout(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "workout_id")
    val id: Long = 0,
    val programId: Long,
    val name: String,
    val position: Int
)
