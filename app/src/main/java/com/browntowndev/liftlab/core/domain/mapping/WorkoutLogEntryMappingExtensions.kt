package com.browntowndev.liftlab.core.domain.mapping

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.domain.models.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.WorkoutLogEntry
import com.browntowndev.liftlab.core.persistence.room.dtos.FlattenedWorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.room.entities.SetLogEntryEntity
import com.browntowndev.liftlab.core.persistence.room.entities.WorkoutLogEntryEntity

object WorkoutLogEntryMappingExtensions {
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
            microcycle = microcycle,
            mesocycle = mesocycle,
            microcyclePosition = microcyclePosition,
            date = date,
            durationInMillis = durationInMillis,
            setResults = emptyList()
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
                        setResults = workoutLog.value.fastMap {
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
                                mesoCycle = it.mesoCycle,
                                microCycle = it.microCycle,
                                isDeload = it.isDeload,
                            )
                        }
                    )
                }
        } else listOf()
    }
}