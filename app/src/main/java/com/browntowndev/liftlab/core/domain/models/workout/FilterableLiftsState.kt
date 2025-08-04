package com.browntowndev.liftlab.core.domain.models.workout

data class FilterableLiftsState(
    val lifts: List<Lift>,
    val liftIdsForWorkout: HashSet<Long>
)
