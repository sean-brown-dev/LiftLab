package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift

data class WorkoutDto (
    val id: Long = 0,
    val programId: Long,
    val name: String,
    val position: Int,
    val lifts: List<GenericWorkoutLift>
)