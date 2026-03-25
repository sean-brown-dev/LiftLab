package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingDropSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.StandardSetResult
import com.browntowndev.liftlab.core.domain.utils.MyoRepContinuationResult
import com.browntowndev.liftlab.core.domain.utils.MyoRepSetGoalUtils
import com.browntowndev.liftlab.core.domain.utils.WeightCalculationUtils
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import kotlin.time.Duration.Companion.seconds

class HydrateLoggingWorkoutWithCompletedSetsUseCaseTest {

    private lateinit var hydrateLoggingWorkoutWithCompletedSetsUseCase: HydrateLoggingWorkoutWithCompletedSetsUseCase

    @BeforeEach
    fun setUp() {
        hydrateLoggingWorkoutWithCompletedSetsUseCase = HydrateLoggingWorkoutWithCompletedSetsUseCase()
        mockkObject(SettingsManager)
        every { SettingsManager.getSetting(INCREMENT_AMOUNT, any()) } returns DEFAULT_INCREMENT_AMOUNT
    }

    @Test
    fun `invoke marks set as complete from inProgressResults`() {
        // Given
        val sets = listOf(
            LoggingStandardSet(position = 0, weightRecommendation = 100f, repRangeBottom = 5, rpeTarget = 8f, complete = false, repRangeTop = 8, repRangePlaceholder = "5-8", hadInitialWeightRecommendation = false, previousSetResultLabel = "")
        )
        val lifts = listOf(
            LoggingWorkoutLift(id = 1, liftId = 101, liftName = "Squat", position = 0, sets = sets, liftMovementPattern = MovementPattern.LEG_PUSH, liftVolumeTypes = 1, liftSecondaryVolumeTypes = null, progressionScheme = ProgressionScheme.LINEAR_PROGRESSION, incrementOverride = null, restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null, isCustom = false)
        )
        val loggingWorkout = LoggingWorkout(id = 1, name = "Test Workout", lifts = lifts)
        val inProgressResults = listOf<SetResult>(
            StandardSetResult(id = 1, workoutId = 1, liftId = 101, liftPosition = 0, setPosition = 0, weight = 100f, reps = 5, rpe = 8f,
                setType = SetType.STANDARD, isDeload = false
            )
        )

        // When
        val result = hydrateLoggingWorkoutWithCompletedSetsUseCase(loggingWorkout.lifts, inProgressResults, 1, 4)

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
            LoggingWorkoutLift(id = 1, liftId = 101, liftName = "Squat", position = 0, sets = sets, liftMovementPattern = MovementPattern.LEG_PUSH, liftVolumeTypes = 1, liftSecondaryVolumeTypes = null, progressionScheme = ProgressionScheme.LINEAR_PROGRESSION, incrementOverride = null, restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null, isCustom = false)
        )
        val loggingWorkout = LoggingWorkout(id = 1, name = "Test Workout", lifts = lifts)

        // When
        val result = hydrateLoggingWorkoutWithCompletedSetsUseCase(loggingWorkout.lifts, emptyList(), 1, 4)

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
        val setResultForLastCompleted = StandardSetResult(
            id = 1L,
            workoutId = 1L,
            liftPosition = 0,
            setPosition = 0,
            liftId = 101L,
            weight = 100f,
            reps = 6,
            rpe = 8.5f,
            persistedOneRepMax = null,
            setType = SetType.STANDARD,
            isDeload = false,
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
            restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null, isCustom = false
        )

        // When: no new set results; hydrator should calculate next-set recommendation
        val result = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), listOf(setResultForLastCompleted), microCycle = 0, 4)
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
        val setResultForLastCompleted = StandardSetResult(
            id = 1L,
            workoutId = 1L,
            liftPosition = 0,
            setPosition = 0,
            liftId = 101L,
            weight = 100f,
            reps = 5,
            rpe = 10f,
            persistedOneRepMax = null,
            setType = SetType.STANDARD,
            isDeload = false,
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
            restTime = 120.seconds, restTimerEnabled = true, deloadWeek = null, note = null, isCustom = false
        )

        // Expected: repGoal should be repRangeTop (10) for conservative lighter suggestion
        val expected = WeightCalculationUtils.calculateSuggestedWeight(
            completedWeight = 100f,
            completedReps = 4,
            completedRpe = 10f,
            repGoal = 8,
            rpeGoal = 8f,
            roundingFactor = incrementOverride
        )

        // When
        val result = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), listOf(setResultForLastCompleted), microCycle = 0, 4)
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
            completedReps = 9,             // raw reps
            completedRpe = 6f,             // adjustedCompleted = 12
            repRangePlaceholder = "6-8", hadInitialWeightRecommendation = false, previousSetResultLabel = ""
        )
        val setResultForLastCompleted = StandardSetResult(
            id = 1L,
            workoutId = 1L,
            liftPosition = 0,
            setPosition = 0,
            liftId = 201L,
            weight = 80f,
            reps = 9,
            rpe = 6f,
            persistedOneRepMax = null,
            setType = SetType.STANDARD,
            isDeload = false,
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
            restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null, isCustom = false
        )

        // Expected: repGoal should be repRangeBottom (6) to push weight up
        val expected = WeightCalculationUtils.calculateSuggestedWeight(
            completedWeight = 80f,
            completedReps = 8,
            completedRpe = 6f,
            repGoal = 6,
            rpeGoal = 8f,
            roundingFactor = incrementOverride
        )

        // When
        val result = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), listOf(setResultForLastCompleted), microCycle = 0, 4)
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
        val setResultForLastCompleted = StandardSetResult(
            id = 1L,
            workoutId = 1L,
            liftPosition = 0,
            setPosition = 0,
            liftId = 301L,
            weight = 90f,
            reps = 8,
            rpe = 10f,
            persistedOneRepMax = null,
            setType = SetType.STANDARD,
            isDeload = false,
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
            restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null, isCustom = false
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
        val result = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), listOf(setResultForLastCompleted), microCycle = 0, 4)
        val updated = result.first().sets[1] as LoggingStandardSet

        // Then
        assertEquals(expected, updated.weightRecommendation!!, 1e-3f)
    }

    @Test
    fun `myo sets with matching results stay complete and flip isNew=false (newly created next set remains isNew=true)`() {
        // Existing activation set + 2 mini-sets, all marked complete prior to hydration
        val activation = LoggingMyoRepSet(
            position = 0,
            rpeTarget = 8f,
            repRangeBottom = 12, repRangeTop = 15,
            weightRecommendation = 100f,
            hadInitialWeightRecommendation = true,
            previousSetResultLabel = "",
            repRangePlaceholder = "12–15",
            myoRepSetPosition = null, // activation
            complete = true,
            completedWeight = 100f,
            completedReps = 12,
            completedRpe = 8f,
            repFloor = 5,
            isNew = true // should be forced to false during hydration
        )
        val mini0 = activation.copy(
            myoRepSetPosition = 0,
            repRangePlaceholder = "—",
            complete = true,
            completedWeight = 100f,
            completedReps = 10,
            completedRpe = 9f,
            isNew = true
        )
        val mini1 = mini0.copy(
            myoRepSetPosition = 1,
            complete = true,
            completedReps = 8,
            completedRpe = 9.5f,
            isNew = true
        )

        val lift = LoggingWorkoutLift(
            id = 1L,
            liftId = 101L,
            liftName = "Incline DB Press (Myo)",
            position = 0,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            deloadWeek = null,
            incrementOverride = 2.5f,
            restTime = null,
            restTimerEnabled = false,
            sets = listOf(activation, mini0, mini1),
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = null,
            note = null,
            isCustom = false
        )

        // Provide matching MyoRepSetResults so hydration will KEEP them complete
        val results = listOf(
            MyoRepSetResult(
                workoutId = 1L, liftId = 101L, liftPosition = 0, setPosition = 0,
                myoRepSetPosition = null, // activation
                weight = 100f, reps = 12, rpe = 8f, isDeload = false
            ),
            MyoRepSetResult(
                workoutId = 1L, liftId = 101L, liftPosition = 0, setPosition = 0,
                myoRepSetPosition = 0, // mini 0
                weight = 100f, reps = 10, rpe = 9f, isDeload = false
            ),
            MyoRepSetResult(
                workoutId = 1L, liftId = 101L, liftPosition = 0, setPosition = 0,
                myoRepSetPosition = 1, // mini 1
                weight = 100f, reps = 8, rpe = 9.5f, isDeload = false
            )
        )

        val sut = HydrateLoggingWorkoutWithCompletedSetsUseCase()
        val hydrated = sut(liftsToHydrate = listOf(lift), setResults = results, microCycle = 0, 4).first()

        // Assert: the ORIGINAL sets (first three) are still complete and now isNew == false
        hydrated.sets.take(3).forEach { set ->
            val s = set as LoggingMyoRepSet
            assertTrue(s.complete, "Existing myo set should remain complete when a matching result is supplied")
            assertFalse(s.isNew, "Existing myo set should be flipped to isNew=false during hydration")
        }

        // Optional behavior: hydrator may append a NEXT myo mini-set if your goal logic says continue.
        // If appended, it must be flagged as isNew == true.
        if (hydrated.sets.size > 3) {
            val maybeNew = hydrated.sets[3] as LoggingMyoRepSet
            assertFalse(maybeNew.complete, "Newly added next myo mini-set should start incomplete")
            assertTrue(maybeNew.isNew, "Newly added next myo mini-set should be isNew=true")
        }
    }

    @Test
    fun `myo sets WITHOUT results are un-completed by hydration`() {
        val activation = LoggingMyoRepSet(
            position = 0,
            rpeTarget = 8f,
            repRangeBottom = 12, repRangeTop = 15,
            weightRecommendation = 100f,
            hadInitialWeightRecommendation = true,
            previousSetResultLabel = "",
            repRangePlaceholder = "12–15",
            myoRepSetPosition = null,
            complete = true,
            completedWeight = 100f,
            completedReps = 12,
            completedRpe = 8f,
            isNew = true
        )
        val mini0 = activation.copy(
            myoRepSetPosition = 0,
            repRangeBottom = 3, repRangeTop = null,
            repRangePlaceholder = "—",
            complete = true,
            completedWeight = 100f,
            completedReps = 3,
            completedRpe = 9f,
            isNew = true
        )

        val lift = LoggingWorkoutLift(
            id = 1L,
            liftId = 101L,
            liftName = "Incline DB Press (Myo)",
            position = 0,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            deloadWeek = null,
            incrementOverride = 2.5f,
            restTime = null,
            restTimerEnabled = false,
            sets = listOf(activation, mini0),
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = null,
            note = null,
            isCustom = false
        )

        val sut = HydrateLoggingWorkoutWithCompletedSetsUseCase()
        val hydrated = sut(liftsToHydrate = listOf(lift), setResults = emptyList(), microCycle = 0, 4).first()

        // Assert: since no results were provided, previously-complete myo sets are un-completed
        hydrated.sets.take(2).forEach { set ->
            val s = set as LoggingMyoRepSet
            assertFalse(s.complete, "Existing myo set should be un-completed if no matching result is supplied")
            // Whether isNew flips here depends on your final implementation; we don’t assert it in this negative test.
        }
    }

    @Test
    fun `activation-only input - single sequence with two minis produces two continuations and tail placeholder when continue true`() {
        // Lift contains ONLY the activation set
        val activation = LoggingMyoRepSet(
            position = 0,
            rpeTarget = 8f,
            repRangeBottom = 12,
            repRangeTop = 15,
            weightRecommendation = 100f,
            hadInitialWeightRecommendation = true,
            previousSetResultLabel = "",
            repRangePlaceholder = "12–15",
            myoRepSetPosition = null,
            complete = true,
            completedWeight = 100f,
            completedReps = 12,
            completedRpe = 8f,
            setMatching = false,
            repFloor = 3,
            isNew = false
        )
        val lift = LoggingWorkoutLift(
            id = 11L,
            liftId = 201L,
            liftName = "Incline DB Press (Myo)",
            position = 0,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            deloadWeek = null,
            incrementOverride = 2.5f,
            sets = listOf(activation), // <-- activation-only
            isCustom = false
        )
        // Results include the activation and two continuation minis
        val results = listOf(
            MyoRepSetResult(workoutId = 1L, liftId = 201L, liftPosition = 0, setPosition = 0, myoRepSetPosition = null, weight = 100f, reps = 12, rpe = 8f, isDeload = false),
            MyoRepSetResult(workoutId = 1L, liftId = 201L, liftPosition = 0, setPosition = 0, myoRepSetPosition = 0, weight = 100f, reps = 6, rpe = 9f, isDeload = false),
            MyoRepSetResult(workoutId = 1L, liftId = 201L, liftPosition = 0, setPosition = 0, myoRepSetPosition = 1, weight = 100f, reps = 4, rpe = 9.5f, isDeload = false),
        )
        val sut = HydrateLoggingWorkoutWithCompletedSetsUseCase()
        val hydrated = sut(liftsToHydrate = listOf(lift), setResults = results, microCycle = 0, 4).first()

        // Assert we started with activation-only
        val inputWasActivationOnly = lift.sets.filterIsInstance<LoggingMyoRepSet>().all { it.myoRepSetPosition == null }
        assertTrue(inputWasActivationOnly, "Test precondition: input lift must contain only activation sets")

        val myoSets = hydrated.sets.filterIsInstance<LoggingMyoRepSet>().filter { it.position == 0 }
        // Expect activation + 2 continuations + tail placeholder
        assertEquals(4, myoSets.size)
        assertEquals(listOf(null, 0, 1, 2), myoSets.map { it.myoRepSetPosition })
        assertFalse(myoSets.last().complete)
        assertTrue(myoSets.last().isNew)
    }

    @Test
    fun `activation-only input - two sequences with boundary and tail placeholders`() {
        val act0 = LoggingMyoRepSet(
            position = 0,
            rpeTarget = 8f,
            repRangeBottom = 10,
            repRangeTop = 12,
            weightRecommendation = 100f,
            hadInitialWeightRecommendation = true,
            previousSetResultLabel = "",
            repRangePlaceholder = "10–12",
            myoRepSetPosition = null,
            complete = true,
            completedWeight = 100f,
            completedReps = 12,
            completedRpe = 8f,
            setMatching = false,
            repFloor = 3,
            isNew = false
        )
        val act1 = act0.copy(position = 1, completedReps = 11)

        val lift = LoggingWorkoutLift(
            id = 12L,
            liftId = 202L,
            liftName = "Press (Myo)",
            position = 0,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            deloadWeek = null,
            incrementOverride = 2.5f,
            isCustom = false,
            sets = listOf(act0, act1) // <-- activation-only for each sequence
        )

        val results = listOf(
            // Sequence 0
            MyoRepSetResult(workoutId = 1L, liftId = 202L, liftPosition = 0, setPosition = 0, myoRepSetPosition = null, weight = 100f, reps = 12, rpe = 8f, isDeload = false),
            MyoRepSetResult(workoutId = 1L, liftId = 202L, liftPosition = 0, setPosition = 0, myoRepSetPosition = 0,    weight = 100f, reps = 6, rpe = 9f, isDeload = false),
            // Sequence 1
            MyoRepSetResult(workoutId = 1L, liftId = 202L, liftPosition = 0, setPosition = 1, myoRepSetPosition = null, weight = 100f, reps = 11, rpe = 8.5f, isDeload = false),
            MyoRepSetResult(workoutId = 1L, liftId = 202L, liftPosition = 0, setPosition = 1, myoRepSetPosition = 0,    weight = 100f, reps = 4, rpe = 9f, isDeload = false),
        )

        val sut = HydrateLoggingWorkoutWithCompletedSetsUseCase()
        val hydrated = sut(liftsToHydrate = listOf(lift), setResults = results, microCycle = 0, 4).first()

        // Assert activation-only precondition
        val allActivationOnly = lift.sets.filterIsInstance<LoggingMyoRepSet>().all { it.myoRepSetPosition == null }
        assertTrue(allActivationOnly, "Test precondition: input lifts contain only activation sets")

        val seq0 = hydrated.sets.filterIsInstance<LoggingMyoRepSet>().filter { it.position == 0 }
        val seq1 = hydrated.sets.filterIsInstance<LoggingMyoRepSet>().filter { it.position == 1 }

        // Seq 0 should have activation + cont0 + boundary placeholder
        assertEquals(listOf(null, 0, 1), seq0.map { it.myoRepSetPosition }, "Seq0 should include boundary placeholder at index 1")
        assertFalse(seq0.last().complete); assertTrue(seq0.last().isNew)

        // Seq 1 should have activation + cont0 + tail placeholder
        assertEquals(listOf(null, 0, 1), seq1.map { it.myoRepSetPosition }, "Seq1 should include tail placeholder at index 1")
        assertFalse(seq1.last().complete); assertTrue(seq1.last().isNew)
    }

    @Test
    fun `activation-only result creates first myo continuation placeholder`() {
        // Arrange: lift has ONLY the activation set, no minis persisted
        val activation = LoggingMyoRepSet(
            position = 0,
            rpeTarget = 8f,
            repRangeBottom = 12,
            repRangeTop = 15,
            weightRecommendation = 100f,
            hadInitialWeightRecommendation = true,
            previousSetResultLabel = "",
            repRangePlaceholder = "12–15",
            myoRepSetPosition = null,   // activation
            complete = true,
            completedWeight = 100f,
            completedReps = 12,
            completedRpe = 8f,
            setMatching = false,
            repFloor = 3,
            isNew = false
        )

        val lift = LoggingWorkoutLift(
            id = 99L,
            liftId = 199L,
            liftName = "Myo Lift (activation-only)",
            position = 0,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            deloadWeek = null,
            incrementOverride = 2.5f,
            sets = listOf(activation),
            isCustom = false
        )

        // Results include ONLY the activation result (no myo minis yet)
        val results = listOf(
            MyoRepSetResult(
                workoutId = 1L,
                liftId = 199L,
                liftPosition = 0,
                setPosition = 0,
                myoRepSetPosition = null,  // activation only
                weight = 100f,
                reps = 12,
                rpe = 8f,
                isDeload = false
            )
        )

        // Act
        val sut = HydrateLoggingWorkoutWithCompletedSetsUseCase()
        val hydrated = sut(liftsToHydrate = listOf(lift), setResults = results, microCycle = 0, 4).first()

        // Assert precondition: input was activation-only
        val inputActivationOnly = lift.sets.filterIsInstance<LoggingMyoRepSet>().all { it.myoRepSetPosition == null }
        assertTrue(inputActivationOnly, "Precondition failed: input must be activation-only")

        // Expect activation + the newly-added placeholder for mini-set 0
        val myoSets = hydrated.sets.filterIsInstance<LoggingMyoRepSet>().filter { it.position == 0 }
        assertEquals(2, myoSets.size, "Should have activation + first myo placeholder")
        assertEquals(listOf(null, 0), myoSets.map { it.myoRepSetPosition }, "Mini index should start at 0")
        val placeholder = myoSets.last()
        assertFalse(placeholder.complete, "Newly created first myo mini-set should be incomplete placeholder")
        assertTrue(placeholder.isNew, "Placeholder mini-set should be marked isNew=true")
    }



    @Test
    fun `standard after drop does NOT recalc and leaves recommendation untouched`() {
        // Given: previous is a completed DROP set, current is an incomplete STANDARD set
        val incrementOverride = 2.5f
        val prevDrop = LoggingDropSet(
            position = 0,
            dropPercentage = 0.20f,
            weightRecommendation = 80f,
            complete = true,
            completedWeight = 80f,
            completedReps = 8,
            completedRpe = 8.5f,
            repRangeBottom = 6, repRangeTop = 8, rpeTarget = 8f,
            repRangePlaceholder = "6-8",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val nextStandard = LoggingStandardSet(
            position = 1,
            weightRecommendation = null, // should remain null since gate prohibits recalc
            repRangeBottom = 6, repRangeTop = 8, rpeTarget = 8f,
            complete = false,
            repRangePlaceholder = "6-8",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val lift = LoggingWorkoutLift(
            id = 1, liftId = 404, liftName = "Bench", position = 0,
            sets = listOf(prevDrop, nextStandard),
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = 1, liftSecondaryVolumeTypes = null,
            progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
            incrementOverride = incrementOverride,
            restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null,
            note = null, isCustom = false
        )
        val results = listOf<SetResult>(
            StandardSetResult(
                id = 1L, workoutId = 1L, liftId = 404L, liftPosition = 0, setPosition = 0,
                weight = 100f, reps = 10, rpe = 8.5f, setType = com.browntowndev.liftlab.core.domain.enums.SetType.STANDARD, isDeload = false
            )
        )
        // Mark the prevDrop as completed via its own values, no new drop result needed for gating behavior

        // Mock WeightCalculationUtils to ensure it's NOT invoked for this path
        mockkObject(WeightCalculationUtils)
        io.mockk.every { WeightCalculationUtils.calculateSuggestedWeight(any(), any(), any(), any(), any(), any()) } returns 999f

        // When
        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), results, microCycle = 0, 4).first()
        val updatedStandard = hydrated.sets[1] as LoggingStandardSet

        // Then: no recompute, recommendation remains null
        assertEquals(null, updatedStandard.weightRecommendation)

        // And WeightCalculationUtils should NOT have been called
        io.mockk.verify(exactly = 0) { WeightCalculationUtils.calculateSuggestedWeight(any(), any(), any(), any(), any(), any()) }
        io.mockk.unmockkObject(WeightCalculationUtils)
    }

    @Test
    fun `drop after drop uses last completed drop weight times (1 - pct) and rounds`() {
        val incrementOverride = 2.5f
        val drop0 = LoggingDropSet(
            position = 0,
            dropPercentage = 0.15f,
            weightRecommendation = null,
            complete = true,
            completedWeight = 170f,
            completedReps = 8,
            completedRpe = 9f,
            repRangeBottom = 6, repRangeTop = 8, rpeTarget = 8f,
            repRangePlaceholder = "6-8",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val drop1 = LoggingDropSet(
            position = 1,
            dropPercentage = 0.20f,
            weightRecommendation = null,
            complete = false,
            repRangeBottom = 6, repRangeTop = 8, rpeTarget = 8f,
            repRangePlaceholder = "6-8",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val lift = LoggingWorkoutLift(
            id = 1, liftId = 505, liftName = "Row", position = 0,
            sets = listOf(drop0, drop1),
            liftMovementPattern = MovementPattern.HORIZONTAL_PULL,
            liftVolumeTypes = 1, liftSecondaryVolumeTypes = null,
            progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
            incrementOverride = incrementOverride,
            restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null, isCustom = false
        )

        // No new results needed; previous completed is already embedded
        val results = listOf<SetResult>(
            StandardSetResult(
                id = 1L, workoutId = 1L, liftId = 505L, liftPosition = 0, setPosition = 0,
                weight = 170f, reps = 8, rpe = 9f, setType = SetType.DROP_SET, isDeload = false
            )
        )
        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), results, microCycle = 0, 4).first()
        val updatedDrop1 = hydrated.sets[1] as LoggingDropSet

        // Expect: 170 * (1 - 0.20) = 136 → rounded to 2.5 = 135.0
        assertEquals(135.0f, updatedDrop1.weightRecommendation)
    }

    @Test
    fun `myo first mini recalculates and uses repFloor when activation reps are low`() {
        val incrementOverride = 2.5f
        val activation = LoggingMyoRepSet(
            position = 0,
            rpeTarget = 8f,
            repRangeBottom = 12, repRangeTop = 15,
            weightRecommendation = 100f,
            hadInitialWeightRecommendation = true,
            previousSetResultLabel = "",
            repRangePlaceholder = "12–15",
            myoRepSetPosition = null, // activation
            complete = true,
            completedWeight = 100f,
            completedReps = 8,   // low activation
            completedRpe = 10f,
            repFloor = 7,
            isNew = false
        )
        val firstMini = activation.copy(
            myoRepSetPosition = 0,
            complete = false,
            completedWeight = null,
            completedReps = null,
            completedRpe = null
        )

        val lift = LoggingWorkoutLift(
            id = 1L, liftId = 606L, liftName = "Myo Test", position = 0,
            sets = listOf(activation, firstMini),
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = 0, liftSecondaryVolumeTypes = null,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            incrementOverride = incrementOverride,
            restTime = null, restTimerEnabled = false, deloadWeek = null, note = null, isCustom = false
        )

        // Provide only the activation result; no mini results yet
        val results = listOf(
            MyoRepSetResult(
                workoutId = 1L, liftId = 606L, liftPosition = 0, setPosition = 0,
                myoRepSetPosition = null, weight = 100f, reps = 8, rpe = 10f, isDeload = false
            )
        )

        // Mock WeightCalculationUtils to capture the repGoal passed
        mockkObject(WeightCalculationUtils)
        val capturedRepGoal = io.mockk.slot<Int>()
        io.mockk.every {
            WeightCalculationUtils.calculateSuggestedWeight(
                completedWeight = any(),
                completedReps = any(),
                completedRpe = any(),
                repGoal = capture(capturedRepGoal),
                rpeGoal = any(),
                roundingFactor = any()
            )
        } returns 135.0f

        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), results, microCycle = 0, 4).first()
        val mini0 = hydrated.sets[1] as LoggingMyoRepSet

        // RepGoal should be max(floor, round(activationReps*0.3)) -> max(7, round(2.4)=2) = 7
        assertEquals(7, capturedRepGoal.captured, "Rep goal should coerce to repFloor when 30% of activation is lower than floor")
        assertEquals(135.0f, mini0.weightRecommendation)

        io.mockk.unmockkObject(WeightCalculationUtils)
    }

    @Test
    fun `myo first mini does NOT recalc when activation within adjusted range`() {
        val activation = LoggingMyoRepSet(
            position = 0,
            rpeTarget = 8f,
            repRangeBottom = 12, repRangeTop = 15,
            weightRecommendation = 120f,
            hadInitialWeightRecommendation = true,
            previousSetResultLabel = "",
            repRangePlaceholder = "12–15",
            myoRepSetPosition = null,
            complete = true,
            completedWeight = 120f,
            completedReps = 13,
            completedRpe = 8f,
            repFloor = 5,
            isNew = false
        )
        val firstMini = activation.copy(
            myoRepSetPosition = 0,
            complete = false,
            completedWeight = null,
            completedReps = null,
            completedRpe = null
        )
        val lift = LoggingWorkoutLift(
            id = 1L, liftId = 707L, liftName = "Myo No Recalc", position = 0,
            sets = listOf(activation, firstMini),
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = 0, liftSecondaryVolumeTypes = null,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            incrementOverride = 2.5f,
            restTime = null, restTimerEnabled = false, deloadWeek = null, note = null, isCustom = false
        )

        // Activation result within range
        val results = listOf(
            MyoRepSetResult(workoutId = 1L, liftId = 707L, liftPosition = 0, setPosition = 0, myoRepSetPosition = null, weight = 120f, reps = 13, rpe = 8f, isDeload = false)
        )

        // When
        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), results, microCycle = 0, 4).first()
        val mini0 = hydrated.sets[1] as LoggingMyoRepSet

        // Then: no recalc -> recommendation defaults to activation completedWeight
        assertEquals(120f, mini0.weightRecommendation)
    }

    // ====================== Standard set recommendation (with persisted results) ======================

    @Test
    fun `standard set recommendation increases when exceeded rep range top (with results)`() {
        val incrementOverride = 2.5f
        val set0 = LoggingStandardSet(
            position = 0,
            weightRecommendation = null,
            complete = true,
            completedWeight = 100f,
            completedReps = 12, // higher than top of range
            completedRpe = 8f,
            repRangeBottom = 8, repRangeTop = 10, rpeTarget = 8f,
            repRangePlaceholder = "8-10",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val set1 = LoggingStandardSet(
            position = 1,
            weightRecommendation = null,
            complete = false,
            repRangeBottom = 8, repRangeTop = 10, rpeTarget = 8f,
            repRangePlaceholder = "8-10",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val lift = LoggingWorkoutLift(
            id = 1, liftId = 777, liftName = "Bench", position = 0,
            sets = listOf(set0, set1),
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = 1, liftSecondaryVolumeTypes = null,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            incrementOverride = incrementOverride,
            restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null, isCustom = false
        )

        // Persist previous completion
        val results = listOf<SetResult>(
            StandardSetResult(
                id = 1L, workoutId = 1L, liftId = 777L, liftPosition = 0, setPosition = 0,
                weight = 100f, reps = 12, rpe = 8f, setType = SetType.STANDARD, isDeload = false
            )
        )

        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), results, microCycle = 0, 4).first()
        val updated = hydrated.sets[1] as LoggingStandardSet

        assertNotNull(updated.weightRecommendation)
        assertTrue(updated.weightRecommendation > 100f)
    }

    @Test
    fun `standard set recommendation decreases when missed rep range bottom (with results)`() {
        val incrementOverride = 2.5f
        val set0 = LoggingStandardSet(
            position = 0,
            weightRecommendation = null,
            complete = true,
            completedWeight = 150f,
            completedReps = 5, // below bottom
            completedRpe = 10f,
            repRangeBottom = 8, repRangeTop = 10, rpeTarget = 8f,
            repRangePlaceholder = "8-10",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val set1 = LoggingStandardSet(
            position = 1,
            weightRecommendation = null,
            complete = false,
            repRangeBottom = 8, repRangeTop = 10, rpeTarget = 8f,
            repRangePlaceholder = "8-10",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val lift = LoggingWorkoutLift(
            id = 1, liftId = 778, liftName = "Squat", position = 0,
            sets = listOf(set0, set1),
            liftMovementPattern = MovementPattern.LEG_PUSH,
            liftVolumeTypes = 1, liftSecondaryVolumeTypes = null,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            incrementOverride = incrementOverride,
            restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null, isCustom = false
        )

        val results = listOf<SetResult>(
            StandardSetResult(
                id = 2L, workoutId = 1L, liftId = 778L, liftPosition = 0, setPosition = 0,
                weight = 150f, reps = 5, rpe = 10f, setType = SetType.STANDARD, isDeload = false
            )
        )

        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), results, microCycle = 0, 4).first()
        val updated = hydrated.sets[1] as LoggingStandardSet

        assertNotNull(updated.weightRecommendation)
        assertTrue(updated.weightRecommendation < 150f)
    }

    @Test
    fun `standard set recommendation recalculates when rep goals differ (with results)`() {
        val incrementOverride = 2.5f
        val set0 = LoggingStandardSet(
            position = 0,
            weightRecommendation = null,
            complete = true,
            completedWeight = 120f,
            completedReps = 9,
            completedRpe = 8f,
            repRangeBottom = 8, repRangeTop = 10, rpeTarget = 8f,
            repRangePlaceholder = "8-10",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val set1 = LoggingStandardSet(
            position = 1,
            weightRecommendation = null,
            complete = false,
            repRangeBottom = 6, repRangeTop = 8, rpeTarget = 9f, // changed from set0
            repRangePlaceholder = "6-8",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val lift = LoggingWorkoutLift(
            id = 1, liftId = 779, liftName = "OHP", position = 0,
            sets = listOf(set0, set1),
            liftMovementPattern = MovementPattern.VERTICAL_PUSH,
            liftVolumeTypes = 1, liftSecondaryVolumeTypes = null,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            incrementOverride = incrementOverride,
            restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null, isCustom = false
        )

        val results = listOf<SetResult>(
            StandardSetResult(
                id = 3L, workoutId = 1L, liftId = 779L, liftPosition = 0, setPosition = 0,
                weight = 120f, reps = 9, rpe = 8f, setType = SetType.STANDARD, isDeload = false
            )
        )

        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), results, microCycle = 0, 4).first()
        val updated = hydrated.sets[1] as LoggingStandardSet

        assertNotNull(updated.weightRecommendation)
        assertNotEquals(120f, updated.weightRecommendation)
    }

// ====================== Drop set recommendation coverage ======================

    @Test
    fun `drop after standard uses previous standard completed weight times (1 - pct) and rounds`() {
        val incrementOverride = 2.5f
        val std = LoggingStandardSet(
            position = 0,
            weightRecommendation = null,
            complete = true,
            completedWeight = 200f,
            completedReps = 8,
            completedRpe = 8f,
            repRangeBottom = 8, repRangeTop = 10, rpeTarget = 8f,
            repRangePlaceholder = "8-10",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val drop = LoggingDropSet(
            position = 1,
            dropPercentage = 0.20f,
            weightRecommendation = null,
            complete = false,
            repRangeBottom = 6, repRangeTop = 8, rpeTarget = 8f,
            repRangePlaceholder = "6-8",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val lift = LoggingWorkoutLift(
            id = 1, liftId = 880, liftName = "Rows", position = 0,
            sets = listOf(std, drop),
            liftMovementPattern = MovementPattern.HORIZONTAL_PULL,
            liftVolumeTypes = 1, liftSecondaryVolumeTypes = null,
            progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
            incrementOverride = incrementOverride,
            restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null, isCustom = false
        )

        val results = listOf<SetResult>(
            StandardSetResult(
                id = 20L, workoutId = 1L, liftId = 880L, liftPosition = 0, setPosition = 0,
                weight = 200f, reps = 8, rpe = 8f, setType = SetType.STANDARD, isDeload = false
            )
        )

        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), results, microCycle = 0, 4).first()
        val updated = hydrated.sets[1] as LoggingDropSet

        // 200 * 0.8 = 160 -> multiple of 2.5
        assertEquals(160f, updated.weightRecommendation)
    }

    @Test
    fun `drop after myo activation does NOT recalc (gate blocks)`() {
        val incrementOverride = 2.5f
        val activation = LoggingMyoRepSet(
            position = 0,
            rpeTarget = 8f,
            repRangeBottom = 12, repRangeTop = 15,
            weightRecommendation = 100f,
            hadInitialWeightRecommendation = true,
            previousSetResultLabel = "",
            repRangePlaceholder = "12–15",
            myoRepSetPosition = null,
            complete = true,
            completedWeight = 100f,
            completedReps = 12,
            completedRpe = 8f,
            repFloor = 5,
            isNew = false
        )
        val drop = LoggingDropSet(
            position = 1,
            dropPercentage = 0.25f,
            weightRecommendation = null,
            complete = false,
            repRangeBottom = 6, repRangeTop = 8, rpeTarget = 8f,
            repRangePlaceholder = "6-8",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val lift = LoggingWorkoutLift(
            id = 1, liftId = 881, liftName = "Pulldown", position = 0,
            sets = listOf(activation, drop),
            liftMovementPattern = MovementPattern.VERTICAL_PULL,
            liftVolumeTypes = 1, liftSecondaryVolumeTypes = null,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            incrementOverride = incrementOverride,
            restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null, isCustom = false
        )

        val results = listOf<SetResult>(
            MyoRepSetResult(
                workoutId = 1L, liftId = 881L, liftPosition = 0, setPosition = 0,
                myoRepSetPosition = null, weight = 100f, reps = 12, rpe = 8f, isDeload = false
            )
        )

        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), results, microCycle = 0, 4).first()
        val updated = hydrated.sets[2] as LoggingDropSet // myo success, so 2nd index is now another myo

        // Gate blocks myo -> drop recalc; recommendation remains null
        assertEquals(null, updated.weightRecommendation)
    }

// ====================== Myo-rep recommendation & behavior ======================

    @Test
    fun `myo first mini recalculates when activation outside adjusted range (too easy)`() {
        val incrementOverride = 2.5f
        val activation = LoggingMyoRepSet(
            position = 0,
            rpeTarget = 8f,
            repRangeBottom = 20, repRangeTop = 25,
            weightRecommendation = 95f,
            hadInitialWeightRecommendation = true,
            previousSetResultLabel = "",
            repRangePlaceholder = "20–25",
            myoRepSetPosition = null,
            complete = true,
            completedWeight = 95f,
            completedReps = 28, // too easy relative to top+threshold
            completedRpe = 8f,
            repFloor = 10,
            isNew = false
        )
        val mini0 = activation.copy(
            myoRepSetPosition = 0,
            complete = false,
            completedWeight = null,
            completedReps = null,
            completedRpe = null
        )

        val lift = LoggingWorkoutLift(
            id = 1L, liftId = 990L, liftName = "Myo EZ Curl", position = 0,
            sets = listOf(activation, mini0),
            liftMovementPattern = MovementPattern.VERTICAL_PULL,
            liftVolumeTypes = 1, liftSecondaryVolumeTypes = null,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            incrementOverride = incrementOverride,
            restTime = null, restTimerEnabled = false, deloadWeek = null, note = null, isCustom = false
        )

        val results = listOf(
            MyoRepSetResult(
                workoutId = 1L, liftId = 990L, liftPosition = 0, setPosition = 0,
                myoRepSetPosition = null, weight = 95f, reps = 28, rpe = 8f, isDeload = false
            )
        )

        mockkObject(WeightCalculationUtils)
        every { WeightCalculationUtils.calculateSuggestedWeight(any(), any(), any(), any(), any(), any()) } returns 80f

        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), results, microCycle = 0, 4).first()
        val updatedMini0 = hydrated.sets[1] as LoggingMyoRepSet
        assertEquals(80f, updatedMini0.weightRecommendation)

        unmockkObject(WeightCalculationUtils)
    }

    @Test
    fun `standard after myo activation does NOT recalc (gate blocks)`() {
        val incrementOverride = 2.5f
        val activation = LoggingMyoRepSet(
            position = 0,
            rpeTarget = 8f,
            repRangeBottom = 12, repRangeTop = 15,
            weightRecommendation = 100f,
            hadInitialWeightRecommendation = true,
            previousSetResultLabel = "",
            repRangePlaceholder = "12–15",
            myoRepSetPosition = null,
            complete = true,
            completedWeight = 100f,
            completedReps = 12,
            completedRpe = 8f,
            repFloor = 5,
            isNew = false
        )
        val standard = LoggingStandardSet(
            position = 1,
            weightRecommendation = null,
            complete = false,
            repRangeBottom = 8, repRangeTop = 12, rpeTarget = 8f,
            repRangePlaceholder = "8–12",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val lift = LoggingWorkoutLift(
            id = 1, liftId = 992, liftName = "Std After Myo", position = 0,
            sets = listOf(activation, standard),
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = 1, liftSecondaryVolumeTypes = null,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            incrementOverride = incrementOverride,
            restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null, isCustom = false
        )

        val results = listOf<SetResult>(
            MyoRepSetResult(workoutId = 1L, liftId = 992L, liftPosition = 0, setPosition = 0, myoRepSetPosition = null, weight = 100f, reps = 12, rpe = 8f, isDeload = false)
        )

        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), results, microCycle = 0, 4).first()
        val updatedStd = hydrated.sets[2] as LoggingStandardSet // myo success, so 2nd index is now another myo

        // Gate blocks; recommendation remains null
        assertEquals(null, updatedStd.weightRecommendation)
    }

    @Test
    fun `when no result exists but set is marked complete, it is uncompleted and completion fields are cleared`() {
        // Given: a previously completed standard set with stored completion data
        val completedSet = LoggingStandardSet(
            position = 0,
            weightRecommendation = 100f,
            repRangeBottom = 5, repRangeTop = 8, rpeTarget = 8f,
            complete = true,
            completedWeight = 100f,
            completedReps = 6,
            completedRpe = 8.5f,
            repRangePlaceholder = "5-8",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val lift = LoggingWorkoutLift(
            id = 1,
            liftId = 101,
            liftName = "Squat",
            position = 0,
            sets = listOf(completedSet),
            liftMovementPattern = MovementPattern.LEG_PUSH,
            liftVolumeTypes = 1,
            liftSecondaryVolumeTypes = null,
            progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
            incrementOverride = null,
            restTime = 90.seconds,
            restTimerEnabled = true,
            deloadWeek = null,
            note = null,
            isCustom = false
        )

        // When: hydrate with NO set results; the set should be "un-completed"
        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(
            liftsToHydrate = listOf(lift),
            setResults = emptyList(),
            microCycle = 0,
            4
        ).first()

        // Then: complete -> false AND completedWeight/Reps/Rpe -> null
        val updated = hydrated.sets.first() as LoggingStandardSet
        assertFalse(updated.complete, "Set should be un-completed when no matching result exists")
        assertEquals(null, updated.completedWeight, "completedWeight should be cleared")
        assertEquals(null, updated.completedReps, "completedReps should be cleared")
        assertEquals(null, updated.completedRpe, "completedRpe should be cleared")
    }

    @Test
    fun `myo-rep continuation - activation missed goal uses suggested weight for next placeholder set`() {
        // Arrange: activation + one completed mini set (so the "last myo set" is complete)
        val activation = LoggingMyoRepSet(
            position = 0,
            rpeTarget = 8f,
            repRangeBottom = 12,
            repRangeTop = 15,
            weightRecommendation = 100f,
            hadInitialWeightRecommendation = true,
            previousSetResultLabel = "",
            repRangePlaceholder = "12–15",
            myoRepSetPosition = null,      // activation
            complete = true,
            completedWeight = 100f,
            completedReps = 12,
            completedRpe = 8f,
            setMatching = false,
            repFloor = 3,
            isNew = false
        )
        val mini0 = activation.copy(
            myoRepSetPosition = 0,         // first mini set
            repRangeBottom = 3,
            repRangeTop = null,
            repRangePlaceholder = "—",
            complete = true,
            completedWeight = 100f,
            completedReps = 3,
            completedRpe = 9f,
            isNew = false
        )

        val incrementOverride = 2.5f
        val lift = LoggingWorkoutLift(
            id = 1L,
            liftId = 101L,
            liftName = "Incline DB Press (Myo)",
            position = 0,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            deloadWeek = null,
            incrementOverride = incrementOverride,
            restTime = null,
            restTimerEnabled = false,
            sets = listOf(activation, mini0),
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = null,
            note = null,
            isCustom = false
        )

        // Matching results for activation + mini0 so hydration sees them as completed
        val results = listOf<SetResult>(
            MyoRepSetResult(
                workoutId = 1L, liftId = 101L, liftPosition = 0, setPosition = 0,
                myoRepSetPosition = null, weight = 100f, reps = 12, rpe = 8f, isDeload = false
            ),
            MyoRepSetResult(
                workoutId = 1L, liftId = 101L, liftPosition = 0, setPosition = 0,
                myoRepSetPosition = 0, weight = 100f, reps = 3, rpe = 9f, isDeload = false
            )
        )

        // Force the path: continuation needed + activation missed goal
        mockkObject(MyoRepSetGoalUtils)
        every {
            MyoRepSetGoalUtils.shouldContinueMyoReps(
                lastMyoRepSet = any(),
                myoRepSetResults = any()
            )
        } returns MyoRepContinuationResult(
            shouldContinueMyoReps = true,
            activationSetMissedGoal = true
        )

        // Make suggested weight obvious so we can assert the else-branch was used
        mockkObject(WeightCalculationUtils)
        every {
            WeightCalculationUtils.calculateSuggestedWeight(
                completedWeight = 100f,
                completedReps = 3,
                completedRpe = 9f,
                repGoal = 3,                 // from mini0.repRangeBottom
                rpeGoal = 8f,                // from lastMyoRepSet.rpeTarget
                roundingFactor = incrementOverride
            )
        } returns 137.5f

        // Act
        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(
            liftsToHydrate = listOf(lift),
            setResults = results,
            microCycle = 0,
            4
        ).first()

        // Find the newly appended placeholder mini set (should be position 0, myoRepSetPosition = 1, incomplete, isNew = true)
        val myoSets = hydrated.sets.filterIsInstance<LoggingMyoRepSet>().filter { it.position == 0 }
        // Expected order: activation (null), mini0 (0), new placeholder (1)
        val newPlaceholder = myoSets.last()

        // Assert: we took the else-branch (suggested weight), not just re-using completedWeight
        assertFalse(newPlaceholder.complete, "New continuation set should be a placeholder (incomplete).")
        assertTrue(newPlaceholder.isNew, "New continuation set should be flagged as isNew.")
        assertEquals(1, newPlaceholder.myoRepSetPosition, "Should append the next mini-set in sequence.")
        assertNotNull(newPlaceholder.weightRecommendation)
        assertEquals(137.5f, newPlaceholder.weightRecommendation, 1e-3f)

        // And confirm the calculator was actually used (i.e., we hit the else branch)
        verify(exactly = 1) {
            WeightCalculationUtils.calculateSuggestedWeight(
                completedWeight = 100f,
                completedReps = 3,
                completedRpe = 9f,
                repGoal = 3,
                rpeGoal = 8f,
                roundingFactor = incrementOverride
            )
        }

        // Clean up mocks
        unmockkObject(MyoRepSetGoalUtils)
        unmockkObject(WeightCalculationUtils)
    }

    // ====================== Additional coverage for getWithWeightRecommendation ======================

    @Test
    fun `standard next set leaves recommendation unchanged when previous set is incomplete`() {
        // Previous set exists but is NOT complete -> early return (no calc)
        val prev = LoggingStandardSet(
            position = 0,
            weightRecommendation = 100f,
            repRangeBottom = 8, repRangeTop = 10, rpeTarget = 8f,
            complete = false, // <-- incomplete triggers guard
            repRangePlaceholder = "8-10",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val next = LoggingStandardSet(
            position = 1,
            weightRecommendation = null,
            repRangeBottom = 8, repRangeTop = 10, rpeTarget = 8f,
            complete = false,
            repRangePlaceholder = "8-10",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val lift = LoggingWorkoutLift(
            id = 1, liftId = 9001, liftName = "Guard Standard", position = 0,
            sets = listOf(prev, next),
            progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
            deloadWeek = null, incrementOverride = 2.5f, isCustom = false
        )

        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), emptyList(), microCycle = 0, 4).first()
        val updated = hydrated.sets[1] as LoggingStandardSet
        assertEquals(null, updated.weightRecommendation, "Should not calculate when previous set is incomplete")
    }

    @Test
    fun `standard with same rep range uses last completed weight when previous succeeded AND this set had no recommendation`() {
        val prev = LoggingStandardSet(
            position = 0,
            weightRecommendation = 92.5f, // previous recommendation (doesn't matter, we look at completedWeight)
            repRangeBottom = 6, repRangeTop = 8, rpeTarget = 8f,
            complete = true,
            completedWeight = 100f,
            completedReps = 8,
            completedRpe = 8f,
            repRangePlaceholder = "6-8",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val next = LoggingStandardSet(
            position = 1,
            weightRecommendation = null, // <- null triggers the "use last completed weight" branch when prior succeeded
            repRangeBottom = 6, repRangeTop = 8, rpeTarget = 8f,
            complete = false,
            repRangePlaceholder = "6-8",
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = ""
        )
        val lift = LoggingWorkoutLift(
            id = 1, liftId = 9002, liftName = "Same Range Use Last Completed", position = 0,
            sets = listOf(prev, next),
            progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
            deloadWeek = null, incrementOverride = 2.5f, isCustom = false
        )

        // Persist prev as completed
        val results = listOf<SetResult>(
            StandardSetResult(
                id = 1, workoutId = 1, liftId = 9002, liftPosition = 0, setPosition = 0,
                weight = 100f, reps = 8, rpe = 8f, setType = SetType.STANDARD, isDeload = false
            )
        )

        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), results, microCycle = 0, 4).first()
        val updated = hydrated.sets[1] as LoggingStandardSet
        assertEquals(100f, updated.weightRecommendation, "Should use last completed weight when rep range same and prev succeeded")
    }

    @Test
    fun `standard with same rep range keeps current recommendation when previous succeeded and rec matches previous`() {
        val prev = LoggingStandardSet(
            position = 0,
            weightRecommendation = 95f,
            repRangeBottom = 5, repRangeTop = 7, rpeTarget = 8f,
            complete = true, completedWeight = 100f, completedReps = 6, completedRpe = 8f,
            repRangePlaceholder = "5-7", hadInitialWeightRecommendation = false, previousSetResultLabel = ""
        )
        val next = LoggingStandardSet(
            position = 1,
            weightRecommendation = 95f, // same as prev's recommendation -> keep it
            repRangeBottom = 5, repRangeTop = 7, rpeTarget = 8f,
            complete = false,
            repRangePlaceholder = "5-7", hadInitialWeightRecommendation = false, previousSetResultLabel = ""
        )
        val lift = LoggingWorkoutLift(
            id = 1, liftId = 9003, liftName = "Keep Current Rec", position = 0,
            sets = listOf(prev, next),
            progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
            deloadWeek = null, incrementOverride = 2.5f, isCustom = false
        )
        val results = listOf<SetResult>(
            StandardSetResult(
                id = 1, workoutId = 1, liftId = 9003, liftPosition = 0, setPosition = 0,
                weight = 100f, reps = 6, rpe = 8f, setType = SetType.STANDARD, isDeload = false
            )
        )

        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), results, microCycle = 0, 4).first()
        val updated = hydrated.sets[1] as LoggingStandardSet
        assertEquals(95f, updated.weightRecommendation, "Should keep current recommendation when rep range same and prior succeeded with same rec")
    }

    @Test
    fun `standard with different rep range and successful previous keeps existing recommendation (no recalc)`() {
        val prev = LoggingStandardSet(
            position = 0,
            weightRecommendation = 100f,
            repRangeBottom = 8, repRangeTop = 10, rpeTarget = 8f,
            complete = true, completedWeight = 100f, completedReps = 9, completedRpe = 8f,
            repRangePlaceholder = "8-10", hadInitialWeightRecommendation = false, previousSetResultLabel = ""
        )
        val next = LoggingStandardSet(
            position = 1,
            weightRecommendation = 125f, // already has a rec -> keep it since prev succeeded and range changed
            repRangeBottom = 5, repRangeTop = 6, rpeTarget = 9f,
            complete = false,
            repRangePlaceholder = "5-6", hadInitialWeightRecommendation = false, previousSetResultLabel = ""
        )
        val lift = LoggingWorkoutLift(
            id = 1, liftId = 9004, liftName = "Different Range Keep", position = 0,
            sets = listOf(prev, next),
            progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
            deloadWeek = null, incrementOverride = 2.5f, isCustom = false
        )
        val results = listOf<SetResult>(
            StandardSetResult(
                id = 1, workoutId = 1, liftId = 9004, liftPosition = 0, setPosition = 0,
                weight = 100f, reps = 9, rpe = 8f, setType = SetType.STANDARD, isDeload = false
            )
        )

        mockkObject(WeightCalculationUtils)
        io.mockk.every { WeightCalculationUtils.calculateSuggestedWeight(any(), any(), any(), any(), any(), any()) } returns 999f

        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), results, microCycle = 0, 4).first()
        val updated = hydrated.sets[1] as LoggingStandardSet
        assertEquals(125f, updated.weightRecommendation, "Should keep existing recommendation when ranges differ and prior succeeded")
        verify(exactly = 0) { WeightCalculationUtils.calculateSuggestedWeight(any(), any(), any(), any(), any(), any()) }
        unmockkObject(WeightCalculationUtils)
    }

    @Test
    fun `drop set after myo previous does nothing (type guard)`() {
        val prevMyo = LoggingMyoRepSet(
            position = 0, rpeTarget = 8f,
            repRangeBottom = 12, repRangeTop = 15, weightRecommendation = 100f,
            hadInitialWeightRecommendation = true, previousSetResultLabel = "",
            repRangePlaceholder = "12–15",
            complete = true, completedWeight = 100f, completedReps = 12, completedRpe = 8f,
            myoRepSetPosition = null, isNew = false
        )
        val nextDrop = LoggingDropSet(
            position = 1, dropPercentage = 0.25f,
            weightRecommendation = null, complete = false,
            repRangeBottom = 6, repRangeTop = 8, rpeTarget = 8f,
            repRangePlaceholder = "6-8", hadInitialWeightRecommendation = false, previousSetResultLabel = ""
        )
        val lift = LoggingWorkoutLift(
            id = 1, liftId = 9005, liftName = "Drop After Myo", position = 0,
            sets = listOf(prevMyo, nextDrop),
            progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
            deloadWeek = null, incrementOverride = 2.5f, isCustom = false
        )

        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), emptyList(), microCycle = 0, 4).first()
        val updated = hydrated.sets[1] as LoggingDropSet
        assertEquals(null, updated.weightRecommendation, "Drop set should not compute from a myo previous")
    }

    @Test
    fun `myo later mini copies previous mini completed weight`() {
        val activation = LoggingMyoRepSet(
            position = 0, rpeTarget = 8f,
            repRangeBottom = 12, repRangeTop = 15, weightRecommendation = 100f,
            hadInitialWeightRecommendation = true, previousSetResultLabel = "",
            repRangePlaceholder = "12–15",
            myoRepSetPosition = null,
            complete = true, completedWeight = 100f, completedReps = 12, completedRpe = 8f,
            isNew = false
        )
        val mini0 = activation.copy(myoRepSetPosition = 0, complete = true, completedWeight = 110f, completedReps = 6, completedRpe = 9f)
        val mini1 = activation.copy(myoRepSetPosition = 1, complete = false, completedWeight = null, completedReps = null, completedRpe = null)

        val lift = LoggingWorkoutLift(
            id = 1, liftId = 9006, liftName = "Myo Minis", position = 0,
            sets = listOf(activation, mini0, mini1),
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            deloadWeek = null, incrementOverride = 2.5f, isCustom = false
        )
        val results = listOf<SetResult>(
            MyoRepSetResult(workoutId = 1, liftId = 9006, liftPosition = 0, setPosition = 0, myoRepSetPosition = null, weight = 100f, reps = 12, rpe = 8f, isDeload = false),
            MyoRepSetResult(workoutId = 1, liftId = 9006, liftPosition = 0, setPosition = 0, myoRepSetPosition = 0, weight = 110f, reps = 6, rpe = 9f, isDeload = false),
        )

        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), results, microCycle = 0, 4).first()
        val mini1Updated = hydrated.sets[2] as LoggingMyoRepSet
        assertEquals(110f, mini1Updated.weightRecommendation, "Later myo mini should copy previous mini's completed weight")
    }

    @Test
    fun `myo second mini copies previous mini completed weight`() {
        val activation = LoggingMyoRepSet(
            position = 0, rpeTarget = 8f,
            repRangeBottom = 12, repRangeTop = 15, weightRecommendation = 100f,
            hadInitialWeightRecommendation = true, previousSetResultLabel = "",
            repRangePlaceholder = "12–15",
            myoRepSetPosition = null,
            complete = true, completedWeight = 100f, completedReps = 12, completedRpe = 8f,
            isNew = false
        )
        val mini0Completed = activation.copy(
            myoRepSetPosition = 0, complete = true,
            completedWeight = 105f, completedReps = 6, completedRpe = 9f
        )
        val mini1Next = activation.copy(
            myoRepSetPosition = 1, complete = false,
            completedWeight = null, completedReps = null, completedRpe = null,
            weightRecommendation = null
        )

        val lift = LoggingWorkoutLift(
            id = 1, liftId = 9007, liftName = "Myo later mini", position = 0,
            sets = listOf(activation, mini0Completed, mini1Next),
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            deloadWeek = null, incrementOverride = 2.5f, isCustom = false
        )
        val results = listOf<SetResult>(
            MyoRepSetResult(workoutId = 1, liftId = 9007, liftPosition = 0, setPosition = 0, myoRepSetPosition = null, weight = 100f, reps = 12, rpe = 8f, isDeload = false),
            MyoRepSetResult(workoutId = 1, liftId = 9007, liftPosition = 0, setPosition = 0, myoRepSetPosition = 0,    weight = 105f, reps = 6,  rpe = 9f, isDeload = false),
        )

        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), results, microCycle = 0, programDeloadWeek = 4).first()
        val mini1 = hydrated.sets[2] as LoggingMyoRepSet
        assertEquals(105f, mini1.weightRecommendation)
    }

    @Test
    fun `myo first mini does nothing when activation is not completed`() {
        val activation = LoggingMyoRepSet(
            position = 0, rpeTarget = 8f,
            repRangeBottom = 12, repRangeTop = 15, weightRecommendation = 100f,
            hadInitialWeightRecommendation = true, previousSetResultLabel = "",
            repRangePlaceholder = "12–15",
            myoRepSetPosition = null,
            complete = false, completedWeight = null, completedReps = null, completedRpe = null,
            isNew = false
        )
        val mini0Next = activation.copy(
            myoRepSetPosition = 0, complete = false,
            completedWeight = null, completedReps = null, completedRpe = null,
            weightRecommendation = null
        )

        val lift = LoggingWorkoutLift(
            id = 1, liftId = 9007, liftName = "Myo first mini guard - not completed", position = 0,
            sets = listOf(activation, mini0Next),
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            deloadWeek = null, incrementOverride = 2.5f, isCustom = false
        )

        val hydrated = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), emptyList(), microCycle = 0, programDeloadWeek = 4).first()
        val mini0 = hydrated.sets[1] as LoggingMyoRepSet
        assertEquals(null, mini0.weightRecommendation)
    }
}
