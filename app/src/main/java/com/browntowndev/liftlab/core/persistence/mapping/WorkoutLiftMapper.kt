package com.browntowndev.liftlab.core.persistence.mapping

import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.dtos.queryable.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLift

class WorkoutLiftMapper(private val customLiftSetMapper: CustomLiftSetMapper) {

    fun map(entity: WorkoutLiftWithRelationships): GenericWorkoutLift {
        return if (entity.customLiftSets.isEmpty()) {
            toStandardWorkoutLiftDto(entity)
        }
        else {
            toCustomWorkoutLiftDto(entity)
        }
    }

    fun map(workoutLiftDto: GenericWorkoutLift): WorkoutLift {
        return when (workoutLiftDto) {
            is StandardWorkoutLiftDto -> toEntity(workoutLiftDto)
            is CustomWorkoutLiftDto -> toEntity(workoutLiftDto)
            else -> throw ClassNotFoundException("${workoutLiftDto::class.simpleName} is not implemented in ${WorkoutLiftMapper::class.simpleName}")
        }
    }

    fun mapToFullEntity(workoutLiftDto: GenericWorkoutLift): WorkoutLift {
        return when (workoutLiftDto) {
            is StandardWorkoutLiftDto -> toEntity(workoutLiftDto)
            is CustomWorkoutLiftDto -> toEntity(workoutLiftDto)
            else -> throw ClassNotFoundException("${workoutLiftDto::class.simpleName} is not implemented in ${WorkoutLiftMapper::class.simpleName}")
        }
    }

    private fun toStandardWorkoutLiftDto(entity: WorkoutLiftWithRelationships): StandardWorkoutLiftDto {
        return StandardWorkoutLiftDto(
            id = entity.workoutLift.id,
            workoutId = entity.workoutLift.workoutId,
            liftId = entity.workoutLift.liftId,
            liftName = entity.lift.name,
            liftMovementPattern = entity.lift.movementPattern,
            deloadWeek = entity.workoutLift.deloadWeek,
            position = entity.workoutLift.position,
            setCount = entity.workoutLift.setCount,
            repRangeBottom = entity.workoutLift.repRangeBottom!!,
            repRangeTop = entity.workoutLift.repRangeTop!!,
            rpeTarget = entity.workoutLift.rpeTarget!!,
            progressionScheme = entity.workoutLift.progressionScheme,
        )
    }

    private fun toCustomWorkoutLiftDto(entity: WorkoutLiftWithRelationships): CustomWorkoutLiftDto {
        return CustomWorkoutLiftDto(
            id = entity.workoutLift.id,
            workoutId = entity.workoutLift.workoutId,
            liftId = entity.workoutLift.liftId,
            liftName = entity.lift.name,
            liftMovementPattern = entity.lift.movementPattern,
            deloadWeek = entity.workoutLift.deloadWeek,
            position = entity.workoutLift.position,
            setCount = entity.workoutLift.setCount,
            progressionScheme = entity.workoutLift.progressionScheme,
            customLiftSets = entity.customLiftSets.map { customLiftSetMapper.map(it) }
        )
    }

    private fun toEntity(dto: StandardWorkoutLiftDto): WorkoutLift {
        return WorkoutLift(
            id = dto.id,
            liftId = dto.liftId,
            workoutId = dto.workoutId,
            position = dto.position,
            setCount = dto.setCount,
            rpeTarget = dto.rpeTarget,
            repRangeBottom = dto.repRangeBottom,
            repRangeTop = dto.repRangeTop,
            progressionScheme = dto.progressionScheme,
        )
    }

    private fun toEntity(dto: CustomWorkoutLiftDto): WorkoutLift {
        return WorkoutLift(
            id = dto.id,
            liftId = dto.liftId,
            workoutId = dto.workoutId,
            position = dto.position,
            setCount = dto.setCount,
            progressionScheme = dto.progressionScheme,
        )
    }
}