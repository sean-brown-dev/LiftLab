package com.browntowndev.liftlab.progression

import android.content.SharedPreferences
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.Utils
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.queryable.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.Lift
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLift
import com.browntowndev.liftlab.core.persistence.mapping.CustomLiftSetMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLiftMapper
import com.browntowndev.liftlab.core.progression.WaveLoadingProgressionCalculator
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class WaveLoadingProgressionCalculatorTests {
    private val calculator = WaveLoadingProgressionCalculator(4, 1)
    private val workoutLiftMapper = WorkoutLiftMapper(CustomLiftSetMapper())

    @Before
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
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
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
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, previousSetData, false)
        result.forEach {
            Assert.assertEquals(80f, it.weightRecommendation)
        }
    }

    @Test
    fun `all sets decrease weight when one fails`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
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
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 4, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = WaveLoadingProgressionCalculator(4, 1)
            .calculate(workoutLiftMapper.map(lift), previousSetData, previousSetData, false)

        result.fastForEach {
            Assert.assertEquals(70f, it.weightRecommendation)
        }
    }

    @Test
    fun `weight decrements on deload week`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
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
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, previousSetData, true)
        result.forEach {
            Assert.assertEquals(65f, it.weightRecommendation)
        }
    }

    @Test
    fun `weight increments up from previous week first week after deload week`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
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
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 85f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 85f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 85f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = WaveLoadingProgressionCalculator(4, 0)
            .calculate(workoutLiftMapper.map(lift), previousSetData, previousSetData, false)

        result.forEach {
            Assert.assertEquals(80f, it.weightRecommendation)
        }
    }

    @Test
    fun `weight increments and placeholder restarts if step size goes past rep range bottom without deload and has uneven steps`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeBottom = 6,
                repRangeTop = 8,
                rpeTarget = 8f,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 85f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 85f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 85f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = WaveLoadingProgressionCalculator(6, 3)
            .calculate(workoutLiftMapper.map(lift), previousSetData, previousSetData, false)

        result.forEach {
            Assert.assertEquals("8", it.repRangePlaceholder)
            Assert.assertEquals(80f, it.weightRecommendation)
        }
    }

    @Test
    fun `weight increments and set recommendation ends on rep range bottom with uneven sets prior to deload`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeBottom = 6,
                repRangeTop = 9,
                rpeTarget = 8f,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 85f, microCycle = 1, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 85f, microCycle = 1, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 85f, microCycle = 1, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = WaveLoadingProgressionCalculator(4, 2)
            .calculate(workoutLiftMapper.map(lift), previousSetData, previousSetData, false)

        result.forEach {
            Assert.assertEquals("6", it.repRangePlaceholder)
            Assert.assertEquals(90f, it.weightRecommendation)
        }
    }

    @Test
    fun `weight increments and placeholder restarts if step size goes past rep range bottom without deload and has even steps`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
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
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 85f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 85f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 85f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = WaveLoadingProgressionCalculator(7, 3)
            .calculate(workoutLiftMapper.map(lift), previousSetData, previousSetData, false)

        result.forEach {
            Assert.assertEquals("10", it.repRangePlaceholder)
            Assert.assertEquals(80f, it.weightRecommendation)
        }
    }

    @Test
    fun `gets correct step size options`() {
        var deloadWeek = 4
        var stepSizes = Utils.getPossibleStepSizes(10, 6, deloadWeek - 2)

        Assert.assertEquals(1, stepSizes.size)
        Assert.assertEquals(2, stepSizes[0])

        deloadWeek = 5
        stepSizes = Utils.getPossibleStepSizes(10, 6, deloadWeek - 2)

        Assert.assertEquals(1, stepSizes.size)
        Assert.assertEquals(4, stepSizes[0])

        deloadWeek = 4
        stepSizes = Utils.getPossibleStepSizes(8, 6, deloadWeek - 2)

        Assert.assertEquals(1, stepSizes.size)
        Assert.assertEquals(1, stepSizes[0])

        deloadWeek = 7
        stepSizes = Utils.getPossibleStepSizes(8, 6, deloadWeek - 2)

        Assert.assertEquals(2, stepSizes.size)
        Assert.assertEquals(1, stepSizes[0])
        Assert.assertEquals(2, stepSizes[1])
    }
}