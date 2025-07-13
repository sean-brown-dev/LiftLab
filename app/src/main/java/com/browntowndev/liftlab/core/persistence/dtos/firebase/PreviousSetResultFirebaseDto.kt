package com.browntowndev.liftlab.core.persistence.dtos.firebase

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.common.enums.SetType

@Keep
data class PreviousSetResultFirebaseDto(
    var id: Long = 0L,
    var workoutId: Long = 0L,
    var liftId: Long = 0L,
    var setType: SetType = SetType.STANDARD,
    var liftPosition: Int = 0,
    var setPosition: Int = 0,
    var myoRepSetPosition: Int? = null,
    var weightRecommendation: Float? = null,
    var weight: Float = 0f,
    var reps: Int = 0,
    var rpe: Float = 0f,
    var oneRepMax: Int = 0,
    var mesoCycle: Int = 0,
    var microCycle: Int = 0,
    var missedLpGoals: Int? = null,
    var isDeload: Boolean = false
): BaseFirebaseDto()
