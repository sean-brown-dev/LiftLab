package com.browntowndev.liftlab.core.domain.useCase.progression

import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_REST_TIME
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.REST_TIME
import com.browntowndev.liftlab.core.data.local.dtos.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.data.local.entities.LiftEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLiftEntity
import com.browntowndev.liftlab.core.data.mapping.toCalculationDomainModel
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.StandardSetResult
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LinearProgressionCalculatorTests {
/*

    private lateinit var calculator: LinearProgressionCalculator

    @BeforeEach
    fun setup() {
        mockkObject(SettingsManager)
        every { SettingsManager.getSetting(INCREMENT_AMOUNT, DEFAULT_INCREMENT_AMOUNT) } returns DEFAULT_INCREMENT_AMOUNT
        every { SettingsManager.getSetting(REST_TIME, DEFAULT_REST_TIME) } returns DEFAULT_REST_TIME

        calculator = LinearProgressionCalculator()
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
            LinearProgressionSetResult(missedLpGoals = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0,
                weight = 75f,
                workoutId = 0, liftId = 0,
                isDeload = false),
            LinearProgressionSetResult(missedLpGoals = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0,
                weight = 75f,
                workoutId = 0, liftId = 0,
                isDeload = false),
            LinearProgressionSetResult(missedLpGoals = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0,
                weight = 75f,
                workoutId = 0, liftId = 0,
                isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)
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
            LinearProgressionSetResult(missedLpGoals = 1, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0,
                weight = 75f,
                workoutId = 0, liftId = 0,
                isDeload = false),
            LinearProgressionSetResult(missedLpGoals = 1, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1,
                weight = 75f,
                workoutId = 0, liftId = 0,
                isDeload = false),
            LinearProgressionSetResult(missedLpGoals = 1, reps = 5, rpe = 8f, liftPosition = 0, setPosition = 2,
                weight = 75f,
                workoutId = 0, liftId = 0,
                isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)
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
            LinearProgressionSetResult(missedLpGoals = 2, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0,
                weight = 100f,
                workoutId = 0, liftId = 0,
                isDeload = false),
            LinearProgressionSetResult(missedLpGoals = 2, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1,
                weight = 100f,
                workoutId = 0, liftId = 0,
                isDeload = false),
            LinearProgressionSetResult(missedLpGoals = 2, reps = 5, rpe = 8f, liftPosition = 0, setPosition = 2,
                weight = 100f,
                workoutId = 0, liftId = 0,
                isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)
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
        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)
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
            StandardSetResult(missedLpGoals = 2, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0,
                weight = 100f,
                workoutId = 0, liftId = 0,
                setType = SetType.STANDARD, isDeload = false),
            LinearProgressionSetResult(missedLpGoals = 2, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0,
                weight = 100f,
                workoutId = 0, liftId = 0,
                isDeload = false),
            LinearProgressionSetResult(missedLpGoals = 2, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 0,
                weight = 100f,
                workoutId = 0, liftId = 0,
                isDeload = false),
        )

        assertThrows(Exception::class.java) {
            calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)
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
        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)
        result.forEach {
            assertEquals(null, it.weightRecommendation)
        }
    }

*/
}