package com.browntowndev.liftlab.core.persistence.mapping

import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.queryable.ProgramWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.Program

class ProgramMapper(private val workoutMapper: WorkoutMapper) {
    fun map(from: ProgramWithRelationships): ProgramDto {
        return ProgramDto(
            id = from.program.id,
            name = from.program.name,
            isActive = from.program.isActive,
            deloadWeek = from.program.deloadWeek,
            currentMesocycle = from.program.currentMesocycle,
            currentMicrocycle = from.program.currentMicrocycle,
            currentMicrocyclePosition = from.program.currentMicrocyclePosition,
            workouts = from.workouts.map { workoutMapper.map(it) }
        )
    }

    fun map(from: Program): ProgramDto {
        return ProgramDto(
            id = from.id,
            name = from.name,
            isActive = from.isActive,
            deloadWeek = from.deloadWeek,
            currentMesocycle = from.currentMesocycle,
            currentMicrocycle = from.currentMicrocycle,
            currentMicrocyclePosition = from.currentMicrocyclePosition,
        )
    }

    fun map(from: ProgramDto): Program {
        return Program(
            id = from.id,
            name = from.name,
            isActive = from.isActive,
            currentMesocycle = from.currentMesocycle,
            currentMicrocycle = from.currentMicrocycle,
            currentMicrocyclePosition = from.currentMicrocyclePosition,
        )
    }
}