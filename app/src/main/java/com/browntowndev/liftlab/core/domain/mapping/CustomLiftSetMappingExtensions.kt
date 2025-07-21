package com.browntowndev.liftlab.core.domain.mapping

import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.domain.models.DropSet
import com.browntowndev.liftlab.core.domain.models.MyoRepSet
import com.browntowndev.liftlab.core.domain.models.StandardSet
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.persistence.room.entities.CustomLiftSetEntity

object CustomLiftSetMappingExtensions {
    /**
     * Maps a [CustomLiftSetEntity] from the persistence layer to a [GenericLiftSet]
     * domain model based on its [SetType].
     */
    fun CustomLiftSetEntity.toDomainModel(): GenericLiftSet {
        return when (type) {
            SetType.STANDARD -> toStandardSetDto()
            SetType.MYOREP -> toMyoRepSetDto()
            SetType.DROP_SET -> toDropSetDto()
        }
    }

    /**
     * Maps a [GenericLiftSet] domain model to a [CustomLiftSetEntity] for persistence.
     * This is the reverse of [toDomainModel].
     */
    fun GenericLiftSet.toEntity(): CustomLiftSetEntity {
        return when (this) {
            is StandardSet -> toEntity()
            is MyoRepSet -> toEntity()
            is DropSet -> toEntity()
            else -> throw ClassNotFoundException(
                "${this::class.simpleName} is not implemented for mapping to CustomLiftSetEntity"
            )
        }
    }

// Private helper extension functions to keep the main functions clean

    private fun CustomLiftSetEntity.toStandardSetDto(): StandardSet {
        return StandardSet(
            id = id,
            workoutLiftId = workoutLiftId,
            position = position,
            rpeTarget = rpeTarget,
            repRangeBottom = repRangeBottom,
            repRangeTop = repRangeTop,
        )
    }

    private fun CustomLiftSetEntity.toMyoRepSetDto(): MyoRepSet {
        return MyoRepSet(
            id = id,
            workoutLiftId = workoutLiftId,
            position = position,
            rpeTarget = rpeTarget,
            repFloor = repFloor,
            repRangeBottom = repRangeBottom,
            repRangeTop = repRangeTop,
            maxSets = maxSets,
            setMatching = setMatching,
            setGoal = setGoal ?: 3, // Keep the non-null default
        )
    }

    private fun CustomLiftSetEntity.toDropSetDto(): DropSet {
        return DropSet(
            id = id,
            workoutLiftId = workoutLiftId,
            position = position,
            dropPercentage = dropPercentage!!, // Retain the non-null assertion if it's a required field in the model
            rpeTarget = rpeTarget,
            repRangeBottom = repRangeBottom,
            repRangeTop = repRangeTop,
        )
    }

    private fun StandardSet.toEntity(): CustomLiftSetEntity {
        return CustomLiftSetEntity(
            id = id,
            workoutLiftId = workoutLiftId,
            type = SetType.STANDARD,
            position = position,
            rpeTarget = rpeTarget,
            repRangeBottom = repRangeBottom,
            repRangeTop = repRangeTop,
        )
    }

    private fun MyoRepSet.toEntity(): CustomLiftSetEntity {
        return CustomLiftSetEntity(
            id = id,
            workoutLiftId = workoutLiftId,
            type = SetType.MYOREP,
            position = position,
            repFloor = repFloor,
            rpeTarget = rpeTarget,
            repRangeBottom = repRangeBottom,
            repRangeTop = repRangeTop,
            maxSets = maxSets,
            setMatching = setMatching,
            setGoal = setGoal,
        )
    }

    private fun DropSet.toEntity(): CustomLiftSetEntity {
        return CustomLiftSetEntity(
            id = id,
            workoutLiftId = workoutLiftId,
            type = SetType.DROP_SET,
            position = position,
            rpeTarget = rpeTarget,
            repRangeBottom = repRangeBottom,
            repRangeTop = repRangeTop,
            dropPercentage = dropPercentage,
        )
    }
}