package com.browntowndev.liftlab.ui.models.workout

data class DropSetUiModel (
    override val id: Long = 0,
    override val workoutLiftId: Long,
    override val position: Int,
    override val rpeTarget: Float,
    override val repRangeBottom: Int,
    override val repRangeTop: Int,
    val dropPercentage: Float,
) : CustomLiftSetUiModel