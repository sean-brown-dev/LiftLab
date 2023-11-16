package com.browntowndev.liftlab.progression

import android.content.SharedPreferences
import com.browntowndev.liftlab.core.common.SettingsManager
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
    private val calculator = WaveLoadingProgressionCalculator(4, 0)
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
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)
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
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 4, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD),
        )

        val result = WaveLoadingProgressionCalculator(4, 1)
            .calculate(workoutLiftMapper.map(lift), previousSetData, false)

        result.forEach {
            Assert.assertEquals(75f, it.weightRecommendation)
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
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 2, mesoCycle = 0, setType = SetType.STANDARD),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, true)
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
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 3, mesoCycle = 0, setType = SetType.STANDARD),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 3, mesoCycle = 0, setType = SetType.STANDARD),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 3, mesoCycle = 0, setType = SetType.STANDARD),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)
        result.forEach {
            Assert.assertEquals(80f, it.weightRecommendation)
        }
    }
}