package com.browntowndev.liftlab.core.domain.models.workout

data class LiftConfigurationState(
    val lifts: List<Lift>,
    val liftIdsForWorkout: HashSet<Long>
)
