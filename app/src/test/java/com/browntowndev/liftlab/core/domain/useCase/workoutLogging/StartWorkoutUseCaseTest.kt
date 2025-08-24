package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Date

class StartWorkoutUseCaseTest {

    private lateinit var workoutInProgressRepository: WorkoutInProgressRepository
    private lateinit var startWorkoutUseCase: StartWorkoutUseCase
    private lateinit var transactionScope: TransactionScope

    @BeforeEach
    fun setUp() {
        workoutInProgressRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        startWorkoutUseCase = StartWorkoutUseCase(workoutInProgressRepository, transactionScope)
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
            workoutInProgressRepository.upsert(withArg { workoutInProgress ->
                assertEquals(workoutId, workoutInProgress.workoutId)
                assertTrue(workoutInProgress.startTime.time >= startTime.time)
            })
        }
    }
}
