package com.browntowndev.liftlab.core.domain.useCase.progression

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_REST_TIME
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.REST_TIME
import com.browntowndev.liftlab.core.data.local.dtos.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.data.local.entities.LiftEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLiftEntity
import com.browntowndev.liftlab.core.data.mapping.toCalculationDomainModel
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.workoutLogging.StandardSetResult
import com.browntowndev.liftlab.core.domain.utils.getPossibleStepSizes
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WaveLoadingProgressionCalculatorTests {
    private lateinit var calculator: WaveLoadingProgressionCalculator

    @BeforeEach
    fun setup() {
        mockkObject(SettingsManager)
        every { SettingsManager.getSetting(INCREMENT_AMOUNT, DEFAULT_INCREMENT_AMOUNT) } returns DEFAULT_INCREMENT_AMOUNT
        every { SettingsManager.getSetting(REST_TIME, DEFAULT_REST_TIME) } returns DEFAULT_REST_TIME

        calculator = WaveLoadingProgressionCalculator(4, 1)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SettingsManager)
    }

    @Test
    fun `all sets increment`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeBottom = 6,
                repRangeTop = 8,
                rpeTarget = 8f,
                stepSize = 1,
            ),
            liftEntity = LiftEntity(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0,
                weight = 75f,
                setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 9f, liftPosition = 0, setPosition = 1,
                weight = 75f,
                setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 10f, liftPosition = 0, setPosition = 2,
                weight = 75f,
                setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)
        result.forEach {
            assertEquals(80f, it.weightRecommendation)
        }
    }

    @Test
    fun `sets do not increment and instead get recalculated weight when one fails`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeBottom = 6,
                repRangeTop = 8,
                rpeTarget = 8f,
                stepSize = 1,
            ),
            liftEntity = LiftEntity(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0,
                weight = 75f,
                setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 9f, liftPosition = 0, setPosition = 1,
                weight = 75f,
                setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 7, rpe = 10f, liftPosition = 0, setPosition = 2,
                weight = 75f,
                setType = SetType.STANDARD, isDeload = false),
        )

        val result = WaveLoadingProgressionCalculator(4, 1)
            .calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.fastForEach {
            assertEquals(75f, it.weightRecommendation)
        }
    }

    @Test
    fun `weight decrements on deload week`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeBottom = 6,
                repRangeTop = 8,
                rpeTarget = 8f,
                stepSize = 1,
            ),
            liftEntity = LiftEntity(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 0,
                weight = 75f,
                setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 1,
                weight = 75f,
                setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 2,
                weight = 75f,
                setType = SetType.STANDARD, isDeload = false),
        )

        val result = WaveLoadingProgressionCalculator(4, 3)
            .calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, true)
        result.forEach {
            assertEquals(65f, it.weightRecommendation)
        }
    }

    @Test
    fun `weight increments up from previous week first week after deload week`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeBottom = 6,
                repRangeTop = 8,
                rpeTarget = 8f,
                stepSize = 1,
            ),
            liftEntity = LiftEntity(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 0,
                weight = 85f,
                setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 9f, liftPosition = 0, setPosition = 1,
                weight = 85f,
                setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 10f, liftPosition = 0, setPosition = 2,
                weight = 85f,
                setType = SetType.STANDARD, isDeload = false),
        )

        val result = WaveLoadingProgressionCalculator(4, 0)
            .calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.forEach {
            assertEquals(80f, it.weightRecommendation)
        }
    }

    @Test
    fun `weight increments and placeholder restarts if step size goes past rep range bottom without deload and has uneven steps`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeBottom = 6,
                repRangeTop = 8,
                rpeTarget = 8f,
            ),
            liftEntity = LiftEntity(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 0,
                weight = 85f,
                setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 9f, liftPosition = 0, setPosition = 1,
                weight = 85f,
                setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 10f, liftPosition = 0, setPosition = 2,
                weight = 85f,
                setType = SetType.STANDARD, isDeload = false),
        )

        val result = WaveLoadingProgressionCalculator(6, 3)
            .calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.forEach {
            assertEquals("8", it.repRangePlaceholder)
            assertEquals(80f, it.weightRecommendation)
        }
    }

    @Test
    fun `weight increments and set recommendation ends on rep range bottom with uneven sets prior to deload`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeBottom = 6,
                repRangeTop = 9,
                rpeTarget = 8f,
            ),
            liftEntity = LiftEntity(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0,
                weight = 85f,
                setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 9f, liftPosition = 0, setPosition = 1,
                weight = 85f,
                setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 10f, liftPosition = 0, setPosition = 2,
                weight = 85f,
                setType = SetType.STANDARD, isDeload = false),
        )

        val result = WaveLoadingProgressionCalculator(4, 2)
            .calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.forEach {
            assertEquals("6", it.repRangePlaceholder)
            assertEquals(90f, it.weightRecommendation)
        }
    }

    @Test
    fun `weight increments and placeholder restarts if step size goes past rep range bottom without deload and has even steps`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeBottom = 6,
                repRangeTop = 10,
                rpeTarget = 8f,
                stepSize = 2,
            ),
            liftEntity = LiftEntity(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 0,
                weight = 85f,
                setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 1,
                weight = 85f,
                setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 2,
                weight = 85f,
                setType = SetType.STANDARD, isDeload = false),
        )

        val result = WaveLoadingProgressionCalculator(7, 3)
            .calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.forEach {
            assertEquals("10", it.repRangePlaceholder)
            assertEquals(80f, it.weightRecommendation)
        }
    }

    @Test
    fun `gets correct step size options`() {
        var deloadWeek = 4
        var stepSizes = getPossibleStepSizes(10, 6, deloadWeek - 2)

        assertEquals(1, stepSizes.size)
        assertEquals(2, stepSizes[0])

        deloadWeek = 5
        stepSizes = getPossibleStepSizes(10, 6, deloadWeek - 2)

        assertEquals(1, stepSizes.size)
        assertEquals(4, stepSizes[0])

        deloadWeek = 4
        stepSizes = getPossibleStepSizes(8, 6, deloadWeek - 2)

        assertEquals(1, stepSizes.size)
        assertEquals(1, stepSizes[0])

        deloadWeek = 7
        stepSizes = getPossibleStepSizes(8, 6, deloadWeek - 2)

        assertEquals(2, stepSizes.size)
        assertEquals(1, stepSizes[0])
        assertEquals(2, stepSizes[1])
    }

    @Test
    fun `recalculate when top set exceeds rep range top within allowed RPE`() {
        // Given a 6–8 rep range (top==8) and microCycle set so current target reps == 7 (not top-of-range week)
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeBottom = 6,
                repRangeTop = 8,
                rpeTarget = 8f,
                stepSize = 1,
            ),
            liftEntity = LiftEntity(name = "", movementPattern = MovementPattern.LEG_PUSH, volumeTypesBitmask = 1),
        )
        // Exceed on the "top set" (set 0 uses top-set RPE target), but still at RPE 8 (allowed)
        val previous = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 10, rpe = 8f, liftPosition = 0, setPosition = 0, weight = 75f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 7f, liftPosition = 0, setPosition = 1, weight = 75f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 6f, liftPosition = 0, setPosition = 2, weight = 75f, setType = SetType.STANDARD, isDeload = false),
        )

        // microCycle=1 here (constructor below) => not the deload week, current-week target reps via step sequence should be 7
        val result = WaveLoadingProgressionCalculator(programDeloadWeek = 4, microCycle = 1)
            .calculate(liftEntity.toCalculationDomainModel(), previous, previous, false)

        // Expect a recalculated weight (not simple +increment to 80f), and flattened across sets
        // We can assert it's NOT equal to the naive increment (80f), and NOT equal to the old 75f.
        result.forEach { set ->
            assert(set.weightRecommendation != 80f) { "Expected recalculated weight, not simple +increment" }
            assert(set.weightRecommendation != 75f) { "Expected a changed recommendation after exceed" }
        }
    }

    @Test
    fun `do NOT recalc when intermediate exceeds top but violates RPE cap (RPE 9_5)`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                position = 0, setCount = 3, repRangeBottom = 6, repRangeTop = 8, rpeTarget = 8f, stepSize = 1
            ),
            liftEntity = LiftEntity(name = "", movementPattern = MovementPattern.LEG_PUSH, volumeTypesBitmask = 1),
        )
        val previous = listOf(
            // top set fine
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weight = 75f, setType = SetType.STANDARD, isDeload = false),
            // intermediate exceeds reps (9) but RPE 9.5 > cap(9) => should NOT trigger recalc
            StandardSetResult(workoutId = 0, liftId = 0, reps = 9, rpe = 9.5f, liftPosition = 0, setPosition = 1, weight = 75f, setType = SetType.STANDARD, isDeload = false),
            // final fine
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 10f, liftPosition = 0, setPosition = 2, weight = 75f, setType = SetType.STANDARD, isDeload = false),
        )

        val result = WaveLoadingProgressionCalculator(programDeloadWeek = 4, microCycle = 1)
            .calculate(liftEntity.toCalculationDomainModel(), previous, previous, false)

        // With no valid-recalc trigger, Wave Loading should fall back to normal increment behavior (flattened to +5)
        result.forEach { set -> assertEquals(80f, set.weightRecommendation) }
    }

    @Test
    fun `single set - exceeding top within allowed RPE triggers recalculation (not simple +increment)`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                position = 0, setCount = 1, repRangeBottom = 6, repRangeTop = 8, rpeTarget = 8f, stepSize = 1
            ),
            liftEntity = LiftEntity(name = "", movementPattern = MovementPattern.BICEP_ISO, volumeTypesBitmask = 1),
        )
        val previous = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 20, rpe = 8f, liftPosition = 0, setPosition = 0, weight = 60f, setType = SetType.STANDARD, isDeload = false),
        )

        val result = WaveLoadingProgressionCalculator(programDeloadWeek = 4, microCycle = 1)
            .calculate(liftEntity.toCalculationDomainModel(), previous, previous, false)

        // Exceed -> recalc
        result.forEach { set ->
            assert(set.weightRecommendation!! > 65f)
        }
    }

    @Test
    fun `when current week equals repRangeTop, exceeding top should recalc instead of decrementing for new microcycle`() {
        // microCycle=2 with deloadWeek=4 and stepSize=1 => sequence [8,7,6] before deload; microCycle=2 => target reps == 6 (repRangeBottom)
        // To hit the branch where "isTopOfRepRange == true", set microCycle so getRepsForMicrocycle returns repRangeTop.
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0, liftId = 0, progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                position = 0, setCount = 3, repRangeBottom = 6, repRangeTop = 8, rpeTarget = 8f, stepSize = 1
            ),
            liftEntity = LiftEntity(name = "", movementPattern = MovementPattern.LEG_PUSH, volumeTypesBitmask = 1),
        )
        val previous = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 9, rpe = 7.5f, liftPosition = 0, setPosition = 0, weight = 75f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 9f, liftPosition = 0, setPosition = 1, weight = 75f, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 10f, liftPosition = 0, setPosition = 2, weight = 75f, setType = SetType.STANDARD, isDeload = false),
        )

        // Force "isTopOfRepRange == true" by making microCycle=0 so current-week reps == repRangeTop
        val result = WaveLoadingProgressionCalculator(programDeloadWeek = 4, microCycle = 0)
            .calculate(liftEntity.toCalculationDomainModel(), previous, previous, false)

        // Because we exceeded top, the branch should do RECALC (not "decrementForNewMicrocycle")
        result.forEach { set ->
            assert(set.weightRecommendation != 70f) { "Should not decrement for new microcycle when exceeding top" }
        }
    }
}