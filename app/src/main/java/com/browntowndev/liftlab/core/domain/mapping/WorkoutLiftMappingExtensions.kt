package com.browntowndev.liftlab.core.domain.mapping

import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.mapping.CustomLiftSetMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.domain.models.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.room.dtos.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.persistence.room.entities.WorkoutLiftEntity

object WorkoutLiftMappingExtensions {
    fun WorkoutLiftWithRelationships.toDomainModel(): GenericWorkoutLift {
        return if (customLiftSetEntities.isEmpty()) {
            toStandardWorkoutLift()
        }
        else {
            toCustomWorkoutLift()
        }
    }

    fun WorkoutLiftEntity.toDomainModel(): GenericWorkoutLift {
        return if (this.repRangeTop != null && this.repRangeBottom != null && this.rpeTarget != null) {
            toStandardWorkoutLift()
        } else {
            toCustomWorkoutLift()
        }
    }

    fun GenericWorkoutLift.toEntity(): WorkoutLiftEntity {
        return when (this) {
            is StandardWorkoutLift -> toEntity()
            is CustomWorkoutLift -> toEntity()
            else -> throw ClassNotFoundException("${this::class.simpleName} is not implemented in ${WorkoutLiftMappingExtensions::class.simpleName}")
        }
    }

    private fun WorkoutLiftWithRelationships.toStandardWorkoutLift(): StandardWorkoutLift {
        return StandardWorkoutLift(
            id = this.workoutLiftEntity.id,
            workoutId = this.workoutLiftEntity.workoutId,
            liftId = this.workoutLiftEntity.liftId,
            liftName = this.liftEntity.name,
            liftMovementPattern = this.liftEntity.movementPattern,
            liftVolumeTypes = this.liftEntity.volumeTypesBitmask,
            liftSecondaryVolumeTypes = this.liftEntity.secondaryVolumeTypesBitmask,
            deloadWeek = this.workoutLiftEntity.deloadWeek,
            incrementOverride = this.liftEntity.incrementOverride,
            restTime = this.liftEntity.restTime,
            restTimerEnabled = this.liftEntity.restTimerEnabled,
            position = this.workoutLiftEntity.position,
            setCount = this.workoutLiftEntity.setCount,
            repRangeBottom = this.workoutLiftEntity.repRangeBottom!!,
            repRangeTop = this.workoutLiftEntity.repRangeTop!!,
            rpeTarget = this.workoutLiftEntity.rpeTarget!!,
            progressionScheme = this.workoutLiftEntity.progressionScheme,
            liftNote = this.liftEntity.note,
            stepSize = this.workoutLiftEntity.stepSize,
        )
    }

    private fun WorkoutLiftWithRelationships.toCustomWorkoutLift(): CustomWorkoutLift {
        return CustomWorkoutLift(
            id = this.workoutLiftEntity.id,
            workoutId = this.workoutLiftEntity.workoutId,
            liftId = this.workoutLiftEntity.liftId,
            liftName = this.liftEntity.name,
            liftMovementPattern = this.liftEntity.movementPattern,
            liftVolumeTypes = this.liftEntity.volumeTypesBitmask,
            liftSecondaryVolumeTypes = this.liftEntity.secondaryVolumeTypesBitmask,
            deloadWeek = this.workoutLiftEntity.deloadWeek,
            incrementOverride = this.liftEntity.incrementOverride,
            restTime = this.liftEntity.restTime,
            restTimerEnabled = this.liftEntity.restTimerEnabled,
            position = this.workoutLiftEntity.position,
            setCount = this.workoutLiftEntity.setCount,
            progressionScheme = this.workoutLiftEntity.progressionScheme,
            liftNote = this.liftEntity.note,
            customLiftSets = this.customLiftSetEntities
                .map { it.toDomainModel() }
                .sortedBy { it.position }
        )
    }

    private fun WorkoutLiftEntity.toStandardWorkoutLift(): StandardWorkoutLift {
        return StandardWorkoutLift(
            id = this.id,
            workoutId = this.workoutId,
            liftId = this.liftId,
            liftName = "LIFT NOT LOADED",
            liftMovementPattern = MovementPattern.CHEST_ISO,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = null,
            deloadWeek = this.deloadWeek,
            incrementOverride = null,
            restTime = null,
            restTimerEnabled = false,
            position = this.position,
            setCount = this.setCount,
            repRangeBottom = this.repRangeBottom!!,
            repRangeTop = this.repRangeTop!!,
            rpeTarget = this.rpeTarget!!,
            progressionScheme = this.progressionScheme,
            liftNote = "LIFT NOT LOADED",
            stepSize = this.stepSize,
        )
    }

    private fun WorkoutLiftEntity.toCustomWorkoutLift(): CustomWorkoutLift {
        return CustomWorkoutLift(
            id = this.id,
            workoutId = this.workoutId,
            liftId = this.liftId,
            liftName = "LIFT NOT LOADED",
            liftMovementPattern = MovementPattern.CHEST_ISO,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = null,
            deloadWeek = this.deloadWeek,
            incrementOverride = null,
            restTime = null,
            restTimerEnabled = false,
            position = this.position,
            setCount = this.setCount,
            progressionScheme = this.progressionScheme,
            liftNote = "LIFT NOT LOADED",
            customLiftSets = emptyList(),
        )
    }

    private fun StandardWorkoutLift.toEntity(): WorkoutLiftEntity {
        return WorkoutLiftEntity(
            id = this.id,
            liftId = this.liftId,
            workoutId = this.workoutId,
            deloadWeek = this.deloadWeek,
            position = this.position,
            setCount = this.setCount,
            rpeTarget = this.rpeTarget,
            repRangeBottom = this.repRangeBottom,
            repRangeTop = this.repRangeTop,
            progressionScheme = this.progressionScheme,
            stepSize = this.stepSize,
        )
    }

    private fun CustomWorkoutLift.toEntity(): WorkoutLiftEntity {
        return WorkoutLiftEntity(
            id = this.id,
            liftId = this.liftId,
            workoutId = this.workoutId,
            deloadWeek = this.deloadWeek,
            position = this.position,
            setCount = this.setCount,
            progressionScheme = this.progressionScheme,
        )
    }
}