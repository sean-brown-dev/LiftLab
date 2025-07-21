package com.browntowndev.liftlab.core.domain.mapping

import com.browntowndev.liftlab.core.domain.models.HistoricalWorkoutName
import com.browntowndev.liftlab.core.persistence.room.entities.HistoricalWorkoutNameEntity

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
