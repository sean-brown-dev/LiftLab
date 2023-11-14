package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

data class EditWorkoutState(
    val duration: String = "00:00:00",
    val setResults: List<SetResult> = listOf(),
)
