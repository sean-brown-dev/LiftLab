package com.browntowndev.liftlab.core.persistence.entities.update

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme

@Entity
data class UpdateWorkoutLift (
    @PrimaryKey(autoGenerate = true) @ColumnInfo("workout_lift_id")
    val id: Long = 0,
    val liftId: Long,
    val position: Int,
    val setCount: Int,
    val rpeTarget: Double? = null,
    val repRangeBottom: Int? = null,
    val repRangeTop: Int? = null,
    val useReversePyramidSets: Boolean = false,
    val progressionScheme: ProgressionScheme
)