package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.util.Date

class StartWorkoutUseCaseTest {

    private lateinit var workoutInProgressRepository: WorkoutInProgressRepository
    private lateinit var startWorkoutUseCase: StartWorkoutUseCase

    @BeforeEach
    fun setUp() {
        workoutInProgressRepository = mockk(relaxed = true)
        startWorkoutUseCase = StartWorkoutUseCase(workoutInProgressRepository)
    }

    @Test
    fun `invoke inserts workout in progress`() = runTest {
        // Given
        val workoutId = 1L
        val startTime = Date()

        // When
        startWorkoutUseCase(workoutId)

        // Then
        coVerify {
            workoutInProgressRepository.insert(withArg { workoutInProgress ->
                assertEquals(workoutId, workoutInProgress.workoutId)
                assertTrue(workoutInProgress.startTime.time >= startTime.time)
            })
        }
    }
}
