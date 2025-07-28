package com.browntowndev.liftlab.core.domain.useCase.workout

import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.domain.models.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.PersonalRecord
import com.browntowndev.liftlab.core.domain.models.StandardSetResult
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class GetWorkoutCompletionSummaryUseCaseTest {

    private lateinit var getWorkoutCompletionSummaryUseCase: GetWorkoutCompletionSummaryUseCase

    @BeforeEach
    fun setUp() {
        getWorkoutCompletionSummaryUseCase = GetWorkoutCompletionSummaryUseCase()
    }

    @Test
    fun `invoke with completed sets and new PR returns correct summary`() {
        // Given
        val lifts = listOf(
            LoggingWorkoutLift(id = 1, liftId = 101, liftName = "Squat", position = 0, setCount = 3, liftMovementPattern = MovementPattern.LEG_PUSH, liftVolumeTypes = 1, liftSecondaryVolumeTypes = null, progressionScheme = ProgressionScheme.LINEAR_PROGRESSION, incrementOverride = null, restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null, sets = emptyList())
        )
        val loggingWorkout = LoggingWorkout(id = 1, name = "Test Workout", lifts = lifts)
        val personalRecords = listOf(PersonalRecord(liftId = 101L, personalRecord = 110))
        val completedSets = listOf<SetResult>(
            StandardSetResult(id = 1, workoutId = 1, liftId = 101, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 100f, reps = 5, rpe = 8f, mesoCycle = 1, microCycle = 1, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(id = 2, workoutId = 1, liftId = 101, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 105f, reps = 5, rpe = 9f, mesoCycle = 1, microCycle = 1, setType = SetType.STANDARD, isDeload = false) // New PR
        )

        // When
        val summary = getWorkoutCompletionSummaryUseCase(loggingWorkout, personalRecords, completedSets)

        // Then
        assertEquals("Test Workout", summary.workoutName)
        assertEquals(1, summary.liftCompletionSummaries.size)
        val liftSummary = summary.liftCompletionSummaries.first()
        assertEquals("Squat", liftSummary.liftName)
        assertEquals(2, liftSummary.setsCompleted)
        assertEquals(3, liftSummary.totalSets)
        assertEquals(105f, liftSummary.bestSetWeight)
        assertEquals(5, liftSummary.bestSetReps)
        assertTrue(liftSummary.isNewPersonalRecord)
    }

    @Test
    fun `invoke with no completed sets returns correct summary`() {
        // Given
        val lifts = listOf(
            LoggingWorkoutLift(id = 1, liftId = 101, liftName = "Squat", position = 0, setCount = 3, liftMovementPattern = MovementPattern.LEG_PUSH, liftVolumeTypes = 1, liftSecondaryVolumeTypes = null, progressionScheme = ProgressionScheme.LINEAR_PROGRESSION, incrementOverride = null, restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null, sets = emptyList())
        )
        val loggingWorkout = LoggingWorkout(id = 1, name = "Test Workout", lifts = lifts)

        // When
        val summary = getWorkoutCompletionSummaryUseCase(loggingWorkout, emptyList(), emptyList())

        // Then
        assertEquals(1, summary.liftCompletionSummaries.size)
        val liftSummary = summary.liftCompletionSummaries.first()
        assertEquals("Squat", liftSummary.liftName)
        assertEquals(0, liftSummary.setsCompleted)
        assertEquals(3, liftSummary.totalSets)
        assertEquals(0f, liftSummary.bestSetWeight)
        assertFalse(liftSummary.isNewPersonalRecord)
    }

    @Test
    fun `invoke with partial completion returns correct summary`() {
        // Given
        val lifts = listOf(
            LoggingWorkoutLift(id = 1, liftId = 101, liftName = "Squat", position = 0, setCount = 3, liftMovementPattern = MovementPattern.LEG_PUSH, liftVolumeTypes = 1, liftSecondaryVolumeTypes = null, progressionScheme = ProgressionScheme.LINEAR_PROGRESSION, incrementOverride = null, restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null, sets = emptyList()),
            LoggingWorkoutLift(id = 2, liftId = 102, liftName = "Bench", position = 1, setCount = 3, liftMovementPattern = MovementPattern.HORIZONTAL_PUSH, liftVolumeTypes = 1, liftSecondaryVolumeTypes = null, progressionScheme = ProgressionScheme.LINEAR_PROGRESSION, incrementOverride = null, restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null, sets = emptyList())
        )
        val loggingWorkout = LoggingWorkout(id = 1, name = "Test Workout", lifts = lifts)
        val completedSets = listOf<SetResult>(
            StandardSetResult(id = 1, workoutId = 1, liftId = 101, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 100f, reps = 5, rpe = 8f, mesoCycle = 1, microCycle = 1, setType = SetType.STANDARD, isDeload = false)
        )

        // When
        val summary = getWorkoutCompletionSummaryUseCase(loggingWorkout, emptyList(), completedSets)

        // Then
        assertEquals(2, summary.liftCompletionSummaries.size)
        val squatSummary = summary.liftCompletionSummaries.find { it.liftId == 101L }!!
        val benchSummary = summary.liftCompletionSummaries.find { it.liftId == 102L }!!
        assertEquals(1, squatSummary.setsCompleted)
        assertEquals(0, benchSummary.setsCompleted)
    }
}
