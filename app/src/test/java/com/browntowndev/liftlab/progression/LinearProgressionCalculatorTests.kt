package com.browntowndev.liftlab.progression

import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.persistence.dtos.LinearProgressionSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.queryable.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.CustomLiftSet
import com.browntowndev.liftlab.core.persistence.entities.Lift
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLift
import com.browntowndev.liftlab.core.persistence.mapping.CustomLiftSetMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLiftMapper
import com.browntowndev.liftlab.core.progression.LinearProgressionCalculator
import org.junit.Assert
import org.junit.Test

class LinearProgressionCalculatorTests {
    private val calculator = LinearProgressionCalculator()
    private val workoutLiftMapper = WorkoutLiftMapper(CustomLiftSetMapper())

    @Test
    fun `all sets increment`() {
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
            LinearProgressionSetResultDto(missedLpGoals = 0, reps = 8, rpe = 8f, setPosition = 0, weight = 75f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0),
            LinearProgressionSetResultDto(missedLpGoals = 0, reps = 8, rpe = 8f, setPosition = 1, weight = 75f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0),
            LinearProgressionSetResultDto(missedLpGoals = 0, reps = 8, rpe = 8f, setPosition = 2, weight = 75f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)
        result.forEach {
            Assert.assertEquals(80f, it.weightRecommendation)
        }
    }

    @Test
    fun `sets do not increment on first failure`() {
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
            LinearProgressionSetResultDto(missedLpGoals = 1, reps = 8, rpe = 8f, setPosition = 0, weight = 75f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0),
            LinearProgressionSetResultDto(missedLpGoals = 1, reps = 8, rpe = 8f, setPosition = 1, weight = 75f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0),
            LinearProgressionSetResultDto(missedLpGoals = 1, reps = 7, rpe = 8f, setPosition = 2, weight = 75f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)
        result.forEach {
            Assert.assertEquals(75f, it.weightRecommendation)
        }
    }

    @Test
    fun `weight drops 10% on second failure`() {
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
            LinearProgressionSetResultDto(missedLpGoals = 2, reps = 8, rpe = 8f, setPosition = 0, weight = 100f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0),
            LinearProgressionSetResultDto(missedLpGoals = 2, reps = 8, rpe = 8f, setPosition = 1, weight = 100f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0),
            LinearProgressionSetResultDto(missedLpGoals = 2, reps = 7, rpe = 8f, setPosition = 2, weight = 100f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)
        result.forEach {
            Assert.assertEquals(90f, it.weightRecommendation)
        }
    }

    @Test
    fun `weight is null when an empty list of set results is passed in`() {
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
        val previousSetData = listOf<LinearProgressionSetResultDto>()
        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)
        result.forEach {
            Assert.assertEquals(null, it.weightRecommendation)
        }
    }

    @Test
    fun `throws exception if invalid set type passed in`() {
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
            StandardSetResultDto(missedLpGoals = 2, reps = 8, rpe = 8f, setPosition = 0, weight = 100f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0, setType = SetType.STANDARD),
            LinearProgressionSetResultDto(missedLpGoals = 2, reps = 8, rpe = 8f, setPosition = 1, weight = 100f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0),
            LinearProgressionSetResultDto(missedLpGoals = 2, reps = 7, rpe = 8f, setPosition = 2, weight = 100f, microCycle = 0, workoutId = 0, liftId = 0, mesoCycle = 0),
        )

        Assert.assertThrows(Exception::class.java) {
            calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)
        }
    }

    @Test
    fun `weight recommendation should be null for all lifts`() {
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
            customLiftSets = listOf(
                CustomLiftSet(
                    workoutLiftId = 0,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeBottom = 6,
                    repRangeTop = 8,
                    type = SetType.STANDARD,
                )
            )
        )
        val previousSetData = listOf<LinearProgressionSetResultDto>()
        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData, false)
        result.forEach {
            Assert.assertEquals(null, it.weightRecommendation)
        }
    }
}