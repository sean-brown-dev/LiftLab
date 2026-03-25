package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.domain.models.programConfiguration.ProgramConfigurationState
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetProgramConfigurationStateFlowUseCase(
    private val programsRepository: ProgramsRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<ProgramConfigurationState> {
        return programsRepository.getAllFlow()
            .map { allPrograms ->
                val activeProgram = allPrograms
                    .firstOrNull { program -> program.isActive }
                    ?.let { program ->
                        program.copy(
                            workouts = program.workouts.sortedBy { it.position }
                        )
                    }
                ProgramConfigurationState(
                    program = activeProgram,
                    allPrograms = allPrograms,
                )
            }
    }
}