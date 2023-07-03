package com.browntowndev.liftlab.progression

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.queryable.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.CustomLiftSet
import com.browntowndev.liftlab.core.persistence.entities.Lift
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLift
import com.browntowndev.liftlab.core.persistence.mapping.CustomLiftSetMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLiftMapper
import com.browntowndev.liftlab.core.progression.DynamicDoubleProgressionCalculator
import org.junit.Assert
import org.junit.Test

class DynamicDoubleProgressionCalculatorTests {
    private val calculator = DynamicDoubleProgressionCalculator()
    private val workoutLiftMapper = WorkoutLiftMapper(CustomLiftSetMapper())

    @Test
    fun `weight should increment by lift increment override when set goals are met for a standard lift`() {
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
                incrementOverride = 5f,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 0, weight = 75f, microCycle = 0),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 1, weight = 75f, microCycle = 0),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 2, weight = 75f, microCycle = 0),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData)

        result.fastForEach { p ->
            Assert.assertEquals(80f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should be 0 for a standard lift when there is no set data`() {
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
                incrementOverride = 5f,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf<StandardSetResultDto>()
        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData)

        result.fastForEach { p ->
            Assert.assertEquals(0f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should increment by lift increment override when set goals are met for a standard lift and a set was deleted`() {
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
                incrementOverride = 5f,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 0, weight = 75f, microCycle = 0),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 1, weight = 75f, microCycle = 0),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 2, weight = 75f, microCycle = 0),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 2, weight = 75f, microCycle = 0),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData)

        result.fastForEach { p ->
            Assert.assertEquals(80f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should increment by lift increment override when set goals are met for a standard lift and a set was added`() {
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
                incrementOverride = 5f,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 0, weight = 75f, microCycle = 0),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 1, weight = 75f, microCycle = 0),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 2, weight = 75f, microCycle = 0),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData)

        result.fastForEach { p ->
            Assert.assertEquals(80f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should not change for last standard lift set when set goal is not met`() {
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
                incrementOverride = 5f,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf(
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 0, weight = 75f, microCycle = 0),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 1, weight = 75f, microCycle = 0),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, setPosition = 2, weight = 75f, microCycle = 0),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData)

        result.fastForEach { p ->
            Assert.assertEquals(if(p.setPosition == 2) 75f else 80f, p.weightRecommendation)
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
                incrementOverride = 5f,
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 0, weight = 75f, microCycle = 0),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 1, weight = 75f, microCycle = 0),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 2, weight = 75f, microCycle = 0),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData)

        result.fastForEach { p ->
            Assert.assertEquals(80f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should not change on last standard custom lift set goals are not met`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0,
                setCount = 3,
                repRangeTop = 8,
                incrementOverride = 5f,
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 0, weight = 75f, microCycle = 0),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 1, weight = 75f, microCycle = 0),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, setPosition = 2, weight = 75f, microCycle = 0),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData)

        result.fastForEach { p ->
            Assert.assertEquals(if(p.setPosition == 2) 75f else 80f, p.weightRecommendation)
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
                incrementOverride = 5f,
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
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 15, rpe = 8f, setPosition = 0, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 10, rpe = 8f, setPosition = 0, myoRepSetPosition = 0, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, setPosition = 0, myoRepSetPosition = 1, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 4, rpe = 8f, setPosition = 0, myoRepSetPosition = 2, weight = 75f, microCycle = 0),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData)

        result.fastForEach { p ->
            Assert.assertEquals(80f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should increment for myorep match set but not standard set`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0,
                setCount = 2,
                repRangeTop = 8,
                incrementOverride = 5f,
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, setPosition = 0, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 15, rpe = 8f, setPosition = 1, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, setPosition = 1, myoRepSetPosition = 0, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 5, rpe = 8f, setPosition = 1, myoRepSetPosition = 1, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 3, rpe = 8f, setPosition = 1, myoRepSetPosition = 2, weight = 75f, microCycle = 0),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData)

        result.fastForEach { p ->
            if (p.setPosition == 0)
                Assert.assertEquals(75f, p.weightRecommendation)
            else
                Assert.assertEquals(80f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should increment for myorep set but not for standard set`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0,
                setCount = 2,
                repRangeTop = 8,
                incrementOverride = 5f,
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, setPosition = 0, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 15, rpe = 8f, setPosition = 1, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 10, rpe = 8f, setPosition = 1, myoRepSetPosition = 0, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, setPosition = 1, myoRepSetPosition = 1, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, setPosition = 1, myoRepSetPosition = 2, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 5, rpe = 8f, setPosition = 1, myoRepSetPosition = 2, weight = 75f, microCycle = 0),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData)

        result.fastForEach { p ->
            if (p.setPosition == 0)
                Assert.assertEquals(75f, p.weightRecommendation)
            else
                Assert.assertEquals(80f,  p.weightRecommendation)
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
                incrementOverride = 5f,
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 0, weight = 75f, microCycle = 0),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 15, rpe = 8f, setPosition = 1, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, setPosition = 1, myoRepSetPosition = 0, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 4, rpe = 8f, setPosition = 1, myoRepSetPosition = 1, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 2, rpe = 8f, setPosition = 1, myoRepSetPosition = 2, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 2, rpe = 8f, setPosition = 1, myoRepSetPosition = 2, weight = 75f, microCycle = 0),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData)

        result.fastForEach { p ->
            Assert.assertEquals(75f, p.weightRecommendation)
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
                setCount = 1,
                repRangeTop = 8,
                incrementOverride = 5f,
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
                    repFloor = 5,
                ),
            )
        )
        val previousSetData = listOf(
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 0, weight = 75f, microCycle = 0),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 14, rpe = 8f, setPosition = 1, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 10, rpe = 8f, setPosition = 1, myoRepSetPosition = 0, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 7, rpe = 8f, setPosition = 1, myoRepSetPosition = 1, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 5, rpe = 8f, setPosition = 1, myoRepSetPosition = 2, weight = 75f, microCycle = 0),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData)

        result.fastForEach { p ->
            Assert.assertEquals(75f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should be 0 for standard lift when there is no set data`() {
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
                incrementOverride = 5f,
            ),
            lift = Lift(
                name = "",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 1
            ),
        )
        val previousSetData = listOf<StandardSetResultDto>()
        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData)

        result.fastForEach { p ->
            Assert.assertEquals(0f, p.weightRecommendation)
        }
    }

    @Test
    fun `weight should be 0 for custom lift when there is no set data`() {
        val lift = WorkoutLiftWithRelationships(
            workoutLift = WorkoutLift(
                workoutId = 0,
                liftId = 0,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                position = 0,
                setCount = 2,
                repRangeTop = 8,
                incrementOverride = 5f,
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
        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData)

        result.fastForEach { p ->
            Assert.assertEquals(0f, p.weightRecommendation)
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
                incrementOverride = 5f,
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 15, rpe = 8f, setPosition = 0, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 1, rpe = 8f, setPosition = 0, myoRepSetPosition = 0, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 1, rpe = 8f, setPosition = 0, myoRepSetPosition = 1, weight = 75f, microCycle = 0),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData)

        result.fastForEach { p ->
            Assert.assertEquals(75f, p.weightRecommendation)
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
                incrementOverride = 5f,
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 15, rpe = 8f, setPosition = 0, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 10, rpe = 8f, setPosition = 0, myoRepSetPosition = 0, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 4, rpe = 8f, setPosition = 0, myoRepSetPosition = 1, weight = 75f, microCycle = 0),
            MyoRepSetResultDto(workoutId = 0, liftId = 0, reps = 1, rpe = 9f, setPosition = 0, myoRepSetPosition = 2, weight = 75f, microCycle = 0),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData)

        result.fastForEach { p ->
            Assert.assertEquals(75f, p.weightRecommendation)
        }
    }
}