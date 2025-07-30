package com.browntowndev.liftlab.core.domain.models.interfaces

import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.domain.models.DropSet
import com.browntowndev.liftlab.core.domain.models.MyoRepSet
import com.browntowndev.liftlab.core.domain.models.StandardSet

interface GenericLiftSet {
    val id: Long
    val workoutLiftId: Long
    val position: Int
    val rpeTarget: Float
    val repRangeBottom: Int
    val repRangeTop: Int
}

fun GenericLiftSet.transformToType(newSetType: SetType): GenericLiftSet {
    return when (this) {
        is StandardSet ->
            when (newSetType) {
                SetType.DROP_SET -> DropSet(
                    id = this.id,
                    workoutLiftId = this.workoutLiftId,
                    position = this.position,
                    dropPercentage = .1f, // TODO: Add a "drop percentage" setting and use it here
                    rpeTarget = this.rpeTarget,
                    repRangeBottom = this.repRangeBottom,
                    repRangeTop = this.repRangeTop,
                )
                SetType.MYOREP -> MyoRepSet(
                    id = this.id,
                    workoutLiftId = this.workoutLiftId,
                    position = this.position,
                    repFloor = 5, // TODO: Add a "myo-rep floor" setting and use it here
                    repRangeTop = this.repRangeTop,
                    repRangeBottom = this.repRangeBottom,
                    rpeTarget = this.rpeTarget,
                    setGoal = 3,
                )
                SetType.STANDARD -> this
            }
        is MyoRepSet ->
            when (newSetType) {
                SetType.DROP_SET -> DropSet(
                    id = this.id,
                    workoutLiftId = this.workoutLiftId,
                    position = this.position,
                    dropPercentage = .1f, // TODO: Add a "drop percentage" setting and use it here
                    rpeTarget = 8f, // TODO: Add a "rpe target" setting and use it here
                    repRangeBottom = this.repRangeBottom,
                    repRangeTop = this.repRangeTop,
                )
                SetType.MYOREP -> this
                SetType.STANDARD -> StandardSet(
                    id = this.id,
                    workoutLiftId = this.workoutLiftId,
                    position = this.position,
                    rpeTarget = 8f, // TODO: Add a "rpe target" setting and use it here
                    repRangeBottom = this.repRangeBottom,
                    repRangeTop = this.repRangeTop,
                )
            }
        is DropSet ->
            when (newSetType) {
                SetType.DROP_SET -> this
                SetType.MYOREP -> MyoRepSet(
                    id = this.id,
                    workoutLiftId = this.workoutLiftId,
                    position = this.position,
                    repFloor = 5, // TODO: Add a "myo-rep floor" setting and use it here
                    repRangeBottom = this.repRangeBottom,
                    repRangeTop = this.repRangeTop,
                    rpeTarget = this.rpeTarget,
                    setGoal = 3,
                )
                SetType.STANDARD -> StandardSet(
                    id = this.id,
                    workoutLiftId = this.workoutLiftId,
                    position = this.position,
                    rpeTarget = 8f, // TODO: Add a "rpe target" setting and use it here
                    repRangeBottom = this.repRangeBottom,
                    repRangeTop = this.repRangeTop,
                )
            }
        else -> throw Exception("${this::class.simpleName} is not recognized as a custom set type.")
    }
}