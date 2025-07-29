package com.browntowndev.liftlab.core.domain.useCase.progression

import android.content.SharedPreferences
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.StandardSetResult
import com.browntowndev.liftlab.core.data.local.dtos.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.data.local.entities.LiftEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLiftEntity
import com.browntowndev.liftlab.core.data.mapping.WorkoutLiftMappingExtensions.toCalculationDomainModel
import com.browntowndev.liftlab.core.domain.useCase.workout.progression.DynamicDoubleProgressionCalculator
import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals

class DynamicDoubleProgressionCalculatorTests {
    private val calculator = DynamicDoubleProgressionCalculator()

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
    fun `weight should increment by lift increment override when set goals are met for a standard lift`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
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
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.fastForEach { p ->
            assertEquals(80f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should be 0 for a standard lift when there is no set data`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
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
        val previousSetData = listOf<StandardSetResult>()
        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.fastForEach { p ->
            assertNull(p.weightRecommendation)
        }
    }

    @Test
    fun `weight should increment by lift increment override when set goals are met for a standard lift and a set was deleted`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
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
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 3, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.fastForEach { p ->
            assertEquals(80f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should increment by lift increment override when set goals are met for a standard lift and a set was added`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                position = 0,
                setCount = 4,
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
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.fastForEach { p ->
            assertEquals(if (p.position != 3) 80f else null, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should not change for last standard lift set when set goal is not met`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                position = 0,
                setCount = 4,
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
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.fastForEach { p ->
            assertEquals(if(p.position == 2) 75f else if (p.position == 3) null else 80f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should increment by increment override when all standard custom lift set goals are met`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeTop = 8,
            ),
            liftEntity = LiftEntity(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 1,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 2,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
            )
        )
        val previousSetData = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.fastForEach { p ->
            assertEquals(80f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should not change on last standard custom lift set goals are not met`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeTop = 8,
            ),
            liftEntity = LiftEntity(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 1,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 2,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
            )
        )
        val previousSetData = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.fastForEach { p ->
            assertEquals(if(p.position == 2) 75f else 80f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should increment by increment override when all myorep set goals are met`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                position = 0,
                setCount = 1,
                repRangeTop = 8,
            ),
            liftEntity = LiftEntity(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    type = SetType.MYOREP,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 15,
                    repRangeBottom = 12,
                    setGoal = 3,
                    repFloor = 5,
                ),
            )
        )
        val previousSetData = listOf(
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 15, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 10, rpe = 8f, liftPosition = 0, setPosition = 0, myoRepSetPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 0, myoRepSetPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 4, rpe = 8f, liftPosition = 0, setPosition = 0, myoRepSetPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.fastForEach { p ->
            assertEquals(80f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should increment for myorep match set but not standard set`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                position = 0,
                setCount = 2,
                repRangeTop = 8,
            ),
            liftEntity = LiftEntity(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    type = SetType.MYOREP,
                    position = 1,
                    rpeTarget = 8f,
                    repRangeTop = 15,
                    repRangeBottom = 12,
                    setGoal = 3,
                    setMatching = true,
                ),
            )
        )
        val previousSetData = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 15, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 5, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 3, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.fastForEach { p ->
            if (p.position == 0)
                assertEquals(75f, p.weightRecommendation)
            else
                assertEquals(80f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should increment for myorep set but not for standard set`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                position = 0,
                setCount = 2,
                repRangeTop = 8,
            ),
            liftEntity = LiftEntity(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    type = SetType.MYOREP,
                    position = 1,
                    rpeTarget = 8f,
                    repRangeTop = 15,
                    repRangeBottom = 12,
                    setGoal = 3,
                    repFloor = 5,
                ),
            )
        )
        val previousSetData = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 15, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 10, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 5, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 3, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.fastForEach { p ->
            if (p.position == 0)
                assertEquals(75f, p.weightRecommendation)
            else
                assertEquals(80f,  p.weightRecommendation)
        }
    }

    @Test
    fun `weight should not change when all myorep match sets are not met`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                position = 0,
                setCount = 2,
                repRangeTop = 8,
            ),
            liftEntity = LiftEntity(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    type = SetType.MYOREP,
                    position = 1,
                    rpeTarget = 8f,
                    repRangeTop = 15,
                    repRangeBottom = 12,
                    setGoal = 3,
                    setMatching = true,
                ),
            )
        )
        val previousSetData = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 4, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 2, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 2, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.fastForEach { p ->
            assertEquals(75f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should not change when myorep activation set rep goal is missed`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                position = 0,
                setCount = 1,
                repRangeTop = 8,
            ),
            liftEntity = LiftEntity(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    type = SetType.MYOREP,
                    position = 1,
                    rpeTarget = 8f,
                    repRangeTop = 15,
                    repRangeBottom = 12,
                    setGoal = 3,
                    repFloor = 5,
                ),
            )
        )
        val previousSetData = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 10, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 5, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.fastForEach { p ->
            assertEquals(75f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should be 0 for standard lift when there is no set data`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                position = 0,
                setCount = 4,
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
        val previousSetData = listOf<StandardSetResult>()
        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.fastForEach { p ->
            assertNull(p.weightRecommendation)
        }
    }

    @Test
    fun `weight should be NULL for custom lift when there is no set data`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                position = 0,
                setCount = 2,
                repRangeTop = 8,
            ),
            liftEntity = LiftEntity(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    type = SetType.MYOREP,
                    position = 1,
                    rpeTarget = 8f,
                    repRangeTop = 15,
                    repRangeBottom = 12,
                    setGoal = 3,
                    repFloor = 5,
                ),
            )
        )
        val previousSetData = listOf<StandardSetResult>()
        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.fastForEach { p ->
            assertNull(p.weightRecommendation)
        }
    }

    @Test
    fun `weight should not change when myorep match set rep count doesn't meet goal`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                position = 0,
                setCount = 1,
                repRangeTop = 8,
            ),
            liftEntity = LiftEntity(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    type = SetType.MYOREP,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 15,
                    repRangeBottom = 12,
                    setGoal = 3,
                    setMatching = true,
                ),
            )
        )
        val previousSetData = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 15, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 1, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 1, rpe = 8f, liftPosition = 0, setPosition = 0, myoRepSetPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.fastForEach { p ->
            assertEquals(75f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should not change when myorep match set rpe does not meet goal`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                position = 0,
                setCount = 1,
                repRangeTop = 8,
            ),
            liftEntity = LiftEntity(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(
                    workoutLiftId = 0,
                    type = SetType.MYOREP,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 15,
                    repRangeBottom = 12,
                    setGoal = 3,
                    setMatching = true,
                ),
            )
        )
        val previousSetData = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 15, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 10, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 4, rpe = 8f, liftPosition = 0, setPosition = 0, myoRepSetPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
            MyoRepSetResult(workoutId = 0, liftId = 0, reps = 1, rpe = 9f, liftPosition = 0, setPosition = 0, myoRepSetPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.fastForEach { p ->
            assertEquals(75f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should increment by increment override when all standard and drop set custom lift set goals are met with double drop sets`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                id = 0,
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeTop = 8,
            ),
            liftEntity = LiftEntity(
                id = 0,
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(
                    id = 0,
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSetEntity(
                    id = 1,
                    workoutLiftId = 0,
                    type = SetType.DROP_SET,
                    dropPercentage = .1f,
                    position = 1,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSetEntity(
                    id = 2,
                    workoutLiftId = 0,
                    type = SetType.DROP_SET,
                    dropPercentage = .1f,
                    position = 2,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
            )
        )
        val previousSetData = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 100f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 90f, microCycle = 0, mesoCycle= 0, setType = SetType.DROP_SET, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 80f, microCycle = 0, mesoCycle= 0, setType = SetType.DROP_SET, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.forEachIndexed { index, p ->
            when (index) {
                0 -> assertEquals(105f, p.weightRecommendation)
                1 -> assertEquals(95f, p.weightRecommendation)
                2 -> assertEquals(85f, p.weightRecommendation)
            }
        }
    }

    @Test
    fun `weight should NOT increment when all standard and drop set custom lift set goals are NOT met with double drop sets`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                id = 0,
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeTop = 8,
            ),
            liftEntity = LiftEntity(
                id = 0,
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(
                    id = 0,
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSetEntity(
                    id = 1,
                    workoutLiftId = 0,
                    type = SetType.DROP_SET,
                    dropPercentage = .1f,
                    position = 1,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSetEntity(
                    id = 2,
                    workoutLiftId = 0,
                    type = SetType.DROP_SET,
                    dropPercentage = .1f,
                    position = 2,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
            )
        )
        val previousSetData = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 100f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 90f, microCycle = 0, mesoCycle= 0, setType = SetType.DROP_SET, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 80f, microCycle = 0, mesoCycle= 0, setType = SetType.DROP_SET, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.forEachIndexed { index, p ->
            when (index) {
                0 -> assertEquals(100f, p.weightRecommendation)
                1 -> assertEquals(90f, p.weightRecommendation)
                2 -> assertEquals(80f, p.weightRecommendation)
            }
        }
    }

    @Test
    fun `weight should decrease ONLY for drop set that fails to meet minimum reps for lift with double drop sets`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                id = 0,
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeTop = 8,
            ),
            liftEntity = LiftEntity(
                id = 0,
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(
                    id = 0,
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSetEntity(
                    id = 1,
                    workoutLiftId = 0,
                    type = SetType.DROP_SET,
                    dropPercentage = .1f,
                    position = 1,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSetEntity(
                    id = 2,
                    workoutLiftId = 0,
                    type = SetType.DROP_SET,
                    dropPercentage = .1f,
                    position = 2,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
            )
        )
        val previousSetData = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 100f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 90f, microCycle = 0, mesoCycle= 0, setType = SetType.DROP_SET, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 3, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 80f, microCycle = 0, mesoCycle= 0, setType = SetType.DROP_SET, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.forEachIndexed { index, p ->
            when (index) {
                0 -> assertEquals(100f, p.weightRecommendation)
                1 -> assertEquals(90f, p.weightRecommendation)
                2 -> assertEquals(75f, p.weightRecommendation)
            }
        }
    }

    @Test
    fun `weight should decrease ONLY for top set that fails to meet minimum reps for lift with double drop sets`() {
        val liftEntity = WorkoutLiftWithRelationships(
            workoutLiftEntity = WorkoutLiftEntity(
                id = 0,
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeTop = 8,
            ),
            liftEntity = LiftEntity(
                id = 0,
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSetEntities = listOf(
                CustomLiftSetEntity(
                    id = 0,
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSetEntity(
                    id = 1,
                    workoutLiftId = 0,
                    type = SetType.DROP_SET,
                    dropPercentage = .1f,
                    position = 1,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSetEntity(
                    id = 2,
                    workoutLiftId = 0,
                    type = SetType.DROP_SET,
                    dropPercentage = .1f,
                    position = 2,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
            )
        )
        val previousSetData = listOf(
            StandardSetResult(workoutId = 0, liftId = 0, reps = 3, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 100f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 90f, microCycle = 0, mesoCycle= 0, setType = SetType.DROP_SET, isDeload = false),
            StandardSetResult(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 80f, microCycle = 0, mesoCycle= 0, setType = SetType.DROP_SET, isDeload = false),
        )

        val result = calculator.calculate(liftEntity.toCalculationDomainModel(), previousSetData, previousSetData, false)

        result.forEachIndexed { index, p ->
            when (index) {
                0 -> assertEquals(95f, p.weightRecommendation)
                1 -> assertEquals(90f, p.weightRecommendation)
                2 -> assertEquals(80f, p.weightRecommendation)
            }
        }
    }
}