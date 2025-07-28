package com.browntowndev.liftlab.core.domain.useCase.workout

import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.domain.models.StandardSetResult
import com.browntowndev.liftlab.core.domain.models.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.Workout
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class GetNewestSetResultsUseCaseTest {

    private lateinit var workoutLogRepository: WorkoutLogRepository
    private lateinit var getNewestSetResultsUseCase: GetNewestSetResultsUseCase

    @BeforeEach
    fun setUp() {
        workoutLogRepository = mockk()
        getNewestSetResultsUseCase = GetNewestSetResultsUseCase(workoutLogRepository)
    }

    @Test
    fun `invoke with empty liftIdsToSearchFor returns existing results`() = runTest {
        // Given
        val workout = Workout(id = 1, programId = 1, name = "Test Workout", position = 0, lifts = emptyList())
        val existingResults = listOf(
            StandardSetResult(id = 1, workoutId = 1, liftId = 101, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 100f, reps = 5, rpe = 8f, mesoCycle = 1, microCycle = 1, setType = SetType.STANDARD, isDeload = false)
        )
        val liftIdsToSearchFor = emptyList<Long>()

        // When
        val result = getNewestSetResultsUseCase(workout, liftIdsToSearchFor, existingResults, false)

        // Then
        assertEquals(existingResults, result)
        coVerify(exactly = 0) { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) }
    }

    @Test
    fun `invoke returns merged results`() = runTest {
        // Given
        val workout = Workout(id = 1, programId = 1, name = "Test Workout", position = 0, lifts = emptyList())
        val existingResults = listOf(
            StandardSetResult(id = 1, workoutId = 1, liftId = 101, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 100f, reps = 5, rpe = 8f, mesoCycle = 1, microCycle = 1, setType = SetType.STANDARD, isDeload = false)
        )
        val newResults = listOf(
            StandardSetResult(id = 2, workoutId = 1, liftId = 102, liftPosition = 1, setPosition = 0, weightRecommendation = null, weight = 200f, reps = 5, rpe = 8f, mesoCycle = 1, microCycle = 1, setType = SetType.STANDARD, isDeload = false)
        )
        val liftIdsToSearchFor = listOf(102L)

        coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns newResults

        // When
        val result = getNewestSetResultsUseCase(workout, liftIdsToSearchFor, existingResults, false)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.containsAll(existingResults))
        assertTrue(result.containsAll(newResults))
    }

    @Test
    fun `invoke calls repository with correct linear progression lift IDs`() = runTest {
        // Given
        val lpLift = StandardWorkoutLift(id = 1, workoutId = 1, liftId = 101, liftName = "Squat", liftMovementPattern = MovementPattern.LEG_PUSH, liftVolumeTypes = 1, liftSecondaryVolumeTypes = null, position = 0, setCount = 3, progressionScheme = ProgressionScheme.LINEAR_PROGRESSION, incrementOverride = null, restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, liftNote = null, rpeTarget = 8.0f, repRangeBottom = 5, repRangeTop = 8)
        val dpLift = StandardWorkoutLift(id = 2, workoutId = 1, liftId = 102, liftName = "Bench", liftMovementPattern = MovementPattern.HORIZONTAL_PUSH, liftVolumeTypes = 1, liftSecondaryVolumeTypes = null, position = 1, setCount = 3, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION, incrementOverride = null, restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, liftNote = null, rpeTarget = 8.0f, repRangeBottom = 5, repRangeTop = 8)

        val workout = Workout(id = 1, programId = 1, name = "Test Workout", position = 0, lifts = listOf(lpLift, dpLift))
        val liftIdsToSearchFor = listOf(101L, 102L)

        coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns emptyList()

        // When
        getNewestSetResultsUseCase(workout, liftIdsToSearchFor, emptyList(), false)

        // Then
        coVerify {
            workoutLogRepository.getMostRecentSetResultsForLiftIds(
                liftIds = liftIdsToSearchFor,
                linearProgressionLiftIds = hashSetOf(101L),
                includeDeload = false
            )
        }
    }
}
