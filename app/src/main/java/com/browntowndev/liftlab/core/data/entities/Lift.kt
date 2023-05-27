package com.browntowndev.liftlab.core.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.core.common.enums.LiftCategory

@Entity(tableName = "lifts")
data class Lift(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("lift_id")
    val id: Long,
    val name: String,
    val category: LiftCategory,
    val isHidden: Boolean
)