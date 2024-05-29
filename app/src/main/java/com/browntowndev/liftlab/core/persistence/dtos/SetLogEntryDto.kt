package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.progression.CalculationEngine

data class SetLogEntryDto(
    val id: Long,
    val liftId: Long,
    val workoutLiftDeloadWeek: Int? = null,
    val liftName: String,
    val liftMovementPattern: MovementPattern,
    val progressionScheme: ProgressionScheme,
    val setType: SetType,
    val liftPosition: Int,
    val setPosition: Int,
    val myoRepSetPosition: Int? = null,
    val repRangeTop: Int?,
    val repRangeBottom: Int?,
    val rpeTarget: Float,
    val weightRecommendation: Float?,
    val weight: Float,
    val reps: Int,
    val rpe: Float,
    private val persistedOneRepMax: Int? = null,
    val mesoCycle: Int,
    val microCycle: Int,
    val isPersonalRecord: Boolean = false,
    val setMatching: Boolean? = null,
    val maxSets: Int? = null,
    val repFloor: Int? = null,
    val dropPercentage: Float? = null,
    val isDeload: Boolean,
) {
    val oneRepMax: Int by lazy {
        persistedOneRepMax ?: CalculationEngine.getOneRepMax(
            weight = weight,
            reps = reps,
            rpe = rpe,
        )
    }
}
