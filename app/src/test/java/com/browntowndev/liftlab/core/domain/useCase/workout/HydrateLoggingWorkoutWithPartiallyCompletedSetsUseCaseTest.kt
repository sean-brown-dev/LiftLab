package com.browntowndev.liftlab.core.domain.useCase.workout

import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.LoggingWorkoutLift
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class HydrateLoggingWorkoutWithPartiallyCompletedSetsUseCaseTest {

    private lateinit var useCase: HydrateLoggingWorkoutWithPartiallyCompletedSetsUseCase

    @BeforeEach
    fun setUp() {
        useCase = HydrateLoggingWorkoutWithPartiallyCompletedSetsUseCase()
    }

    @Test
    fun `invoke with partially completed set hydrates correctly`() {
        // Given
        val sets = listOf(
            LoggingStandardSet(position = 0, complete = false, completedReps = 5, repRangeBottom = 5, repRangeTop = 8, repRangePlaceholder = "5-8", rpeTarget = 8f, hadInitialWeightRecommendation = false, previousSetResultLabel = "", weightRecommendation = 100f),
            LoggingStandardSet(position = 1, complete = true, completedReps = 8, repRangeBottom = 5, repRangeTop = 8, repRangePlaceholder = "5-8", rpeTarget = 8f, hadInitialWeightRecommendation = false, previousSetResultLabel = "", weightRecommendation = 100f)
        )
        val lifts = listOf(
            LoggingWorkoutLift(id = 1, liftId = 101, liftName = "Squat", position = 0, setCount = 2, sets = sets, liftMovementPattern = MovementPattern.LEG_PUSH, liftVolumeTypes = 1, liftSecondaryVolumeTypes = null, progressionScheme = ProgressionScheme.LINEAR_PROGRESSION, incrementOverride = null, restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null)
        )
        val loggingWorkout = LoggingWorkout(id = 1, name = "Test Workout", lifts = lifts)

        // When
        val result = useCase(loggingWorkout)

        // Then
        val resultLift = result.lifts.first()
        val partiallyCompletedSet = resultLift.sets.first() as LoggingStandardSet
        val completedSet = resultLift.sets.last() as LoggingStandardSet

        assertEquals(5, partiallyCompletedSet.completedReps)
        assertNull(partiallyCompletedSet.completedWeight)
        assertNull(partiallyCompletedSet.completedRpe)

        // Ensure other sets are untouched
        assertEquals(8, completedSet.completedReps)

    }

    @Test
    fun `invoke with no partially completed sets returns same workout`() {
        // Given
        val sets = listOf(
            LoggingStandardSet(position = 0, complete = true, completedReps = 5, repRangeBottom = 5, repRangeTop = 8, repRangePlaceholder = "5-8", rpeTarget = 8f, hadInitialWeightRecommendation = false, previousSetResultLabel = "", weightRecommendation = 100f),
            LoggingStandardSet(position = 1, complete = false, repRangeBottom = 5, repRangeTop = 8, repRangePlaceholder = "5-8", rpeTarget = 8f, hadInitialWeightRecommendation = false, previousSetResultLabel = "", weightRecommendation = 100f)
        )
        val lifts = listOf(
            LoggingWorkoutLift(id = 1, liftId = 101, liftName = "Squat", position = 0, setCount = 2, sets = sets, liftMovementPattern = MovementPattern.LEG_PUSH, liftVolumeTypes = 1, liftSecondaryVolumeTypes = null, progressionScheme = ProgressionScheme.LINEAR_PROGRESSION, incrementOverride = null, restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null)
        )
        val loggingWorkout = LoggingWorkout(id = 1, name = "Test Workout", lifts = lifts)

        // When
        val result = useCase(loggingWorkout)

        // Then
        assertEquals(loggingWorkout, result)
    }
}
