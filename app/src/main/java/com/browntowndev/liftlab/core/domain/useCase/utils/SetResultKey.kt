package com.browntowndev.liftlab.core.domain.useCase.utils

import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult

data class SetResultKey(
    val liftId: Long,
    val liftPosition: Int,
    val setPosition: Int,
    val myoRepSetPosition: Int?,
)

fun SetResultKey.matchesResult(result: SetResult): Boolean {
    return liftId == result.liftId &&
            liftPosition == result.liftPosition &&
            setPosition == result.setPosition &&
            (myoRepSetPosition == (result as? MyoRepSetResult)?.myoRepSetPosition)
}
