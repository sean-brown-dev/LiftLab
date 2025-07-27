package com.browntowndev.liftlab.core.domain.models

data class LiftCompletionSummary(
    val liftName: String,
    val liftId: Long,
    val liftPosition: Int,
    val setsCompleted: Int,
    val totalSets: Int,
    val bestSetReps: Int,
    val bestSetWeight: Float,
    val bestSetRpe: Float,
    val bestSet1RM: Int,
    val isNewPersonalRecord: Boolean,
)
