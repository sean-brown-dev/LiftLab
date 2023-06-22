package com.browntowndev.liftlab.core.persistence.mapping

import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.queryable.WorkoutWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.Workout

class WorkoutMapper(private val workoutLiftMapper: WorkoutLiftMapper) {
    fun map(entity: WorkoutWithRelationships): WorkoutDto {
        return WorkoutDto(
            id = entity.workout.id,
            programId = entity.workout.programId,
            name = entity.workout.name,
            position = entity.workout.position,
            lifts = entity.lifts.map { workoutLiftMapper.map(it) }
        )
    }

    fun map(workoutDto: WorkoutDto): Workout {
        return Workout(
            id = workoutDto.id,
            programId = workoutDto.programId,
            name = workoutDto.name,
            position = workoutDto.position,
        )
    }
}