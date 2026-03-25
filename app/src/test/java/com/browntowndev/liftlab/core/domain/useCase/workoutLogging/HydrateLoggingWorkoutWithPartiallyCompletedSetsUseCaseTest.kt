package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class HydrateLoggingWorkoutWithPartiallyCompletedSetsUseCaseTest {

    private lateinit var useCase: HydrateLoggingWorkoutWithExistingLiftDataUseCase

    @BeforeEach
    fun setUp() {
        useCase = HydrateLoggingWorkoutWithExistingLiftDataUseCase()
    }

    private fun createLoggingWorkoutLift(
        id: Long,
        liftId: Long,
        liftName: String,
        sets: List<LoggingStandardSet>
    ): LoggingWorkoutLift {
        return LoggingWorkoutLift(
            id = id,
            liftId = liftId,
            liftName = liftName,
            liftMovementPattern = MovementPattern.LEG_PUSH,
            liftVolumeTypes = 1,
            liftSecondaryVolumeTypes = null,
            note = null,
            position = 0,
            progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
            deloadWeek = null,
            incrementOverride = null,
            restTime = 90.seconds,
            restTimerEnabled = true,
            sets = sets,
            isCustom = false
        )
    }

    private fun createLoggingStandardSet(
        position: Int,
        complete: Boolean = false,
        completedReps: Int? = null,
        completedWeight: Float? = null,
        completedRpe: Float? = null,
    ): LoggingStandardSet {
        return LoggingStandardSet(
            position = position,
            rpeTarget = 8f,
            repRangeBottom = 8,
            repRangeTop = 12,
            weightRecommendation = 100f,
            hadInitialWeightRecommendation = true,
            previousSetResultLabel = "",
            repRangePlaceholder = "8-12",
            complete = complete,
            completedReps = completedReps,
            completedWeight = completedWeight,
            completedRpe = completedRpe
        )
    }

    @Test
    fun `invoke with partially completed set hydrates correctly`() {
        // Given
        val hydratingLifts = listOf(
            createLoggingWorkoutLift(
                id = 1, liftId = 101, liftName = "Squat",
                sets = listOf(
                    createLoggingStandardSet(position = 0, completedReps = 5),
                    createLoggingStandardSet(position = 1, complete = true, completedReps = 8, completedWeight = 100f, completedRpe = 8f)
                )
            )
        )
        val loggingWorkout = LoggingWorkout(
            id = 1, name = "Test Workout",
            lifts = listOf(
                createLoggingWorkoutLift(
                    id = 1, liftId = 101, liftName = "Squat",
                    sets = listOf(
                        createLoggingStandardSet(position = 0),
                        createLoggingStandardSet(position = 1)
                    )
                )
            )
        )

        // When
        val result = useCase(loggingWorkout, hydratingLifts)

        // Then
        val resultLift = result.lifts.first()
        val partiallyCompletedSet = resultLift.sets.first() as LoggingStandardSet

        assertEquals(false, partiallyCompletedSet.complete)
        assertEquals(5, partiallyCompletedSet.completedReps)
        assertNull(partiallyCompletedSet.completedWeight)
        assertNull(partiallyCompletedSet.completedRpe)
    }

    @Test
    fun `Scenario 1 - Workout with No Partially Completed Sets`() {
        // GIVEN: A hydrating lift list where all sets are either fully complete or have no completion data
        val hydratingLifts = listOf(
            createLoggingWorkoutLift(
                id = 1, liftId = 101, liftName = "Squat",
                sets = listOf(
                    createLoggingStandardSet(position = 0, complete = true, completedReps = 5, completedWeight = 100f, completedRpe = 8f),
                    createLoggingStandardSet(position = 1)
                )
            )
        )
        val loggingWorkout = LoggingWorkout(
            id = 1, name = "Test Workout",
            lifts = listOf(
                createLoggingWorkoutLift(
                    id = 1, liftId = 101, liftName = "Squat",
                    sets = listOf(
                        createLoggingStandardSet(position = 0),
                        createLoggingStandardSet(position = 1)
                    )
                )
            )
        )

        // WHEN: The use case is invoked with this workout
        val result = useCase(loggingWorkout, hydratingLifts)

        // THEN: Assert that the returned LoggingWorkout object is a new, hydrated object
        val resultSet = result.lifts.first().sets.first() as LoggingStandardSet
        assert(resultSet.complete)
        assertEquals(resultSet.completedReps, hydratingLifts[0].sets[0].completedReps)
        assertEquals(resultSet.completedWeight, hydratingLifts[0].sets[0].completedWeight)
        assertEquals(resultSet.completedRpe, hydratingLifts[0].sets[0].completedRpe)
    }

    @Test
    fun `Scenario 2 - Partially Completed Standard Set`() {
        // GIVEN: A hydrating lift with a single LoggingStandardSet where complete = false, but completedReps = 8
        val hydratingLifts = listOf(
            createLoggingWorkoutLift(
                id = 1, liftId = 101, liftName = "Squat",
                sets = listOf(
                    createLoggingStandardSet(position = 0, completedReps = 8)
                )
            )
        )
        val loggingWorkout = LoggingWorkout(
            id = 1, name = "Test Workout",
            lifts = listOf(
                createLoggingWorkoutLift(
                    id = 1, liftId = 101, liftName = "Squat",
                    sets = listOf(createLoggingStandardSet(position = 0))
                )
            )
        )

        // WHEN: The use case is invoked
        val result = useCase(loggingWorkout, hydratingLifts)

        // THEN: Assert that the returned workout is a new object and hydrated
        assertNotEquals(loggingWorkout, result)
        val resultSet = result.lifts.first().sets.first() as LoggingStandardSet
        assertEquals(false, resultSet.complete)
        assertEquals(8, resultSet.completedReps)
        assertNull(resultSet.completedWeight)
        assertNull(resultSet.completedRpe)
    }

    @Test
    fun `Scenario 3 - A Previously Completed Set is Un-checked by the User`() {
        // GIVEN: A hydrating lift where a set was previously saved as complete = true and now is not
        val hydratingLifts = listOf(
            createLoggingWorkoutLift(
                id = 1, liftId = 101, liftName = "Squat",
                sets = listOf(
                    createLoggingStandardSet(position = 0, complete = false, completedReps = 10, completedWeight = 100.0f, completedRpe = 8f)
                )
            )
        )
        val loggingWorkout = LoggingWorkout(
            id = 1, name = "Test Workout",
            lifts = listOf(
                createLoggingWorkoutLift(
                    id = 1, liftId = 101, liftName = "Squat",
                    sets = listOf(createLoggingStandardSet(position = 0, complete = true, completedReps = 10, completedWeight = 100.0f, completedRpe = 8f))
                )
            )
        )

        // WHEN: The use case is invoked
        val result = useCase(loggingWorkout, hydratingLifts)

        // THEN: Assert that the use case correctly identifies this as a partially completed set
        assertNotEquals(loggingWorkout, result)
        val resultSet = result.lifts.first().sets.first() as LoggingStandardSet
        assertEquals(false, resultSet.complete)
        assertEquals(10, resultSet.completedReps)
        assertEquals(100.0f, resultSet.completedWeight)
        assertEquals(8f, resultSet.completedRpe)
    }
}
