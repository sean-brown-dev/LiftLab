package com.browntowndev.liftlab.core.persistence.mapping

import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.persistence.dtos.DropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericCustomLiftSet
import com.browntowndev.liftlab.core.persistence.entities.CustomLiftSet
import com.browntowndev.liftlab.core.persistence.entities.update.UpdateCustomLiftSet

class CustomLiftSetMapper {
    fun map(entity: CustomLiftSet): GenericCustomLiftSet {
        return when (entity.type) {
            SetType.STANDARD_SET -> toStandardSetDto(entity)
            SetType.MYOREP_SET -> toMyoRepSetDto(entity)
            SetType.DROP_SET -> toDropSetDto(entity)
        }
    }

    fun map(setDto: GenericCustomLiftSet): UpdateCustomLiftSet {
        return when (setDto) {
            is StandardSetDto -> toEntity(setDto)
            is MyoRepSetDto -> toEntity(setDto)
            is DropSetDto -> fromDropSetDto(setDto)
            else -> throw ClassNotFoundException("${setDto::class.simpleName} is not implemented in ${CustomLiftSetMapper::class.simpleName}")
        }
    }

    private fun toStandardSetDto(entity: CustomLiftSet): StandardSetDto {
        return StandardSetDto(
            id = entity.id,
            position = entity.position,
            rpeTarget = entity.rpeTarget!!,
            repRangeBottom = entity.repRangeBottom!!,
            repRangeTop = entity.repRangeTop!!,
        )
    }

    private fun toMyoRepSetDto(entity: CustomLiftSet): MyoRepSetDto {
        return MyoRepSetDto(
            id = entity.id,
            position = entity.position,
            repFloor = entity.repFloor!!,
            repRangeBottom = entity.repRangeBottom!!,
            repRangeTop = entity.repRangeTop!!,
            maxSets = entity.maxSets,
            setMatching = entity.setMatching,
            matchSetGoal = entity.matchSetGoal,
        )
    }

    private fun toDropSetDto(entity: CustomLiftSet): DropSetDto {
        return DropSetDto(
            id = entity.id,
            position = entity.position,
            dropPercentage = entity.dropPercentage!!,
            rpeTarget = entity.rpeTarget!!,
            repRangeBottom = entity.repRangeBottom!!,
            repRangeTop = entity.repRangeTop!!,
        )
    }

    private fun toEntity(setDto: StandardSetDto): UpdateCustomLiftSet {
        return setDto.id?.let { id ->
            UpdateCustomLiftSet(
                id = id,
                type = SetType.STANDARD_SET,
                position = setDto.position,
                repFloor = null,
                rpeTarget = setDto.rpeTarget,
                repRangeBottom = setDto.repRangeBottom,
                repRangeTop = setDto.repRangeTop,
                dropPercentage = null
            )
        } ?: run {
            UpdateCustomLiftSet(
                type = SetType.STANDARD_SET,
                position = setDto.position,
                repFloor = null,
                rpeTarget = setDto.rpeTarget,
                repRangeBottom = setDto.repRangeBottom,
                repRangeTop = setDto.repRangeTop,
                dropPercentage = null
            )
        }
    }

    private fun toEntity(setDto: MyoRepSetDto): UpdateCustomLiftSet {
        return setDto.id?.let { id ->
            UpdateCustomLiftSet(
                id = id,
                type = SetType.STANDARD_SET,
                position = setDto.position,
                repFloor = setDto.repFloor,
                rpeTarget = null,
                repRangeBottom = setDto.repRangeBottom,
                repRangeTop = setDto.repRangeTop,
                dropPercentage = null,
                maxSets = setDto.maxSets,
                setMatching = setDto.setMatching,
            )
        } ?: run {
            UpdateCustomLiftSet(
                type = SetType.STANDARD_SET,
                position = setDto.position,
                repFloor = setDto.repFloor,
                rpeTarget = null,
                repRangeBottom = setDto.repRangeBottom,
                repRangeTop = setDto.repRangeTop,
                dropPercentage = null,
                maxSets = setDto.maxSets,
                setMatching = setDto.setMatching,
            )
        }
    }

    private fun fromDropSetDto(setDto: DropSetDto): UpdateCustomLiftSet {
        return setDto.id?.let {id ->
            UpdateCustomLiftSet(
                id = id,
                type = SetType.STANDARD_SET,
                position = setDto.position,
                repFloor = null,
                rpeTarget = setDto.rpeTarget,
                repRangeBottom = setDto.repRangeBottom,
                repRangeTop = setDto.repRangeTop,
                dropPercentage = setDto.dropPercentage,
            )
        } ?: run {
            UpdateCustomLiftSet(
                type = SetType.STANDARD_SET,
                position = setDto.position,
                repFloor = null,
                rpeTarget = setDto.rpeTarget,
                repRangeBottom = setDto.repRangeBottom,
                repRangeTop = setDto.repRangeTop,
                dropPercentage = setDto.dropPercentage,
            )
        }
    }
}