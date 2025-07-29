package com.browntowndev.liftlab.core.data.mapping

import com.browntowndev.liftlab.core.data.local.dtos.WorkoutMetadataDto
import com.browntowndev.liftlab.core.data.mapping.WorkoutLiftMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.domain.models.Workout
import com.browntowndev.liftlab.core.data.local.dtos.WorkoutWithRelationships
import com.browntowndev.liftlab.core.data.local.entities.WorkoutEntity
import com.browntowndev.liftlab.core.data.mapping.WorkoutLiftMappingExtensions.toCalculationDomainModel
import com.browntowndev.liftlab.core.domain.models.metadata.WorkoutMetadata
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationWorkout

object WorkoutMappingExtensions {
    fun WorkoutWithRelationships.toCalculationDomainModel(): CalculationWorkout =
        CalculationWorkout(
            id = this.workoutEntity.id,
            lifts = this.lifts
                .map { it.toCalculationDomainModel() }
                .sortedBy { it.position }
        )

    fun WorkoutWithRelationships.toDomainModel(): Workout =
        Workout(
            id = this.workoutEntity.id,
            programId = this.workoutEntity.programId,
            name = this.workoutEntity.name,
            position = this.workoutEntity.position,
            lifts = this.lifts
                .map { it.toDomainModel() }
                .sortedBy { it.position }
        )

    fun Workout.toEntity(): WorkoutEntity =
        WorkoutEntity(
            id = this.id,
            programId = this.programId,
            name = this.name,
            position = this.position,
        )

    fun WorkoutMetadataDto.toDomainModel(): WorkoutMetadata =
        WorkoutMetadata(
            id = this.id,
            name = this.name,
        )
}