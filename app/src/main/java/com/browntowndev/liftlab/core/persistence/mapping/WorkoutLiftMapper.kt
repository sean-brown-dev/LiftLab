package com.browntowndev.liftlab.core.persistence.mapping

import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.dtos.queryable.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.update.UpdateWorkoutLift

class WorkoutLiftMapper(private val customLiftSetMapper: CustomLiftSetMapper) {

    fun map(entity: WorkoutLiftWithRelationships): GenericWorkoutLift {
        return if (entity.customLiftSets.isEmpty()) {
            toStandardWorkoutLiftDto(entity)
        }
        else {
            toCustomWorkoutLiftDto(entity)
        }
    }

    fun map(setDto: GenericWorkoutLift): UpdateWorkoutLift {
        return when (setDto) {
            is StandardWorkoutLiftDto -> toUpdateEntity(setDto)
            is CustomWorkoutLiftDto -> toUpdateEntity(setDto)
            else -> throw ClassNotFoundException("${setDto::class.simpleName} is not implemented in ${WorkoutLiftMapper::class.simpleName}")
        }
    }

    private fun toStandardWorkoutLiftDto(entity: WorkoutLiftWithRelationships): StandardWorkoutLiftDto {
        return StandardWorkoutLiftDto(
            id = entity.workoutLift.id,
            liftId = entity.workoutLift.liftId,
            liftName = entity.lift.name,
            liftMovementPattern = entity.lift.movementPattern,
            position = entity.workoutLift.position,
            setCount = entity.workoutLift.setCount,
            repRangeBottom = entity.workoutLift.repRangeBottom!!,
            repRangeTop = entity.workoutLift.repRangeTop!!,
            rpeTarget = entity.workoutLift.rpeTarget!!,
            useReversePyramidSets = entity.workoutLift.useReversePyramidSets,
            progressionScheme = entity.workoutLift.progressionScheme,
        )
    }

    private fun toCustomWorkoutLiftDto(entity: WorkoutLiftWithRelationships): CustomWorkoutLiftDto {
        return CustomWorkoutLiftDto(
            id = entity.workoutLift.id,
            liftId = entity.workoutLift.liftId,
            liftName = entity.lift.name,
            liftMovementPattern = entity.lift.movementPattern,
            position = entity.workoutLift.position,
            setCount = entity.workoutLift.setCount,
            useReversePyramidSets = entity.workoutLift.useReversePyramidSets,
            progressionScheme = entity.workoutLift.progressionScheme,
            customLiftSets = entity.customLiftSets.map { customLiftSetMapper.map(it) }
        )
    }

    private fun toUpdateEntity(setDto: StandardWorkoutLiftDto): UpdateWorkoutLift {
        return UpdateWorkoutLift(
            id = setDto.id,
            liftId = setDto.liftId,
            position = setDto.position,
            setCount = setDto.setCount,
            rpeTarget = setDto.rpeTarget,
            repRangeBottom = setDto.repRangeBottom,
            repRangeTop = setDto.repRangeTop,
            useReversePyramidSets = setDto.useReversePyramidSets,
            progressionScheme = setDto.progressionScheme,
        )
    }

    private fun toUpdateEntity(setDto: CustomWorkoutLiftDto): UpdateWorkoutLift {
        return UpdateWorkoutLift(
            id = setDto.id,
            liftId = setDto.liftId,
            position = setDto.position,
            setCount = setDto.setCount,
            useReversePyramidSets = setDto.useReversePyramidSets,
            progressionScheme = setDto.progressionScheme,
        )
    }
}