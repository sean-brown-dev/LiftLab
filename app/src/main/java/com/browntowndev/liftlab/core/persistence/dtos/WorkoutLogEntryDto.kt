package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import java.util.Date

data class WorkoutLogEntryDto(
    val mesocycle: Int,
    val microcycle: Int,
    val date: Date,
    val durationInMillis: Long,
    val setResults: List<SetResult>,
)
