package com.browntowndev.liftlab.core.data.local.views.program

import androidx.room.ColumnInfo
import androidx.room.DatabaseView

@DatabaseView("SELECT * FROM programs WHERE deleted = 0")
data class LiveProgramView(
    @ColumnInfo("program_id")
    val id: Long,
    val name: String,
    val deloadWeek: Int,
    val isActive: Boolean,
    val currentMicrocycle: Int,
    val currentMicrocyclePosition: Int,
    val currentMesocycle: Int,
)
