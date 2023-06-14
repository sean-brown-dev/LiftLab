package com.browntowndev.liftlab.core.persistence.mapping

import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.queryable.WorkoutWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.update.UpdateWorkout

class WorkoutMapper(private val workoutLiftMapper: WorkoutLiftMapper) {
    fun map(entity: WorkoutWithRelationships): WorkoutDto {
        return WorkoutDto(
            id = entity.workout.id,
            name = entity.workout.name,
            position = entity.workout.position,
            lifts = entity.lifts.map { workoutLiftMapper.map(it) }
        )
    }

    fun map(setDto: WorkoutDto): UpdateWorkout {
        return UpdateWorkout(
            id = setDto.id,
            name = setDto.name,
            position = setDto.position,
        )
    }
}