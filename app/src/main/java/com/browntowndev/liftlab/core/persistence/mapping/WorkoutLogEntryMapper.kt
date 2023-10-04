package com.browntowndev.liftlab.core.persistence.mapping

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.queryable.FlattenedWorkoutLogEntries
import com.browntowndev.liftlab.core.persistence.entities.PreviousSetResult

class WorkoutLogEntryMapper(private val setResultMapper: SetResultMapper) {
    fun map(flattenedResults: List<FlattenedWorkoutLogEntries>): List<WorkoutLogEntryDto>? {
        return if (flattenedResults.isNotEmpty()) {
            flattenedResults
                .groupBy { it.historicalWorkoutNameId }
                .map { workoutLog ->
                    val firstEntry = workoutLog.value[0]
                    WorkoutLogEntryDto(
                        microcycle = firstEntry.microCycle,
                        mesocycle = firstEntry.mesoCycle,
                        date = firstEntry.date,
                        durationInMillis = firstEntry.durationInMillis,
                        setResults = workoutLog.value.fastMap {
                            setResultMapper.map(
                                PreviousSetResult(
                                    id = 0L,
                                    workoutId = 0L,
                                    liftId = 0L,
                                    setType = it.setType,
                                    setPosition = it.setPosition,
                                    myoRepSetPosition = it.myoRepSetPosition,
                                    weight = it.weight,
                                    reps = it.reps,
                                    rpe = it.rpe,
                                    mesoCycle = it.mesoCycle,
                                    microCycle = it.microCycle,
                                    missedLpGoals = it.missedLpGoals,
                                )
                            )
                        }
                    )
                }
        } else null
    }
}