package com.browntowndev.liftlab.core.persistence.dtos

data class OneRepMaxResultDto(
    val setsAndRepsLabel: String,
    val date: String,
    val oneRepMax: Int,
)
