package com.browntowndev.liftlab.core.domain.mapping

import com.browntowndev.liftlab.core.domain.models.Workout
import com.browntowndev.liftlab.core.persistence.room.dtos.WorkoutWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.room.WorkoutEntity

object WorkoutMappingExtensions {
    fun WorkoutWithRelationships.toDomainModel(): Workout {
        return Workout(
            id = this.workoutEntity.id,
            programId = this.workoutEntity.programId,
            name = this.workoutEntity.name,
            position = this.workoutEntity.position,
            lifts = this.lifts
                .sortedBy { it.workoutLiftEntity.position }
                .map { it.toDomainModel() }
        )
    }

    fun Workout.toEntity(): WorkoutEntity {
        return WorkoutEntity(
            id = this.id,
            programId = this.programId,
            name = this.name,
            position = this.position,
        )
    }
}