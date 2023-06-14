package com.browntowndev.liftlab.core.persistence.entities.update

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.core.common.enums.SetType

@Entity
data class UpdateCustomLiftSet(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("set_id")
    val id: Long = 0,
    val type: SetType,
    val position: Int,
    val repFloor: Int? = null,
    val rpeTarget: Double? = null,
    val repRangeBottom: Int? = null,
    val repRangeTop: Int? = null,
    val dropPercentage: Double? = null,
    val maxSets: Int? = null,
    val setMatching: Boolean = false,
)
