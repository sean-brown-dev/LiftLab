package com.browntowndev.liftlab.core.domain.util

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.LiftMetricChartType
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeImpact
import com.browntowndev.liftlab.core.domain.models.metrics.LiftMetricChart
import com.browntowndev.liftlab.core.domain.models.metrics.VolumeMetricChart
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry
import java.time.Instant
import java.util.Date
import kotlin.time.Duration

fun createProgram(
    id: Long = TestDefaults.DEFAULT_ID,
    name: String = "Test Program",
    isActive: Boolean = true,
    deloadWeek: Int = 4,
    currentMicrocycle: Int = 0,
    currentMicrocyclePosition: Int = 0,
    currentMesocycle: Int = 0,
    workouts: List<Workout> = emptyList()
) = Program(
    id = id,
    name = name,
    isActive = isActive,
    deloadWeek = deloadWeek,
    currentMicrocycle = currentMicrocycle,
    currentMicrocyclePosition = currentMicrocyclePosition,
    currentMesocycle = currentMesocycle,
    workouts = workouts
)

fun createLift(
    id: Long = TestDefaults.DEFAULT_ID,
    name: String = "Squat",
    movementPattern: MovementPattern = MovementPattern.LEG_PUSH,
    volumeTypesBitmask: Int = 1,
    secondaryVolumeTypesBitmask: Int = 0,
    incrementOverride: Float? = null,
    restTime: Duration? = null,
    restTimerEnabled: Boolean = true,
    isBodyweight: Boolean = false,
    note: String = ""
) = Lift(
    id = id,
    name = name,
    movementPattern = movementPattern,
    volumeTypesBitmask = volumeTypesBitmask,
    secondaryVolumeTypesBitmask = secondaryVolumeTypesBitmask,
    incrementOverride = incrementOverride,
    restTime = restTime,
    restTimerEnabled = restTimerEnabled,
    isBodyweight = isBodyweight,
    note = note
)

fun createWorkoutLogEntry(
    id: Long = TestDefaults.DEFAULT_ID,
    date: Date = Date.from(Instant.now()),
    historicalWorkoutNameId: Long = TestDefaults.DEFAULT_ID,
    programWorkoutCount: Int = 4,
    programDeloadWeek: Int = 4,
    programName: String = "Test Program",
    workoutName: String = "Test Workout",
    programId: Long = TestDefaults.DEFAULT_ID,
    workoutId: Long = TestDefaults.DEFAULT_ID,
    mesocycle: Int = 0,
    microcycle: Int = 0,
    microcyclePosition: Int = 0,
    durationInMillis: Long = 0,
    setResults: List<SetLogEntry> = emptyList()
) = WorkoutLogEntry(
    id = id,
    date = date,
    historicalWorkoutNameId = historicalWorkoutNameId,
    programWorkoutCount = programWorkoutCount,
    programDeloadWeek = programDeloadWeek,
    programName = programName,
    workoutName = workoutName,
    programId = programId,
    workoutId = workoutId,
    mesocycle = mesocycle,
    microcycle = microcycle,
    microcyclePosition = microcyclePosition,
    durationInMillis = durationInMillis,
    setResults = setResults
)

fun createSetLogEntry(
    id: Long = TestDefaults.DEFAULT_ID,
    liftId: Long = TestDefaults.DEFAULT_ID,
    weight: Float = 100f,
    reps: Int = 5,
    rpe: Float = 8f,
    isPersonalRecord: Boolean = false,
    workoutLogEntryId: Long = TestDefaults.DEFAULT_ID,
    workoutLiftDeloadWeek: Int = 4,
    liftName: String = "Squat",
    liftMovementPattern: MovementPattern = MovementPattern.LEG_PUSH,
    progressionScheme: ProgressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
    setType: SetType = SetType.STANDARD,
    liftPosition: Int = 0,
    setPosition: Int = 0,
    myoRepSetPosition: Int? = null,
    repRangeTop: Int = 5,
    repRangeBottom: Int = 3,
    rpeTarget: Float = 8f,
    weightRecommendation: Float = 100f,
    persistedOneRepMax: Int = 120,
    mesoCycle: Int = 0,
    microCycle: Int = 0,
    setMatching: Boolean = false,
    maxSets: Int = 3,
    repFloor: Int = 1,
    dropPercentage: Float = 0.1f,
    isDeload: Boolean = false
) = SetLogEntry(
    id = id,
    liftId = liftId,
    weight = weight,
    reps = reps,
    rpe = rpe,
    isPersonalRecord = isPersonalRecord,
    workoutLogEntryId = workoutLogEntryId,
    workoutLiftDeloadWeek = workoutLiftDeloadWeek,
    liftName = liftName,
    liftMovementPattern = liftMovementPattern,
    progressionScheme = progressionScheme,
    setType = setType,
    liftPosition = liftPosition,
    setPosition = setPosition,
    myoRepSetPosition = myoRepSetPosition,
    repRangeTop = repRangeTop,
    repRangeBottom = repRangeBottom,
    rpeTarget = rpeTarget,
    weightRecommendation = weightRecommendation,
    persistedOneRepMax = persistedOneRepMax,
    mesoCycle = mesoCycle,
    microCycle = microCycle,
    setMatching = setMatching,
    maxSets = maxSets,
    repFloor = repFloor,
    dropPercentage = dropPercentage,
    isDeload = isDeload
)

fun createLiftMetricChart(
    id: Long = TestDefaults.DEFAULT_ID,
    liftId: Long = TestDefaults.DEFAULT_ID,
    chartType: LiftMetricChartType = LiftMetricChartType.ESTIMATED_ONE_REP_MAX
) = LiftMetricChart(
    id = id,
    liftId = liftId,
    chartType = chartType
)

fun createVolumeMetricChart(
    id: Long = TestDefaults.DEFAULT_ID,
    volumeType: VolumeType = VolumeType.QUAD,
    volumeTypeImpact: VolumeTypeImpact = VolumeTypeImpact.PRIMARY
) = VolumeMetricChart(
    id = id,
    volumeType = volumeType,
    volumeTypeImpact = volumeTypeImpact
)
