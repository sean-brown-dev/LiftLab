package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SkipDeloadAndStartWorkoutUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var startWorkoutUseCase: StartWorkoutUseCase
    private lateinit var skipDeloadAndStartWorkoutUseCase: SkipDeloadAndStartWorkoutUseCase

    @BeforeEach
    fun setUp() {
        programsRepository = mockk(relaxed = true)
        startWorkoutUseCase = mockk(relaxed = true)
        skipDeloadAndStartWorkoutUseCase = SkipDeloadAndStartWorkoutUseCase(
            programsRepository,
            startWorkoutUseCase
        )
    }

    @Test
    fun `invoke updates cycle and starts workout`() = runTest {
        // Given
        val programMetadata = ActiveProgramMetadata(
            programId = 1L,
            name = "Test Program",
            deloadWeek = 4,
            currentMesocycle = 1,
            currentMicrocycle = 3,
            currentMicrocyclePosition = 2,
            workoutCount = 3
        )
        val workoutId = 101L

        // When
        skipDeloadAndStartWorkoutUseCase(programMetadata, workoutId)

        // Then
        coVerify {
            programsRepository.updateMesoAndMicroCycle(
                id = programMetadata.programId,
                mesoCycle = programMetadata.currentMesocycle + 1,
                microCycle = 0,
                microCyclePosition = 0
            )
        }
        coVerify { startWorkoutUseCase(workoutId) }
    }
}
