package com.browntowndev.liftlab.ui.viewmodels.liftLibrary

data class LiftMerge(
    val liftToMergeInto: Long,
    val liftsToMerge: List<Long>
)
