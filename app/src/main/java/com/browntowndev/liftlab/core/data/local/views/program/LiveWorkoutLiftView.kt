package com.browntowndev.liftlab.core.data.local.views.program

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme

@DatabaseView("SELECT * FROM workoutLifts WHERE deleted = 0")
data class LiveWorkoutLiftView(
    @ColumnInfo("workout_lift_id")
    val id: Long,
    val workoutId: Long,
    val liftId: Long,
    val progressionScheme: ProgressionScheme,
    val position: Int,
    val setCount: Int,
    val deloadWeek: Int?,
    val rpeTarget: Float?,
    val repRangeBottom: Int?,
    val repRangeTop: Int?,
    val stepSize: Int?,
)
