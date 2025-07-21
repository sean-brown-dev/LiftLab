package com.browntowndev.liftlab.core.domain.mapping

import com.browntowndev.liftlab.core.domain.mapping.WorkoutMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.domain.models.Program
import com.browntowndev.liftlab.core.persistence.room.dtos.ProgramWithRelationshipsDto
import com.browntowndev.liftlab.core.persistence.room.entities.ProgramEntity

object ProgramMappingExtensions {
    fun ProgramWithRelationshipsDto.toDomainModel(): Program {
        return Program(
            id = this.programEntity.id,
            name = this.programEntity.name,
            isActive = this.programEntity.isActive,
            deloadWeek = this.programEntity.deloadWeek,
            currentMesocycle = this.programEntity.currentMesocycle,
            currentMicrocycle = this.programEntity.currentMicrocycle,
            currentMicrocyclePosition = this.programEntity.currentMicrocyclePosition,
            workouts = this.workouts
                .map { it.toDomainModel() }
                .sortedBy { it.position }
        )
    }

    fun ProgramEntity.toDomainModel(): Program {
        return Program(
            id = this.id,
            name = this.name,
            isActive = this.isActive,
            deloadWeek = this.deloadWeek,
            currentMesocycle = this.currentMesocycle,
            currentMicrocycle = this.currentMicrocycle,
            currentMicrocyclePosition = this.currentMicrocyclePosition,
        )
    }

    fun Program.toEntity(): ProgramEntity {
        return ProgramEntity(
            id = this.id,
            name = this.name,
            isActive = this.isActive,
            currentMesocycle = this.currentMesocycle,
            currentMicrocycle = this.currentMicrocycle,
            currentMicrocyclePosition = this.currentMicrocyclePosition,
        )
    }
}