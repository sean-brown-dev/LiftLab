package com.browntowndev.liftlab.core.data.mapping

import com.browntowndev.liftlab.core.data.mapping.WorkoutLiftMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.domain.models.Workout
import com.browntowndev.liftlab.core.data.local.dtos.WorkoutWithRelationships
import com.browntowndev.liftlab.core.data.local.entities.WorkoutEntity

object WorkoutMappingExtensions {
    fun WorkoutWithRelationships.toDomainModel(): Workout {
        return Workout(
            id = this.workoutEntity.id,
            programId = this.workoutEntity.programId,
            name = this.workoutEntity.name,
            position = this.workoutEntity.position,
            lifts = this.lifts
                .map { it.toDomainModel() }
                .sortedBy { it.position }
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