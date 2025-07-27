package com.browntowndev.liftlab.progression

import android.content.SharedPreferences
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.domain.models.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.StandardSetResult
import com.browntowndev.liftlab.core.data.local.dtos.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.data.local.entities.LiftEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLiftEntity
import com.browntowndev.liftlab.core.data.mapping.WorkoutLiftMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.domain.useCase.workout.progression.LinearProgressionCalculator
import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows

class LinearProgressionCalculatorTests {
    private val calculator = LinearProgressionCalculator()

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
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
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
            LinearProgressionSetResult(missedLpGoals = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0, isDeload = false),
            LinearProgressionSetResult(missedLpGoals = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0, isDeload = false),
            LinearProgressionSetResult(missedLpGoals = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toDomainModel(), previousSetData, previousSetData, false)
        result.forEach {
            assertEquals(80f, it.weightRecommendation)
        }
    }

    @Test
    fun `sets do not increment on first failure`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
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
            LinearProgressionSetResult(missedLpGoals = 1, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0, isDeload = false),
            LinearProgressionSetResult(missedLpGoals = 1, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0, isDeload = false),
            LinearProgressionSetResult(missedLpGoals = 1, reps = 5, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toDomainModel(), previousSetData, previousSetData, false)
        result.forEach {
            assertEquals(75f, it.weightRecommendation)
        }
    }

    @Test
    fun `weight drops 10 percent on second failure`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
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
            LinearProgressionSetResult(missedLpGoals = 2, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 100f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0, isDeload = false),
            LinearProgressionSetResult(missedLpGoals = 2, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 100f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0, isDeload = false),
            LinearProgressionSetResult(missedLpGoals = 2, reps = 5, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 100f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toDomainModel(), previousSetData, previousSetData, false)
        result.forEach {
            assertEquals(90f, it.weightRecommendation)
        }
    }

    @Test
    fun `weight is null when an empty list of set results is passed in`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
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
        val previousSetData = listOf<LinearProgressionSetResult>()
        val result = calculator.calculate(liftEntity.toDomainModel(), previousSetData, previousSetData, false)
        result.forEach {
            assertEquals(null, it.weightRecommendation)
        }
    }

    @Test
    fun `throws exception if invalid set type passed in`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
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
            StandardSetResult(missedLpGoals = 2, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 100f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            LinearProgressionSetResult(missedLpGoals = 2, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 100f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0, isDeload = false),
            LinearProgressionSetResult(missedLpGoals = 2, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 100f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0, isDeload = false),
        )

        assertThrows(Exception::class.java) {
            calculator.calculate(liftEntity.toDomainModel(), previousSetData, previousSetData, false)
        }
    }

    @Test
    fun `weight recommendation should be null for all lifts`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
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
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeBottom = 6,
                    repRangeTop = 8,
                    type = SetType.STANDARD,
                )
            )
        )
        val previousSetData = listOf<LinearProgressionSetResult>()
        val result = calculator.calculate(liftEntity.toDomainModel(), previousSetData, previousSetData, false)
        result.forEach {
            assertEquals(null, it.weightRecommendation)
        }
    }
}