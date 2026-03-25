package com.browntowndev.liftlab.core.domain.models.programConfiguration

import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import kotlinx.serialization.Serializable

@Serializable
data class ProgramPayload(
    val program: ProgramCore,
    val workouts: List<WorkoutCore>,
    val weeklyVolumeSummary: List<WeeklyVolumeItem>
)

@Serializable
data class ProgramCore(val name: String, val deloadWeek: Int)

@Serializable
data class WorkoutCore(
    val name: String,
    val position: Int,
    val workoutLifts: List<WorkoutLiftCore>
)

@Serializable
data class WorkoutLiftCore(
    val liftId: Long,
    val progressionScheme: String,
    val position: Int,
    val setCount: Int,
    val deloadWeek: Int? = null,
    val rpeTarget: Double,
    val repRangeBottom: Int,
    val repRangeTop: Int,
    val stepSize: Int? = null
)

@Serializable
data class WeeklyVolumeItem(
    val muscle: String,
    val primarySets: Double,
    val secondarySets: Double
)

/** Inputs to the generator use case. */
data class ProgramGenerationRequest(
    val microcycleWorkoutCount: Int,
    val specializationMuscles: Set<VolumeType>,
    val deloadEvery: Int = 4,
    val liftCatalog: List<Lift>,
)

data class ValidationIssue(val kind: String, val message: String)
data class ValidationResult(
    val isValid: Boolean,
    val issues: List<ValidationIssue> = emptyList(),
    val volumesByGroup: Map<String, Double> = emptyMap(),
)
