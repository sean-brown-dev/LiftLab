package com.browntowndev.liftlab.core.persistence.mapping

import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.queryable.ProgramWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.Program

class ProgramMapper(private val workoutMapper: WorkoutMapper) {
    fun map(entity: ProgramWithRelationships): ProgramDto {
        return ProgramDto(
            id = entity.program.id,
            name = entity.program.name,
            isActive = entity.program.isActive,
            deloadWeek = entity.program.deloadWeek,
            currentMesocycle = entity.program.currentMesocycle,
            currentMicrocycle = entity.program.currentMicrocycle,
            currentMicrocyclePosition = entity.program.currentMicrocyclePosition,
            workouts = entity.workouts.map { workoutMapper.map(it) }
        )
    }

    fun map(setDto: ProgramDto): Program {
        return Program(
            id = setDto.id,
            name = setDto.name,
            isActive = setDto.isActive,
            currentMesocycle = setDto.currentMesocycle,
            currentMicrocycle = setDto.currentMicrocycle,
            currentMicrocyclePosition = setDto.currentMicrocyclePosition,
        )
    }
}