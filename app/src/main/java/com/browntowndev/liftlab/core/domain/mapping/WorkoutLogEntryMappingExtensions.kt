package com.browntowndev.liftlab.core.domain.mapping

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.domain.models.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.WorkoutLogEntry
import com.browntowndev.liftlab.core.persistence.room.dtos.FlattenedWorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.room.entities.SetLogEntryEntity

object WorkoutLogEntryMappingExtensions {
    fun SetLogEntry.toEntity(workoutLogEntryId: Long): SetLogEntryEntity {
        return SetLogEntryEntity(
            id = this.id,
            workoutLogEntryId = workoutLogEntryId,
            liftId = this.liftId,
            liftName = this.liftName,
            liftMovementPattern = this.liftMovementPattern,
            progressionScheme = this.progressionScheme,
            setType = this.setType,
            liftPosition = this.liftPosition,
            setPosition = this.setPosition,
            myoRepSetPosition = this.myoRepSetPosition,
            weightRecommendation = this.weightRecommendation,
            weight = this.weight,
            reps = this.reps,
            rpe = this.rpe,
            mesoCycle = this.mesoCycle,
            microCycle = this.microCycle,
            repRangeBottom = this.repRangeBottom,
            repRangeTop = this.repRangeTop,
            rpeTarget = this.rpeTarget,
            repFloor = this.repFloor,
            maxSets = this.maxSets,
            setMatching = this.setMatching,
            oneRepMax = this.oneRepMax,
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
                                id = it.setLogEntryId!!,
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