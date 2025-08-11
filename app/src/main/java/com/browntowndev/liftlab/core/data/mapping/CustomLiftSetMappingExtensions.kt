package com.browntowndev.liftlab.core.data.mapping

import com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.interfaces.CalculationCustomLiftSet
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.models.workout.DropSet
import com.browntowndev.liftlab.core.domain.models.workout.MyoRepSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardSet
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationDropSet
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationStandardSet


fun CustomLiftSetEntity.toCalculationDomainModel(): CalculationCustomLiftSet =
    when (type) {
        SetType.STANDARD -> toCalculationStandardSetDto()
        SetType.MYOREP -> toCalculationMyoRepSetDto()
        SetType.DROP_SET -> toCalculationDropSetDto()
    }

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

private fun CustomLiftSetEntity.toCalculationStandardSetDto(): CalculationStandardSet {
    return CalculationStandardSet(
        id = id,
        position = position,
        rpeTarget = rpeTarget,
        repRangeBottom = repRangeBottom,
        repRangeTop = repRangeTop,
    )
}

private fun CustomLiftSetEntity.toCalculationMyoRepSetDto(): CalculationMyoRepSet {
    return CalculationMyoRepSet(
        id = id,
        position = position,
        rpeTarget = rpeTarget,
        repFloor = repFloor,
        repRangeBottom = repRangeBottom,
        repRangeTop = repRangeTop,
        maxSets = maxSets,
        setMatching = setMatching,
        setGoal = setGoal!!
    )
}

private fun CustomLiftSetEntity.toCalculationDropSetDto(): CalculationDropSet {
    return CalculationDropSet(
        id = id,
        position = position,
        dropPercentage = dropPercentage!!,
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
