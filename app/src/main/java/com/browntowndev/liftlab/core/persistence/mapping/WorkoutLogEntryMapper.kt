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
            liftName = setLogEntryDto.liftName,
            liftMovementPattern = setLogEntryDto.liftMovementPattern,
            progressionScheme = setLogEntryDto.progressionScheme,
            setType = setLogEntryDto.setType,
            liftPosition = setLogEntryDto.liftPosition,
            setPosition = setLogEntryDto.setPosition,
            myoRepSetPosition = setLogEntryDto.myoRepSetPosition,
            weightRecommendation = setLogEntryDto.weightRecommendation,
            weight = setLogEntryDto.weight,
            reps = setLogEntryDto.reps,
            rpe = setLogEntryDto.rpe,
            mesoCycle = setLogEntryDto.mesoCycle,
            microCycle = setLogEntryDto.microCycle,
            repRangeBottom = setLogEntryDto.repRangeBottom,
            repRangeTop = setLogEntryDto.repRangeTop,
            rpeTarget = setLogEntryDto.rpeTarget,
            repFloor = setLogEntryDto.repFloor,
            maxSets = setLogEntryDto.maxSets,
            setMatching = setLogEntryDto.setMatching,
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
                        workoutId = firstEntry.workoutId,
                        historicalWorkoutNameId = firstEntry.historicalWorkoutNameId,
                        workoutName = firstEntry.workoutName,
                        programName = firstEntry.programName,
                        programDeloadWeek = firstEntry.programDeloadWeek,
                        programWorkoutCount = firstEntry.programWorkoutCount,
                        microcycle = firstEntry.microCycle,
                        mesocycle = firstEntry.mesoCycle,
                        microcyclePosition = firstEntry.microcyclePosition,
                        date = firstEntry.date,
                        durationInMillis = firstEntry.durationInMillis,
                        setResults = workoutLog.value.fastMap {
                            SetLogEntryDto(
                                liftId = it.liftId,
                                liftName = it.liftName,
                                liftMovementPattern = it.liftMovementPattern,
                                progressionScheme = it.progressionScheme,
                                setType = it.setType,
                                liftPosition = it.liftPosition,
                                setPosition = it.setPosition,
                                myoRepSetPosition = it.myoRepSetPosition,
                                repRangeBottom = it.repRangeBottom,
                                repRangeTop = it.repRangeTop,
                                rpeTarget = it.rpeTarget,
                                repFloor = it.repFloor,
                                maxSets = it.maxSets,
                                setMatching = it.setMatching,
                                weightRecommendation = it.weightRecommendation,
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