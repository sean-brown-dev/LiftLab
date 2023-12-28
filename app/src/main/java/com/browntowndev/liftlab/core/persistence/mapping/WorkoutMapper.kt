package com.browntowndev.liftlab.core.persistence.mapping

import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.queryable.WorkoutWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.Workout

class WorkoutMapper(private val workoutLiftMapper: WorkoutLiftMapper) {
    fun map(from: WorkoutWithRelationships): WorkoutDto {
        return WorkoutDto(
            id = from.workout.id,
            programId = from.workout.programId,
            name = from.workout.name,
            position = from.workout.position,
            lifts = from.lifts
                .sortedBy { it.workoutLift.position }
                .map { workoutLiftMapper.map(it) }
        )
    }

    fun map(from: WorkoutDto): Workout {
        return Workout(
            id = from.id,
            programId = from.programId,
            name = from.name,
            position = from.position,
        )
    }
}