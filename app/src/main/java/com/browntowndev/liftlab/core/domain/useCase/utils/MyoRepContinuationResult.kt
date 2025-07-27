package com.browntowndev.liftlab.core.domain.useCase.utils

data class MyoRepContinuationResult(
    val shouldContinueMyoReps: Boolean,
    val activationSetMissedGoal: Boolean,
)
