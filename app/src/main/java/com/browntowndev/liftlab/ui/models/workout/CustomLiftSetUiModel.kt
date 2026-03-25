package com.browntowndev.liftlab.ui.models.workout

interface CustomLiftSetUiModel {
    val id: Long
    val workoutLiftId: Long
    val position: Int
    val rpeTarget: Float
    val repRangeBottom: Int
    val repRangeTop: Int
}
