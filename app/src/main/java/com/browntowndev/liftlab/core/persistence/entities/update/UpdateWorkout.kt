package com.browntowndev.liftlab.core.persistence.entities.update

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class UpdateWorkout(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "workout_id")
    val id: Long = 0,
    val name: String,
    val position: Int,
)