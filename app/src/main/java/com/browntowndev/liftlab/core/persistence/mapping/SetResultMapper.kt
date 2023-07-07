package com.browntowndev.liftlab.core.persistence.mapping

import com.browntowndev.liftlab.core.persistence.dtos.LinearProgressionSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.entities.PreviousSetResult

class SetResultMapper {
    fun map(from: PreviousSetResult): SetResult {
        return if (from.missedLpGoals != null) {
            toLpSetResult(from)
        } else if (from.myoRepSetPosition != null) {
            toMyoRepSetResult(from)
        } else {
            toStandardSetResult(from)
        }
    }

    fun map(from: SetResult): PreviousSetResult {
        return when (from) {
            is StandardSetResultDto -> toEntity(from)
            is MyoRepSetResultDto ->  toEntity(from)
            is LinearProgressionSetResultDto -> toEntity(from)
            else -> throw Exception("${from::class.simpleName} is not defined.")
        }
    }

    private fun toLpSetResult(from: PreviousSetResult): LinearProgressionSetResultDto {
        return LinearProgressionSetResultDto(
            workoutId = from.workoutId,
            liftId = from.liftId,
            mesoCycle = from.mesoCycle,
            microCycle = from.microCycle,
            setPosition = from.setPosition,
            weight = from.weight,
            rpe = from.rpe,
            reps = from.reps,
            missedLpGoals = from.missedLpGoals!!,
        )
    }

    private fun toMyoRepSetResult(from: PreviousSetResult): MyoRepSetResultDto {
        return MyoRepSetResultDto(
            workoutId = from.workoutId,
            liftId = from.liftId,
            mesoCycle = from.mesoCycle,
            microCycle = from.microCycle,
            setPosition = from.setPosition,
            myoRepSetPosition = from.myoRepSetPosition!!,
            weight = from.weight,
            rpe = from.rpe,
            reps = from.reps,
        )
    }

    private fun toStandardSetResult(from: PreviousSetResult): StandardSetResultDto {
        return StandardSetResultDto(
            workoutId = from.workoutId,
            liftId = from.liftId,
            mesoCycle = from.mesoCycle,
            microCycle = from.microCycle,
            setPosition = from.setPosition,
            weight = from.weight,
            rpe = from.rpe,
            reps = from.reps,
        )
    }

    private fun toEntity(from: StandardSetResultDto): PreviousSetResult {
        return PreviousSetResult(
            workoutId = from.workoutId,
            liftId = from.liftId,
            mesoCycle = from.mesoCycle,
            microCycle = from.microCycle,
            setPosition = from.setPosition,
            weight = from.weight,
            rpe = from.rpe,
            reps = from.reps,
        )
    }

    private fun toEntity(from: MyoRepSetResultDto): PreviousSetResult {
        return PreviousSetResult(
            workoutId = from.workoutId,
            liftId = from.liftId,
            mesoCycle = from.mesoCycle,
            microCycle = from.microCycle,
            setPosition = from.setPosition,
            myoRepSetPosition = from.myoRepSetPosition,
            weight = from.weight,
            rpe = from.rpe,
            reps = from.reps,
        )
    }

    private fun toEntity(from: LinearProgressionSetResultDto): PreviousSetResult {
        return PreviousSetResult(
            workoutId = from.workoutId,
            liftId = from.liftId,
            mesoCycle = from.mesoCycle,
            microCycle = from.microCycle,
            setPosition = from.setPosition,
            weight = from.weight,
            rpe = from.rpe,
            reps = from.reps,
            missedLpGoals = from.missedLpGoals,
        )
    }
}