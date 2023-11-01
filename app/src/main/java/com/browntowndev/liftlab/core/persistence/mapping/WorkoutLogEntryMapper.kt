package com.browntowndev.liftlab.core.persistence.mapping

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.persistence.dtos.SetLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.queryable.FlattenedWorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.entities.SetLogEntry

class WorkoutLogEntryMapper {
    fun map(workoutLogEntryId: Long, setLogEntryDto: SetLogEntryDto): SetLogEntry {
        return SetLogEntry(
            workoutLogEntryId = workoutLogEntryId,
            liftId = setLogEntryDto.liftId,
            setType = setLogEntryDto.setType,
            liftPosition = setLogEntryDto.liftPosition,
            setPosition = setLogEntryDto.setPosition,
            myoRepSetPosition = setLogEntryDto.myoRepSetPosition,
            weight = setLogEntryDto.weight,
            reps = setLogEntryDto.reps,
            rpe = setLogEntryDto.rpe,
            mesoCycle = setLogEntryDto.mesoCycle,
            microCycle = setLogEntryDto.microCycle,
        )
    }

    fun map(flattenedResults: List<FlattenedWorkoutLogEntryDto>): List<WorkoutLogEntryDto> {
        return if (flattenedResults.isNotEmpty()) {
            flattenedResults
                .groupBy { it.id }
                .map { workoutLog ->
                    val firstEntry = workoutLog.value[0]
                    WorkoutLogEntryDto(
                        id = firstEntry.id,
                        historicalWorkoutNameId = firstEntry.historicalWorkoutNameId,
                        workoutName = firstEntry.workoutName,
                        programName = firstEntry.programName,
                        microcycle = firstEntry.microCycle,
                        mesocycle = firstEntry.mesoCycle,
                        date = firstEntry.date,
                        durationInMillis = firstEntry.durationInMillis,
                        setResults = workoutLog.value.fastMap {
                            SetLogEntryDto(
                                liftId = it.liftId,
                                liftName = it.liftName,
                                setType = it.setType,
                                liftPosition = it.liftPosition,
                                setPosition = it.setPosition,
                                myoRepSetPosition = it.myoRepSetPosition,
                                weight = it.weight,
                                reps = it.reps,
                                rpe = it.rpe,
                                mesoCycle = it.mesoCycle,
                                microCycle = it.microCycle,
                            )
                        }
                    )
                }
        } else listOf()
    }
}