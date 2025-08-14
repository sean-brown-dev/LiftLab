package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingDropSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.StandardSetResult
import com.browntowndev.liftlab.core.domain.useCase.utils.WeightCalculationUtils
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
            StandardSetResult(id = 1, workoutId = 1, liftId = 101, liftPosition = 0, setPosition = 0, weight = 100f, reps = 5, rpe = 8f,
                setType = SetType.STANDARD, isDeload = false
            )
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

    @Test
    fun `getWithWeightRecommendation - drop set uses last weight times (1 - dropPercent) and rounds to increment`() {
        // Given: one completed standard set followed by an incomplete drop set
        val incrementOverride = 2.5f
        val lastCompleted = LoggingStandardSet(
            position = 0,
            weightRecommendation = 100f,
            repRangeBottom = 5,
            repRangeTop = 8,
            rpeTarget = 8f,
            complete = true,
            completedWeight = 100f,
            completedReps = 6,
            completedRpe = 8.5f,
            repRangePlaceholder = "5-8",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val dropSet = LoggingDropSet(
            position = 1,
            dropPercentage = 0.20f, // 20% drop
            weightRecommendation = null,
            complete = false,
            repRangeBottom = 5,
            repRangeTop = 8,
            rpeTarget = 8f,
            repRangePlaceholder = "5-8",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val lift = LoggingWorkoutLift(
            id = 1, liftId = 101, liftName = "Bench", position = 0,
            sets = listOf(lastCompleted, dropSet),
            liftMovementPattern = MovementPattern.LEG_PUSH,
            liftVolumeTypes = 1, liftSecondaryVolumeTypes = null,
            progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
            incrementOverride = incrementOverride,
            restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null
        )

        // When: no new set results; hydrator should calculate next-set recommendation
        val result = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), emptyList(), microCycle = 0)
        val next = result.first().sets[1] as LoggingDropSet

        // Then: (100 * (1 - 0.2)) rounded to 2.5 → 80.0
        assertEquals(80f, next.weightRecommendation)
    }

    @Test
    fun `getWithWeightRecommendation - standard set recalculates when bottom missed aiming at top`() {
        // Given: last set was too heavy (missed RPE-adjusted bottom)
        val incrementOverride = 2.5f
        val lastCompleted = LoggingStandardSet(
            position = 0,
            weightRecommendation = 100f,
            repRangeBottom = 8, repRangeTop = 10, rpeTarget = 8f,
            complete = true,
            completedWeight = 100f,
            completedReps = 5,               // raw reps
            completedRpe = 10f,              // RPE 10 → adjustedCompleted = 5
            repRangePlaceholder = "8-10", hadInitialWeightRecommendation = false, previousSetResultLabel = ""
        )
        val nextStandard = LoggingStandardSet(
            position = 1,
            weightRecommendation = null,
            repRangeBottom = 8, repRangeTop = 10, rpeTarget = 8f,
            complete = false,
            repRangePlaceholder = "8-10", hadInitialWeightRecommendation = false, previousSetResultLabel = ""
        )
        val lift = LoggingWorkoutLift(
            id = 1, liftId = 101, liftName = "Squat", position = 0,
            sets = listOf(lastCompleted, nextStandard),
            liftMovementPattern = MovementPattern.LEG_PUSH,
            liftVolumeTypes = 1, liftSecondaryVolumeTypes = null,
            progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
            incrementOverride = incrementOverride,
            restTime = 120.seconds, restTimerEnabled = true, deloadWeek = null, note = null
        )

        // Expected: repGoal should be repRangeTop (10) for conservative lighter suggestion
        val expected = WeightCalculationUtils.calculateSuggestedWeight(
            completedWeight = 100f,
            completedReps = 5,
            completedRpe = 10f,
            repGoal = 10,
            rpeGoal = 8f,
            roundingFactor = incrementOverride
        )

        // When
        val result = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), emptyList(), microCycle = 0)
        val updated = result.first().sets[1] as LoggingStandardSet

        // Then
        assertEquals(expected, updated.weightRecommendation!!, 1e-3f)
    }

    @Test
    fun `getWithWeightRecommendation - standard set recalculates when too easy by threshold aiming at bottom`() {
        // Given: last set exceeded adjusted top by >= threshold (3)
        val incrementOverride = 2.5f
        val lastCompleted = LoggingStandardSet(
            position = 0,
            weightRecommendation = 80f,
            repRangeBottom = 6, repRangeTop = 8, rpeTarget = 8f,
            complete = true,
            completedWeight = 80f,
            completedReps = 12,             // raw reps
            completedRpe = 10f,             // adjustedCompleted = 12
            repRangePlaceholder = "6-8", hadInitialWeightRecommendation = false, previousSetResultLabel = ""
        )
        val nextStandard = LoggingStandardSet(
            position = 1,
            weightRecommendation = null,
            repRangeBottom = 6, repRangeTop = 8, rpeTarget = 8f,
            complete = false,
            repRangePlaceholder = "6-8", hadInitialWeightRecommendation = false, previousSetResultLabel = ""
        )
        val lift = LoggingWorkoutLift(
            id = 1, liftId = 201, liftName = "Row", position = 0,
            sets = listOf(lastCompleted, nextStandard),
            liftMovementPattern = MovementPattern.LEG_PUSH,
            liftVolumeTypes = 1, liftSecondaryVolumeTypes = null,
            progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
            incrementOverride = incrementOverride,
            restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null
        )

        // Expected: repGoal should be repRangeBottom (6) to push weight up
        val expected = WeightCalculationUtils.calculateSuggestedWeight(
            completedWeight = 80f,
            completedReps = 12,
            completedRpe = 10f,
            repGoal = 6,
            rpeGoal = 8f,
            roundingFactor = incrementOverride
        )

        // When
        val result = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), emptyList(), microCycle = 0)
        val updated = result.first().sets[1] as LoggingStandardSet

        // Then
        assertEquals(expected, updated.weightRecommendation!!, 1e-3f)
    }

    @Test
    fun `getWithWeightRecommendation - standard set recalculates when bottom or RPE goal changed aiming at top`() {
        // Given: last set met goals, but bottom or RPE target changed
        val incrementOverride = 2.5f
        // Last completed met the old goals: adjustedCompleted = 8 fits in old 7..9
        val lastCompleted = LoggingStandardSet(
            position = 0,
            weightRecommendation = 90f,
            repRangeBottom = 7, repRangeTop = 9, rpeTarget = 8f,
            complete = true,
            completedWeight = 90f,
            completedReps = 8,     // raw reps
            completedRpe = 10f,    // adjustedCompleted = 8
            repRangePlaceholder = "7-9", hadInitialWeightRecommendation = false, previousSetResultLabel = ""
        )
        // New set changes bottom (or RPE) but not top-only
        val nextStandard = LoggingStandardSet(
            position = 1,
            weightRecommendation = null,
            repRangeBottom = 8,    // bottom changed 7 -> 8 (material change)
            repRangeTop = 10,      // top can change; still conservative
            rpeTarget = 8f,
            complete = false,
            repRangePlaceholder = "8-10", hadInitialWeightRecommendation = false, previousSetResultLabel = ""
        )
        val lift = LoggingWorkoutLift(
            id = 1, liftId = 301, liftName = "Press", position = 0,
            sets = listOf(lastCompleted, nextStandard),
            liftMovementPattern = MovementPattern.LEG_PUSH,
            liftVolumeTypes = 1, liftSecondaryVolumeTypes = null,
            progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
            incrementOverride = incrementOverride,
            restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null
        )

        // Expected: aim at the (new) top conservatively
        val expected = WeightCalculationUtils.calculateSuggestedWeight(
            completedWeight = 90f,
            completedReps = 8,
            completedRpe = 10f,
            repGoal = 10,
            rpeGoal = 8f,
            roundingFactor = incrementOverride
        )

        // When
        val result = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), emptyList(), microCycle = 0)
        val updated = result.first().sets[1] as LoggingStandardSet

        // Then
        assertEquals(expected, updated.weightRecommendation!!, 1e-3f)
    }
}
