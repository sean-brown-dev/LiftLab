package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.StandardSetResult
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class HydrateLoggingWorkoutWithCompletedSetsUseCaseTest {

    private lateinit var hydrateLoggingWorkoutWithCompletedSetsUseCase: HydrateLoggingWorkoutWithCompletedSetsUseCase

    @BeforeEach
    fun setUp() {
        hydrateLoggingWorkoutWithCompletedSetsUseCase = HydrateLoggingWorkoutWithCompletedSetsUseCase()
    }

    @Test
    fun `invoke marks set as complete from inProgressResults`() {
        // Given
        val sets = listOf(
            LoggingStandardSet(position = 0, weightRecommendation = 100f, repRangeBottom = 5, rpeTarget = 8f, complete = false, repRangeTop = 8, repRangePlaceholder = "5-8", hadInitialWeightRecommendation = false, previousSetResultLabel = "")
        )
        val lifts = listOf(
            LoggingWorkoutLift(id = 1, liftId = 101, liftName = "Squat", position = 0, sets = sets, liftMovementPattern = MovementPattern.LEG_PUSH, liftVolumeTypes = 1, liftSecondaryVolumeTypes = null, progressionScheme = ProgressionScheme.LINEAR_PROGRESSION, incrementOverride = null, restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null)
        )
        val loggingWorkout = LoggingWorkout(id = 1, name = "Test Workout", lifts = lifts)
        val inProgressResults = listOf<SetResult>(
            StandardSetResult(id = 1, workoutId = 1, liftId = 101, liftPosition = 0, setPosition = 0, weight = 100f, reps = 5, rpe = 8f, mesoCycle = 1, microCycle = 1, setType = SetType.STANDARD, isDeload = false, weightRecommendation = 100f)
        )

        // When
        val result = hydrateLoggingWorkoutWithCompletedSetsUseCase(loggingWorkout.lifts, inProgressResults, 1)

        // Then
        val resultLift = result.first()
        val resultSet = resultLift.sets.first() as LoggingStandardSet
        assertTrue(resultSet.complete)
        assertEquals(100f, resultSet.completedWeight)
        assertEquals(5, resultSet.completedReps)
        assertEquals(8f, resultSet.completedRpe)
    }

    @Test
    fun `invoke marks previously complete set as incomplete`() {
        // Given
        val sets = listOf(
            LoggingStandardSet(position = 0, weightRecommendation = 100f, repRangeBottom = 5, rpeTarget = 8f, complete = true, completedWeight = 100f, completedReps = 5, completedRpe = 8f, repRangeTop = 8, repRangePlaceholder = "5-8", hadInitialWeightRecommendation = false, previousSetResultLabel = "")
        )
        val lifts = listOf(
            LoggingWorkoutLift(id = 1, liftId = 101, liftName = "Squat", position = 0, sets = sets, liftMovementPattern = MovementPattern.LEG_PUSH, liftVolumeTypes = 1, liftSecondaryVolumeTypes = null, progressionScheme = ProgressionScheme.LINEAR_PROGRESSION, incrementOverride = null, restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null)
        )
        val loggingWorkout = LoggingWorkout(id = 1, name = "Test Workout", lifts = lifts)

        // When
        val result = hydrateLoggingWorkoutWithCompletedSetsUseCase(loggingWorkout.lifts, emptyList(), 1)

        // Then
        val resultLift = result.first()
        val resultSet = resultLift.sets.first()
        assertFalse(resultSet.complete)
    }
}
