package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.data.dtos.ProgramDto

data class LabState(
    val programs: List<ProgramDto> = listOf()
)