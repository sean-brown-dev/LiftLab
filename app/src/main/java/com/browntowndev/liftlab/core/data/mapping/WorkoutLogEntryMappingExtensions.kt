package com.browntowndev.liftlab.core.data.mapping

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.local.dtos.FlattenedWorkoutLogEntryDto
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLogEntryEntity
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry

fun WorkoutLogEntry.toEntity(): WorkoutLogEntryEntity {
    return WorkoutLogEntryEntity(
        id = id,
        historicalWorkoutNameId = historicalWorkoutNameId,
        programDeloadWeek = programDeloadWeek,
        programWorkoutCount = programWorkoutCount,
        microCycle = microcycle,
        mesoCycle = mesocycle,
        microcyclePosition = microcyclePosition,
        date = date,
        durationInMillis = durationInMillis
    )
}

fun WorkoutLogEntryEntity.toDomainModel(): WorkoutLogEntry {
    return WorkoutLogEntry(
        id = id,
        programId = 0,
        workoutId = 0,
        historicalWorkoutNameId = historicalWorkoutNameId,
        workoutName = "NOT LOADED",
        programName = "NOT LOADED",
        programDeloadWeek = programDeloadWeek,
        programWorkoutCount = programWorkoutCount,
        microcycle = microCycle,
        mesocycle = mesoCycle,
        microcyclePosition = microcyclePosition,
        date = date,
        durationInMillis = durationInMillis,
        setLogEntries = emptyList()
    )
}

fun List<FlattenedWorkoutLogEntryDto>.toDomainModel(): List<WorkoutLogEntry> {
    return if (this.isNotEmpty()) {
        this
            .groupBy { it.id }
            .map { workoutLog ->
                val firstEntry = workoutLog.value[0]
                WorkoutLogEntry(
                    id = firstEntry.id,
                    programId = firstEntry.programId,
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
                    setLogEntries = workoutLog.value.fastMap {
                        SetLogEntry(
                            id = it.setLogEntryId,
                            workoutLogEntryId = it.id,
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
                            isDeload = it.isDeload,
                        )
                    }
                )
            }
    } else listOf()
}
