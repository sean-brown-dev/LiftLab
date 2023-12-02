package com.browntowndev.liftlab.progression

import android.content.SharedPreferences
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_REST_TIME
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.queryable.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.CustomLiftSet
import com.browntowndev.liftlab.core.persistence.entities.Lift
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLift
import com.browntowndev.liftlab.core.persistence.mapping.CustomLiftSetMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLiftMapper
import com.browntowndev.liftlab.core.progression.DoubleProgressionCalculator
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DoubleProgressionCalculatorTests {
    private val calculator = DoubleProgressionCalculator()
    private val workoutLiftMapper = WorkoutLiftMapper(CustomLiftSetMapper())

    @Before
    fun setup() {
        // Set the main dispatcher to the test dispatcher
        val sharedPrefs = mockk<SharedPreferences>()
        every { sharedPrefs.getBoolean(any(), any()) } returns true
        every { sharedPrefs.getLong(any(), any()) } returns DEFAULT_REST_TIME
        every { sharedPrefs.getFloat(any(), any()) } returns DEFAULT_INCREMENT_AMOUNT

        SettingsManager.initialize(sharedPrefs)
    }

    @Test
    fun `weight should increment by lift increment override when all set goals are met for a standard lift`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle = 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)

        result.fastForEach { p ->
            assertEquals(80f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should increment by lift increment override when all set goals are met for a standard lift and a set was deleted`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)

        result.fastForEach { p ->
            assertEquals(80f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should increment by lift increment override when all set goals are met for a standard lift and a set was added`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0,
                setCount = 4,
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)

        result.fastForEach { p ->
            assertEquals(80f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should not change when all set goals are not met for a standard lift`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)

        result.fastForEach { p ->
            assertEquals(75f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should increment by increment override when all standard custom lift set goals are met`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeTop = 8,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSets = listOf(
                CustomLiftSet(
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSet(
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 1,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSet(
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)

        result.fastForEach { p ->
            assertEquals(80f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should increment by increment override when all standard and drop set custom lift set goals are met`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeTop = 8,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSets = listOf(
                CustomLiftSet(
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSet(
                    workoutLiftId = 0,
                    type = SetType.DROP_SET,
                    dropPercentage = .1f,
                    position = 1,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSet(
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 100f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 90f, microCycle = 0, mesoCycle= 0, setType = SetType.DROP_SET, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 100f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)

        result.fastForEach { p ->
            if (p is LoggingStandardSetDto)
                assertEquals(105f, p.weightRecommendation)
            else
                assertEquals(95f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should increment by increment override when all standard and drop set custom lift set goals are met with double drop sets`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeTop = 8,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSets = listOf(
                CustomLiftSet(
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSet(
                    workoutLiftId = 0,
                    type = SetType.DROP_SET,
                    dropPercentage = .1f,
                    position = 1,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSet(
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 100f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 90f, microCycle = 0, mesoCycle= 0, setType = SetType.DROP_SET, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 80f, microCycle = 0, mesoCycle= 0, setType = SetType.DROP_SET, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)

        result.forEachIndexed { index, p ->
            when (index) {
                0 -> assertEquals(105f, p.weightRecommendation)
                1 -> assertEquals(95f, p.weightRecommendation)
                2 -> assertEquals(85f, p.weightRecommendation)
            }
        }
    }

    @Test
    fun `weight should NOT increment when a standard lift goal with a drop set is missed`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeTop = 8,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSets = listOf(
                CustomLiftSet(
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSet(
                    workoutLiftId = 0,
                    type = SetType.DROP_SET,
                    dropPercentage = .1f,
                    position = 1,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSet(
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 100f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 90f, microCycle = 0, mesoCycle= 0, setType = SetType.DROP_SET, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 100f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)

        result.fastForEach { p ->
            if (p is LoggingStandardSetDto)
                assertEquals(100f, p.weightRecommendation)
            else
                assertEquals(90f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should not change when all standard custom lift set goals are met`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeTop = 8,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSets = listOf(
                CustomLiftSet(
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSet(
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 1,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSet(
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)

        result.fastForEach { p ->
            assertEquals(75f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should increment by increment override when all myorep set goals are met`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0,
                setCount = 1,
                repRangeTop = 8,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSets = listOf(
                CustomLiftSet(
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
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 15, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 10, rpe = 8f, liftPosition = 0, setPosition = 0, myoRepSetPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 0, myoRepSetPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 4, rpe = 8f, liftPosition = 0, setPosition = 0, myoRepSetPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)

        result.fastForEach { p ->
            assertEquals(80f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should increment by increment override when all myorep match set goals are met`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0,
                setCount = 1,
                repRangeTop = 8,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSets = listOf(
                CustomLiftSet(
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
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 15, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 10, rpe = 8f, liftPosition = 0, setPosition = 0, myoRepSetPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 4, rpe = 8f, liftPosition = 0, setPosition = 0, myoRepSetPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 1, rpe = 8f, liftPosition = 0, setPosition = 0, myoRepSetPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)

        result.fastForEach { p ->
            assertEquals(80f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should not change when all myorep match sets are met but standard sets are not`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0,
                setCount = 2,
                repRangeTop = 8,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSets = listOf(
                CustomLiftSet(
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSet(
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 15, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 5, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 3, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)

        result.fastForEach { p ->
            assertEquals(75f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should not change when all myorep sets are met but standard sets are not`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0,
                setCount = 2,
                repRangeTop = 8,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSets = listOf(
                CustomLiftSet(
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSet(
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 15, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 10, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 5, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)

        result.fastForEach { p ->
            assertEquals(75f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should not change when all myorep match sets are not met`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0,
                setCount = 2,
                repRangeTop = 8,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSets = listOf(
                CustomLiftSet(
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 15, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 4, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 2, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 2, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)

        result.fastForEach { p ->
            assertEquals(75f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should not change when myorep activation set rep goal is missed`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0,
                setCount = 2,
                repRangeTop = 8,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSets = listOf(
                CustomLiftSet(
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSet(
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 14, rpe = 8f, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 10, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 5, rpe = 8f, liftPosition = 0, setPosition = 1, myoRepSetPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)

        result.fastForEach { p ->
            assertEquals(75f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should be null for standard lift when there is no set data`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0,
                setCount = 4,
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
        val previousSetData = listOf<StandardSetResultDto>()
        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)

        result.fastForEach { p ->
            assertEquals(null, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should be null for custom lift when there is no set data`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0,
                setCount = 2,
                repRangeTop = 8,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSets = listOf(
                CustomLiftSet(
                    workoutLiftId = 0,
                    type = SetType.STANDARD,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeTop = 8,
                    repRangeBottom = 6,
                ),
                CustomLiftSet(
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
        val previousSetData = listOf<StandardSetResultDto>()
        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)

        result.fastForEach { p ->
            assertEquals(null, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should not change when myorep match set rep count doesn't meet goal`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0,
                setCount = 1,
                repRangeTop = 8,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSets = listOf(
                CustomLiftSet(
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 15, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 1, rpe = 8f, liftPosition = 0, setPosition = 0, myoRepSetPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 1, rpe = 8f, liftPosition = 0, setPosition = 0, myoRepSetPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)

        result.fastForEach { p ->
            assertEquals(75f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should not change when myorep match set rpe does not meet goal`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0,
                setCount = 1,
                repRangeTop = 8,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
            customLiftSets = listOf(
                CustomLiftSet(
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 15, rpe = 8f, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, setType = SetType.STANDARD, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 10, rpe = 8f, liftPosition = 0, setPosition = 0, myoRepSetPosition = 0, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 4, rpe = 8f, liftPosition = 0, setPosition = 0, myoRepSetPosition = 1, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 1, rpe = 9f, liftPosition = 0, setPosition = 0, myoRepSetPosition = 2, weightRecommendation = null, weight = 75f, microCycle = 0, mesoCycle= 0, isDeload = false),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)

        result.fastForEach { p ->
            assertEquals(75f, p.weightRecommendation)
        }
    }
}
