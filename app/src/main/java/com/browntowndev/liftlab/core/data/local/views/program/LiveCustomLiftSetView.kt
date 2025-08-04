package com.browntowndev.liftlab.core.data.local.views.program

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import com.browntowndev.liftlab.core.domain.enums.SetType

@DatabaseView("SELECT * FROM sets WHERE deleted = 0")
data class LiveCustomLiftSetView(
    @ColumnInfo("set_id")
    val id: Long,
    val workoutLiftId: Long,
    val type: SetType,
    val position: Int,
    val rpeTarget: Float,
    val repRangeBottom: Int,
    val repRangeTop: Int,
    val setGoal: Int?,
    val repFloor: Int?,
    val dropPercentage: Float?,
    val maxSets: Int?,
    val setMatching: Boolean,
)
