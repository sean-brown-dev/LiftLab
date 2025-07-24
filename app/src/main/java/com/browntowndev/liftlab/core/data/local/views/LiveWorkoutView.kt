package com.browntowndev.liftlab.core.data.local.views

import androidx.room.ColumnInfo
import androidx.room.DatabaseView

@DatabaseView("SELECT * FROM workouts WHERE deleted = 0")
data class LiveWorkoutView(
    @ColumnInfo("workout_id")
    val id: Long,
    val programId: Long,
    val name: String,
    val position: Int,
)
