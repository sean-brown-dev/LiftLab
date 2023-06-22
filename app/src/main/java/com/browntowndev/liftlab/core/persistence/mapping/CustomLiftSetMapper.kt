package com.browntowndev.liftlab.core.persistence.mapping

import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.persistence.dtos.DropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericCustomLiftSet
import com.browntowndev.liftlab.core.persistence.entities.CustomLiftSet

class CustomLiftSetMapper {
    fun map(entity: CustomLiftSet): GenericCustomLiftSet {
        return when (entity.type) {
            SetType.STANDARD_SET -> toStandardSetDto(entity)
            SetType.MYOREP_SET -> toMyoRepSetDto(entity)
            SetType.DROP_SET -> toDropSetDto(entity)
        }
    }

    fun map(setDto: GenericCustomLiftSet): CustomLiftSet {
        return when (setDto) {
            is StandardSetDto -> toUpdateEntity(setDto)
            is MyoRepSetDto -> toUpdateEntity(setDto)
            is DropSetDto -> toUpdateEntity(setDto)
            else -> throw ClassNotFoundException("${setDto::class.simpleName} is not implemented in ${CustomLiftSetMapper::class.simpleName}")
        }
    }

    private fun toStandardSetDto(entity: CustomLiftSet): StandardSetDto {
        return StandardSetDto(
            id = entity.id,
            workoutLiftId = entity.workoutLiftId,
            position = entity.position,
            rpeTarget = entity.rpeTarget,
            repRangeBottom = entity.repRangeBottom,
            repRangeTop = entity.repRangeTop,
        )
    }

    private fun toMyoRepSetDto(entity: CustomLiftSet): MyoRepSetDto {
        return MyoRepSetDto(
            id = entity.id,
            workoutLiftId = entity.workoutLiftId,
            position = entity.position,
            rpeTarget = entity.rpeTarget,
            repFloor = entity.repFloor,
            repRangeBottom = entity.repRangeBottom,
            repRangeTop = entity.repRangeTop,
            maxSets = entity.maxSets,
            setMatching = entity.setMatching,
            matchSetGoal = entity.matchSetGoal,
        )
    }

    private fun toDropSetDto(entity: CustomLiftSet): DropSetDto {
        return DropSetDto(
            id = entity.id,
            workoutLiftId = entity.workoutLiftId,
            position = entity.position,
            dropPercentage = entity.dropPercentage!!,
            rpeTarget = entity.rpeTarget,
            repRangeBottom = entity.repRangeBottom,
            repRangeTop = entity.repRangeTop,
        )
    }

    private fun toUpdateEntity(setDto: StandardSetDto): CustomLiftSet {
        return CustomLiftSet(
            id = setDto.id,
            workoutLiftId = setDto.workoutLiftId,
            type = SetType.STANDARD_SET,
            position = setDto.position,
            rpeTarget = setDto.rpeTarget,
            repRangeBottom = setDto.repRangeBottom,
            repRangeTop = setDto.repRangeTop,
        )
    }

    private fun toUpdateEntity(setDto: MyoRepSetDto): CustomLiftSet {
        return CustomLiftSet(
            id = setDto.id,
            workoutLiftId = setDto.workoutLiftId,
            type = SetType.MYOREP_SET,
            position = setDto.position,
            repFloor = setDto.repFloor,
            rpeTarget = setDto.rpeTarget,
            repRangeBottom = setDto.repRangeBottom,
            repRangeTop = setDto.repRangeTop,
            maxSets = setDto.maxSets,
            setMatching = setDto.setMatching,
            matchSetGoal = setDto.matchSetGoal,
        )
    }

    private fun toUpdateEntity(setDto: DropSetDto): CustomLiftSet {
        return CustomLiftSet(
            id = setDto.id,
            workoutLiftId = setDto.workoutLiftId,
            type = SetType.DROP_SET,
            position = setDto.position,
            rpeTarget = setDto.rpeTarget,
            repRangeBottom = setDto.repRangeBottom,
            repRangeTop = setDto.repRangeTop,
            dropPercentage = setDto.dropPercentage,
        )
    }
}