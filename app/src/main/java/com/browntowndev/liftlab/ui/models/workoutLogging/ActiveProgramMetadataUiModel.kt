package com.browntowndev.liftlab.ui.models.workoutLogging

data class ActiveProgramMetadataUiModel(
    val programId: Long,
    val name: String,
    val deloadWeek: Int,
    val currentMesocycle: Int,
    val currentMicrocycle: Int,
    val currentMicrocyclePosition: Int,
    val workoutCount: Int,
)
