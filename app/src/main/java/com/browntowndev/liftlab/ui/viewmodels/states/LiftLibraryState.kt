package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.data.dtos.LiftDto

data class LiftLibraryState (
    val lifts: List<LiftDto> = listOf(),
)