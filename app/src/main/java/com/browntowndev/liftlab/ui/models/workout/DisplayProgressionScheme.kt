package com.browntowndev.liftlab.ui.models.workout

data class DisplayProgressionScheme(
    val name: String,
    val shortName: String,
    val isLinearProgression: Boolean,
    val canHaveCustomSets: Boolean,
    val rpeLabel: String
)
