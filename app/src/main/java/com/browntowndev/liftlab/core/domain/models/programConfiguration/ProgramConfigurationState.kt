package com.browntowndev.liftlab.core.domain.models.programConfiguration

data class ProgramConfigurationState(
    val allPrograms: List<Program> = listOf(),
    val program: Program? = null,
)
