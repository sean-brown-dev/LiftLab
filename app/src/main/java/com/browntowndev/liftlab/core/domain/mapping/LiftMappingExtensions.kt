package com.browntowndev.liftlab.core.domain.mapping

import com.browntowndev.liftlab.core.domain.models.Lift
import com.browntowndev.liftlab.core.persistence.entities.room.LiftEntity

object LiftMappingExtensions {
    fun Lift.toEntity(): LiftEntity {
        return LiftEntity(
            id = this.id,
            name = this.name,
            movementPattern = this.movementPattern,
            volumeTypesBitmask = this.volumeTypesBitmask,
            secondaryVolumeTypesBitmask = this.secondaryVolumeTypesBitmask,
            restTime = this.restTime,
            restTimerEnabled = this.restTimerEnabled,
            incrementOverride = this.incrementOverride,
            isHidden = this.isHidden,
            isBodyweight = this.isBodyweight,
            note = this.note,
        )
    }

    fun LiftEntity.toDomainModel(): Lift {
        return Lift(
            id = this.id,
            name = this.name,
            movementPattern = this.movementPattern,
            volumeTypesBitmask = this.volumeTypesBitmask,
            secondaryVolumeTypesBitmask = this.secondaryVolumeTypesBitmask,
            restTime = this.restTime,
            restTimerEnabled = this.restTimerEnabled,
            incrementOverride = this.incrementOverride,
            isHidden = this.isHidden,
            isBodyweight = this.isBodyweight,
            note = this.note,
        )
    }
}

