package com.browntowndev.liftlab.ui.models

class LiftCompletionSummary(
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
) {
    val isIncomplete: Boolean by lazy {
        setsCompleted < totalSets
    }
}