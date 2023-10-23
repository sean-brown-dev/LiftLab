package com.browntowndev.liftlab.core.persistence.mapping

import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.dtos.queryable.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLift

class WorkoutLiftMapper(private val customLiftSetMapper: CustomLiftSetMapper)  {

    fun map(from: WorkoutLiftWithRelationships): GenericWorkoutLift {
        return if (from.customLiftSets.isEmpty()) {
            toStandardWorkoutLiftDto(from)
        }
        else {
            toCustomWorkoutLiftDto(from)
        }
    }

    fun map(from: GenericWorkoutLift): WorkoutLift {
        return when (from) {
            is StandardWorkoutLiftDto -> toEntity(from)
            is CustomWorkoutLiftDto -> toEntity(from)
            else -> throw ClassNotFoundException("${from::class.simpleName} is not implemented in ${WorkoutLiftMapper::class.simpleName}")
        }
    }

    private fun toStandardWorkoutLiftDto(entity: WorkoutLiftWithRelationships): StandardWorkoutLiftDto {
        return StandardWorkoutLiftDto(
            id = entity.workoutLift.id,
            workoutId = entity.workoutLift.workoutId,
            liftId = entity.workoutLift.liftId,
            liftName = entity.lift.name,
            liftMovementPattern = entity.lift.movementPattern,
            liftVolumeTypes = entity.lift.volumeTypesBitmask,
            liftSecondaryVolumeTypes = entity.lift.secondaryVolumeTypesBitmask,
            deloadWeek = entity.workoutLift.deloadWeek,
            incrementOverride = entity.lift.incrementOverride,
            restTime = entity.lift.restTime,
            restTimerEnabled = entity.lift.restTimerEnabled,
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
            liftVolumeTypes = entity.lift.volumeTypesBitmask,
            liftSecondaryVolumeTypes = entity.lift.secondaryVolumeTypesBitmask,
            deloadWeek = entity.workoutLift.deloadWeek,
            incrementOverride = entity.lift.incrementOverride,
            restTime = entity.lift.restTime,
            restTimerEnabled = entity.lift.restTimerEnabled,
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
            deloadWeek = dto.deloadWeek,
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
            deloadWeek = dto.deloadWeek,
            position = dto.position,
            setCount = dto.setCount,
            progressionScheme = dto.progressionScheme,
        )
    }
}