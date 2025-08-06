package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CancelWorkoutUseCaseTest {

    private lateinit var workoutInProgressRepository: WorkoutInProgressRepository
    private lateinit var setResultsRepository: LiveWorkoutCompletedSetsRepository
    private lateinit var cancelWorkoutUseCase: CancelWorkoutUseCase
    private lateinit var transactionScope: TransactionScope

    @BeforeEach
    fun setUp() {
        workoutInProgressRepository = mockk(relaxed = true)
        setResultsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        cancelWorkoutUseCase = CancelWorkoutUseCase(workoutInProgressRepository, setResultsRepository, transactionScope)
    }

    @Test
    fun `invoke calls repositories to delete data`() = runTest {
        // Given
        val programMetadata = ActiveProgramMetadata(
            programId = 1L,
            name = "Test Program",
            deloadWeek = 4,
            currentMesocycle = 1,
            currentMicrocycle = 1,
            currentMicrocyclePosition = 0,
            workoutCount = 3
        )
        val workout = LoggingWorkout(
            id = 101L,
            name = "Test Workout",
            lifts = emptyList()
        )

        // When
        cancelWorkoutUseCase(programMetadata, workout)

        // Then
        coVerify { workoutInProgressRepository.deleteAll() }
        coVerify {
            setResultsRepository.deleteAll()
        }
    }
}
