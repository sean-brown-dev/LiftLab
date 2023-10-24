package com.browntowndev.liftlab.core.persistence.mapping

import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.persistence.dtos.LinearProgressionSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.entities.PreviousSetResult

class SetResultMapper {
    fun map(from: PreviousSetResult): SetResult {
        return when (from.setType) {
            SetType.STANDARD,
            SetType.DROP_SET -> {
                if (from.missedLpGoals != null) {
                    toLpSetResult(from)
                } else {
                    toStandardSetResult(from)
                }
            }

            SetType.MYOREP -> toMyoRepSetResult(from)
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
            id = from.id,
            workoutId = from.workoutId,
            liftId = from.liftId,
            mesoCycle = from.mesoCycle,
            microCycle = from.microCycle,
            liftPosition = from.liftPosition,
            setPosition = from.setPosition,
            weight = from.weight,
            rpe = from.rpe,
            reps = from.reps,
            missedLpGoals = from.missedLpGoals!!,
        )
    }

    private fun toMyoRepSetResult(from: PreviousSetResult): MyoRepSetResultDto {
        return MyoRepSetResultDto(
            id = from.id,
            workoutId = from.workoutId,
            liftId = from.liftId,
            mesoCycle = from.mesoCycle,
            microCycle = from.microCycle,
            liftPosition = from.liftPosition,
            setPosition = from.setPosition,
            myoRepSetPosition = from.myoRepSetPosition,
            weight = from.weight,
            rpe = from.rpe,
            reps = from.reps,
        )
    }

    private fun toStandardSetResult(from: PreviousSetResult): StandardSetResultDto {
        return StandardSetResultDto(
            id = from.id,
            setType = from.setType,
            workoutId = from.workoutId,
            liftId = from.liftId,
            mesoCycle = from.mesoCycle,
            microCycle = from.microCycle,
            liftPosition = from.liftPosition,
            setPosition = from.setPosition,
            weight = from.weight,
            rpe = from.rpe,
            reps = from.reps,
        )
    }

    private fun toEntity(from: StandardSetResultDto): PreviousSetResult {
        return PreviousSetResult(
            id = from.id,
            setType = from.setType,
            workoutId = from.workoutId,
            liftId = from.liftId,
            mesoCycle = from.mesoCycle,
            microCycle = from.microCycle,
            liftPosition = from.liftPosition,
            setPosition = from.setPosition,
            weight = from.weight,
            rpe = from.rpe,
            reps = from.reps,
        )
    }

    private fun toEntity(from: MyoRepSetResultDto): PreviousSetResult {
        return PreviousSetResult(
            id = from.id,
            setType = SetType.MYOREP,
            workoutId = from.workoutId,
            liftId = from.liftId,
            mesoCycle = from.mesoCycle,
            microCycle = from.microCycle,
            liftPosition = from.liftPosition,
            setPosition = from.setPosition,
            myoRepSetPosition = from.myoRepSetPosition,
            weight = from.weight,
            rpe = from.rpe,
            reps = from.reps,
        )
    }

    private fun toEntity(from: LinearProgressionSetResultDto): PreviousSetResult {
        return PreviousSetResult(
            id = from.id,
            setType = SetType.STANDARD,
            workoutId = from.workoutId,
            liftId = from.liftId,
            mesoCycle = from.mesoCycle,
            microCycle = from.microCycle,
            liftPosition = from.liftPosition,
            setPosition = from.setPosition,
            weight = from.weight,
            rpe = from.rpe,
            reps = from.reps,
            missedLpGoals = from.missedLpGoals,
        )
    }
}