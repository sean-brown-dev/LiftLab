package com.browntowndev.liftlab.core.domain.models.workout

import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift

data class Workout (
    val id: Long = 0,
    val programId: Long,
    val name: String,
    val position: Int,
    val lifts: List<GenericWorkoutLift>
)