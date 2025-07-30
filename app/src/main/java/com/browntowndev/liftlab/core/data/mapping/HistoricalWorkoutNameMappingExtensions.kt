package com.browntowndev.liftlab.core.data.mapping

import com.browntowndev.liftlab.core.domain.models.workoutLogging.HistoricalWorkoutName
import com.browntowndev.liftlab.core.data.local.entities.HistoricalWorkoutNameEntity

object HistoricalWorkoutNameMappingExtensions {
    fun HistoricalWorkoutNameEntity.toDomainModel(): HistoricalWorkoutName {
        return HistoricalWorkoutName(
            id = this.id,
            programId = this.programId,
            workoutId = this.workoutId,
            programName = this.programName,
            workoutName = this.workoutName
        )
    }

    fun HistoricalWorkoutName.toEntity(): HistoricalWorkoutNameEntity {
        return HistoricalWorkoutNameEntity(
            id = this.id,
            programId = this.programId,
            workoutId = this.workoutId,
            programName = this.programName,
            workoutName = this.workoutName
        )
    }
}
