package com.browntowndev.liftlab.core.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.core.common.enums.LiftCategory

@Entity(tableName = "lifts")
data class Lift(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val category: LiftCategory
)