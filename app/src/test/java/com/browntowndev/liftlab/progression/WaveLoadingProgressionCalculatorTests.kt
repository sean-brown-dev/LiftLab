package com.browntowndev.liftlab.progression

import android.content.SharedPreferences
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.Utils.StepSize.Companion.getPossibleStepSizes
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.domain.models.StandardSetResult
import com.browntowndev.liftlab.core.data.local.dtos.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.data.local.entities.LiftEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLiftEntity
import com.browntowndev.liftlab.core.domain.mapping.CustomLiftSetMapper
import com.browntowndev.liftlab.core.domain.mapping.WorkoutLiftMapper
import com.browntowndev.liftlab.core.domain.useCase.workout.progression.WaveLoadingProgressionCalculator
import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals

class WaveLoadingProgressionCalculatorTests {
    private val calculator = WaveLoadingProgressionCalculator(4, 1)
    private val workoutLiftMapper = WorkoutLiftMapper(CustomLiftSetMapper())

    @BeforeEach
    fun setup() {
        // Set the main dispatcher to the test dispatcher
        val sharedPrefs = mockk<SharedPreferences>()
        every { sharedPrefs.getBoolean(any(), any()) } returns true
        every { sharedPrefs.getLong(any(), any()) } returns SettingsManager.SettingNames.DEFAULT_REST_TIME
        every { sharedPrefs.getFloat(any(), any()) } returns SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT

        SettingsManager.initialize(sharedPrefs)
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
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(liftEntity), previousSetData, previousSetData, false)
        result.forEach {
            assertEquals(80f, it.weightRecommendation)
        }
    }

    @Test
    fun `all sets decrease weight when one fails`() {
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
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 4, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = WaveLoadingProgressionCalculator(4, 1)
            .calculate(workoutLiftMapper.map(liftEntity), previousSetData, previousSetData, false)

        result.fastForEach {
            assertEquals(70f, it.weightRecommendation)
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
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(liftEntity), previousSetData, previousSetData, true)
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
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 85f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 85f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 85f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = WaveLoadingProgressionCalculator(4, 0)
            .calculate(workoutLiftMapper.map(liftEntity), previousSetData, previousSetData, false)

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
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 85f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 85f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 85f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = WaveLoadingProgressionCalculator(6, 3)
            .calculate(workoutLiftMapper.map(liftEntity), previousSetData, previousSetData, false)

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
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 85f, microCycle = 1, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 85f, microCycle = 1, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 85f, microCycle = 1, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = WaveLoadingProgressionCalculator(4, 2)
            .calculate(workoutLiftMapper.map(liftEntity), previousSetData, previousSetData, false)

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
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 85f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 85f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 85f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = WaveLoadingProgressionCalculator(7, 3)
            .calculate(workoutLiftMapper.map(liftEntity), previousSetData, previousSetData, false)

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
}