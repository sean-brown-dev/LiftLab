package com.browntowndev.liftlab.progression

import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.queryable.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.Lift
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLift
import com.browntowndev.liftlab.core.persistence.mapping.CustomLiftSetMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLiftMapper
import com.browntowndev.liftlab.core.progression.WaveLoadingProgressionCalculator
import org.junit.Assert
import org.junit.Test

class WaveLoadingProgressionCalculatorTests {
    private val calculator = WaveLoadingProgressionCalculator(4)
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 0, weight = 75f, microCycle = 0),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 1, weight = 75f, microCycle = 0),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 2, weight = 75f, microCycle = 0),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData)
        result.forEach {
            Assert.assertEquals(80f, it.weightRecommendation)
        }
    }

    @Test
    fun `weight decrements on deload week`() {
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 0, weight = 75f, microCycle = 2),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 1, weight = 75f, microCycle = 2),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 8, rpe = 8f, setPosition = 2, weight = 75f, microCycle = 2),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData)
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
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, setPosition = 0, weight = 75f, microCycle = 3),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, setPosition = 1, weight = 75f, microCycle = 3),
            StandardSetResultDto(workoutId = 0, liftId = 0, reps = 6, rpe = 8f, setPosition = 2, weight = 75f, microCycle = 3),
        )

        val result = calculator.calculate(workoutLiftMapper.map(lift), previousSetData)
        result.forEach {
            Assert.assertEquals(80f, it.weightRecommendation)
        }
    }
}