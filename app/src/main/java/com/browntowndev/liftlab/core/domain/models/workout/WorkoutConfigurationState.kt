package com.browntowndev.liftlab.core.domain.models.workout

data class WorkoutConfigurationState(
    val workout: Workout? = null,
    val programDeloadWeek: Int? = null,
    val workoutLiftStepSizeOptions: Map<Long, Map<Int, List<Int>>> = mapOf(),
)
