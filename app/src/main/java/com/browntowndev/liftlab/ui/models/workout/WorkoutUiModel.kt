package com.browntowndev.liftlab.ui.models.workout

data class WorkoutUiModel(
    val id: Long = 0,
    val programId: Long,
    val name: String,
    val position: Int,
    val lifts: List<WorkoutLiftUiModel>
)
