package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.PersonalRecord
import com.browntowndev.liftlab.core.domain.models.workoutLogging.StandardSetResult
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
            LoggingWorkoutLift(id = 1, liftId = 101, liftName = "Squat", position = 0, liftMovementPattern = MovementPattern.LEG_PUSH, liftVolumeTypes = 1, liftSecondaryVolumeTypes = null, progressionScheme = ProgressionScheme.LINEAR_PROGRESSION, incrementOverride = null, restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null, sets = emptyList())
        )
        val loggingWorkout = LoggingWorkout(id = 1, name = "Test Workout", lifts = lifts)
        val personalRecords = listOf(PersonalRecord(liftId = 101L, personalRecord = 110))
        val completedSets = listOf<SetResult>(
            StandardSetResult(id = 1, workoutId = 1, liftId = 101, liftPosition = 0, setPosition = 0,
                weight = 100f, reps = 5, rpe = 8f,
                setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(id = 2, workoutId = 1, liftId = 101, liftPosition = 0, setPosition = 1,
                weight = 105f, reps = 5, rpe = 9f,
                setType = SetType.STANDARD, isDeload = false) // New PR
        )

        // When
        val summary = getWorkoutCompletionSummaryUseCase(loggingWorkout, personalRecords, completedSets)

        // Then
        assertEquals("Test Workout", summary.workoutName)
        assertEquals(1, summary.liftCompletionSummaries.size)
        val liftSummary = summary.liftCompletionSummaries.first()
        assertEquals("Squat", liftSummary.liftName)
        assertEquals(2, liftSummary.setsCompleted)
        assertEquals(2, liftSummary.totalSets)
        assertEquals(105f, liftSummary.bestSetWeight)
        assertEquals(5, liftSummary.bestSetReps)
        assertTrue(liftSummary.isNewPersonalRecord)
    }

    @Test
    fun `invoke with no completed sets returns correct summary`() {
        // Given
        val lifts = listOf(
            LoggingWorkoutLift(id = 1, liftId = 101, liftName = "Squat", position = 0, liftMovementPattern = MovementPattern.LEG_PUSH, liftVolumeTypes = 1, liftSecondaryVolumeTypes = null, progressionScheme = ProgressionScheme.LINEAR_PROGRESSION, incrementOverride = null, restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null, sets = emptyList())
        )
        val loggingWorkout = LoggingWorkout(id = 1, name = "Test Workout", lifts = lifts)

        // When
        val summary = getWorkoutCompletionSummaryUseCase(loggingWorkout, emptyList(), emptyList())

        // Then
        assertEquals(1, summary.liftCompletionSummaries.size)
        val liftSummary = summary.liftCompletionSummaries.first()
        assertEquals("Squat", liftSummary.liftName)
        assertEquals(0, liftSummary.setsCompleted)
        assertEquals(0, liftSummary.totalSets)
        assertEquals(0f, liftSummary.bestSetWeight)
        assertFalse(liftSummary.isNewPersonalRecord)
    }

    @Test
    fun `invoke with partial completion returns correct summary`() {
        // Given
        val lifts = listOf(
            LoggingWorkoutLift(id = 1, liftId = 101, liftName = "Squat", position = 0, liftMovementPattern = MovementPattern.LEG_PUSH, liftVolumeTypes = 1, liftSecondaryVolumeTypes = null, progressionScheme = ProgressionScheme.LINEAR_PROGRESSION, incrementOverride = null, restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null, sets = emptyList()),
            LoggingWorkoutLift(id = 2, liftId = 102, liftName = "Bench", position = 1, liftMovementPattern = MovementPattern.HORIZONTAL_PUSH, liftVolumeTypes = 1, liftSecondaryVolumeTypes = null, progressionScheme = ProgressionScheme.LINEAR_PROGRESSION, incrementOverride = null, restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null, sets = emptyList())
        )
        val loggingWorkout = LoggingWorkout(id = 1, name = "Test Workout", lifts = lifts)
        val completedSets = listOf<SetResult>(
            StandardSetResult(id = 1, workoutId = 1, liftId = 101, liftPosition = 0, setPosition = 0,
                weight = 100f, reps = 5, rpe = 8f,
                setType = SetType.STANDARD, isDeload = false)
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
