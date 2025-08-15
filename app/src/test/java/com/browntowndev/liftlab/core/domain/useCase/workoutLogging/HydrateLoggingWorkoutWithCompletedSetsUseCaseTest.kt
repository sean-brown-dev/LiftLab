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
import com.browntowndev.liftlab.core.domain.useCase.utils.WeightCalculationUtils
import io.mockk.every
import io.mockk.mockkObject
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
            restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null
        )

        // When: no new set results; hydrator should calculate next-set recommendation
        val result = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), listOf(setResultForLastCompleted), microCycle = 0)
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
        val result = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), listOf(setResultForLastCompleted), microCycle = 0)
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
            restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, note = null
        )

        // Expected: repGoal should be repRangeBottom (6) to push weight up
        val expected = WeightCalculationUtils.calculateSuggestedWeight(
            completedWeight = 80f,
            completedReps = 9,
            completedRpe = 6f,
            repGoal = 6,
            rpeGoal = 8f,
            roundingFactor = incrementOverride
        )

        // When
        val result = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), listOf(setResultForLastCompleted), microCycle = 0)
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
        val result = hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift), listOf(setResultForLastCompleted), microCycle = 0)
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
            note = null
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
        val hydrated = sut(liftsToHydrate = listOf(lift), setResults = results, microCycle = 0).first()

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
            note = null
        )

        val sut = HydrateLoggingWorkoutWithCompletedSetsUseCase()
        val hydrated = sut(liftsToHydrate = listOf(lift), setResults = emptyList(), microCycle = 0).first()

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
            sets = listOf(activation) // <-- activation-only
        )
        // Results include the activation and two continuation minis
        val results = listOf(
            MyoRepSetResult(workoutId = 1L, liftId = 201L, liftPosition = 0, setPosition = 0, myoRepSetPosition = null, weight = 100f, reps = 12, rpe = 8f, isDeload = false),
            MyoRepSetResult(workoutId = 1L, liftId = 201L, liftPosition = 0, setPosition = 0, myoRepSetPosition = 0, weight = 100f, reps = 6, rpe = 9f, isDeload = false),
            MyoRepSetResult(workoutId = 1L, liftId = 201L, liftPosition = 0, setPosition = 0, myoRepSetPosition = 1, weight = 100f, reps = 4, rpe = 9.5f, isDeload = false),
        )
        val sut = HydrateLoggingWorkoutWithCompletedSetsUseCase()
        val hydrated = sut(liftsToHydrate = listOf(lift), setResults = results, microCycle = 0).first()

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
        val hydrated = sut(liftsToHydrate = listOf(lift), setResults = results, microCycle = 0).first()

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
            sets = listOf(activation)
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
        val hydrated = sut(liftsToHydrate = listOf(lift), setResults = results, microCycle = 0).first()

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
}
