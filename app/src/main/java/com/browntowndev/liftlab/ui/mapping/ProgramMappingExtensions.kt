package com.browntowndev.liftlab.ui.mapping

import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.ui.models.workout.ProgramUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.ActiveProgramMetadataUiModel

fun Program.toUiModel(): ProgramUiModel {
    return ProgramUiModel(
        id = id,
        name = name,
        isActive = isActive,
        deloadWeek = deloadWeek,
        currentMicrocycle = currentMicrocycle,
        currentMicrocyclePosition = currentMicrocyclePosition,
        currentMesocycle = currentMesocycle,
        workouts = workouts.map { it.toUiModel() }
    )
}

fun ProgramUiModel.toDomainModel(): Program {
    return Program(
        id = id,
        name = name,
        isActive = isActive,
        deloadWeek = deloadWeek,
        currentMicrocycle = currentMicrocycle,
        currentMicrocyclePosition = currentMicrocyclePosition,
        currentMesocycle = currentMesocycle,
        workouts = workouts.map { it.toDomainModel() }
    )
}

fun ActiveProgramMetadata.toUiModel(): ActiveProgramMetadataUiModel {
    return ActiveProgramMetadataUiModel(
        programId = this.programId,
        name = this.name,
        deloadWeek = this.deloadWeek,
        currentMesocycle = this.currentMesocycle,
        currentMicrocycle = this.currentMicrocycle,
        currentMicrocyclePosition = this.currentMicrocyclePosition,
        workoutCount = this.workoutCount,
    )
}

fun ActiveProgramMetadataUiModel.toDomainModel(): ActiveProgramMetadata {
    return ActiveProgramMetadata(
        programId = this.programId,
        name = this.name,
        deloadWeek = this.deloadWeek,
        currentMesocycle = this.currentMesocycle,
        currentMicrocycle = this.currentMicrocycle,
        currentMicrocyclePosition = this.currentMicrocyclePosition,
        workoutCount = this.workoutCount,
    )
}
