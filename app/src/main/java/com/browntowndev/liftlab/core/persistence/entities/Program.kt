package com.browntowndev.liftlab.core.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("programs")
data class Program(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "program_id")
    val id: Long = 0,
    val name: String,
    val isActive: Boolean = true,
    val currentMicrocycle: Int = 0,
    val currentMicrocyclePosition: Int = 0,
    val currentMesocycle: Int = 0
)
