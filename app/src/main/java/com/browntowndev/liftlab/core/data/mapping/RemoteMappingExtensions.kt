package com.browntowndev.liftlab.core.data.mapping

import com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.data.local.entities.HistoricalWorkoutNameEntity
import com.browntowndev.liftlab.core.data.local.entities.LiftEntity
import com.browntowndev.liftlab.core.data.local.entities.LiftMetricChartEntity
import com.browntowndev.liftlab.core.data.local.entities.LiveWorkoutCompletedSetEntity
import com.browntowndev.liftlab.core.data.local.entities.ProgramEntity
import com.browntowndev.liftlab.core.data.local.entities.RestTimerInProgressEntity
import com.browntowndev.liftlab.core.data.local.entities.SetLogEntryEntity
import com.browntowndev.liftlab.core.data.local.entities.VolumeMetricChartEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutInProgressEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLiftEntity
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLogEntryEntity
import com.browntowndev.liftlab.core.data.remote.dto.CustomLiftSetRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.HistoricalWorkoutNameRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.LiftMetricChartRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.LiftRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.LiveWorkoutCompletedSetDto
import com.browntowndev.liftlab.core.data.remote.dto.ProgramRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.RestTimerInProgressRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.SetLogEntryRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.VolumeMetricChartRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.WorkoutInProgressRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.WorkoutLiftRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.WorkoutLogEntryRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.WorkoutRemoteDto
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun LiftMetricChartRemoteDto.toEntity(): LiftMetricChartEntity = LiftMetricChartEntity(
    id = this.id,
    liftId = this.liftId,
    chartType = this.chartType,
).apply {
    this.remoteId = this@toEntity.remoteId
    this.remoteLastUpdated = this@toEntity.lastUpdated
    this.deleted = this@toEntity.deleted
    this.synced = true
}
fun LiftMetricChartEntity.toRemoteDto(): LiftMetricChartRemoteDto = LiftMetricChartRemoteDto(
    id = this.id,
    liftId = this.liftId,
    chartType = this.chartType
).apply {
    this.remoteId = this@toRemoteDto.remoteId
    this.lastUpdated = this@toRemoteDto.remoteLastUpdated
    this.deleted = this@toRemoteDto.deleted
    this.synced = this@toRemoteDto.synced
}
fun LiveWorkoutCompletedSetDto.toEntity(): LiveWorkoutCompletedSetEntity = LiveWorkoutCompletedSetEntity(
    id = this.id,
    workoutId = this.workoutId,
    liftId = this.liftId,
    setType = this.setType,
    liftPosition = this.liftPosition,
    setPosition = this.setPosition,
    myoRepSetPosition = this.myoRepSetPosition,
    weight = this.weight,
    reps = this.reps,
    rpe = this.rpe,
    oneRepMax = this.oneRepMax,
    missedLpGoals = this.missedLpGoals,
    isDeload = this.isDeload
).apply {
    this.remoteId = this@toEntity.remoteId
    this.remoteLastUpdated = this@toEntity.lastUpdated
    this.deleted = this@toEntity.deleted
    this.synced = true
}
fun LiveWorkoutCompletedSetEntity.toRemoteDto(): LiveWorkoutCompletedSetDto = LiveWorkoutCompletedSetDto(
    id = this.id,
    workoutId = this.workoutId,
    liftId = this.liftId,
    setType = this.setType,
    liftPosition = this.liftPosition,
    setPosition = this.setPosition,
    myoRepSetPosition = this.myoRepSetPosition,
    weight = this.weight,
    reps = this.reps,
    rpe = this.rpe,
    oneRepMax = this.oneRepMax,
    missedLpGoals = this.missedLpGoals,
    isDeload = this.isDeload
).apply {
    this.remoteId = this@toRemoteDto.remoteId
    this.lastUpdated = this@toRemoteDto.remoteLastUpdated
    this.deleted = this@toRemoteDto.deleted
    this.synced = this@toRemoteDto.synced
}
fun ProgramRemoteDto.toEntity(): ProgramEntity = ProgramEntity(
    id = this.id,
    name = this.name,
    deloadWeek = this.deloadWeek,
    isActive = this.isActive,
    currentMicrocycle = this.currentMicrocycle,
    currentMicrocyclePosition = this.currentMicrocyclePosition,
    currentMesocycle = this.currentMesocycle
).apply {
    this.remoteId = this@toEntity.remoteId
    this.remoteLastUpdated = this@toEntity.lastUpdated
    this.deleted = this@toEntity.deleted
    this.synced = true
}
fun ProgramEntity.toRemoteDto(): ProgramRemoteDto = ProgramRemoteDto(
    id = this.id,
    name = this.name,
    deloadWeek = this.deloadWeek,
    isActive = this.isActive,
    currentMicrocycle = this.currentMicrocycle,
    currentMicrocyclePosition = this.currentMicrocyclePosition,
    currentMesocycle = this.currentMesocycle
).apply {
    this.remoteId = this@toRemoteDto.remoteId
    this.lastUpdated = this@toRemoteDto.remoteLastUpdated
    this.deleted = this@toRemoteDto.deleted
    this.synced = this@toRemoteDto.synced
}
fun RestTimerInProgressRemoteDto.toEntity(): RestTimerInProgressEntity =
    RestTimerInProgressEntity(
        id = this.id,
        timeStartedInMillis = this.timeStartedInMillis,
        restTime = this.restTime
    )
fun RestTimerInProgressEntity.toRemoteDto(): RestTimerInProgressRemoteDto =
    RestTimerInProgressRemoteDto(
        id = this.id,
        timeStartedInMillis = this.timeStartedInMillis,
        restTime = this.restTime
    )
fun SetLogEntryRemoteDto.toEntity(): SetLogEntryEntity = SetLogEntryEntity(
    id = this.id,
    workoutLogEntryId = this.workoutLogEntryId,
    liftId = this.liftId,
    workoutLiftDeloadWeek = this.workoutLiftDeloadWeek,
    liftName = this.liftName,
    liftMovementPattern = this.liftMovementPattern,
    progressionScheme = this.progressionScheme,
    setType = this.setType,
    liftPosition = this.liftPosition,
    setPosition = this.setPosition,
    myoRepSetPosition = this.myoRepSetPosition,
    repRangeTop = this.repRangeTop,
    repRangeBottom = this.repRangeBottom,
    rpeTarget = this.rpeTarget,
    weightRecommendation = this.weightRecommendation,
    weight = this.weight,
    reps = this.reps,
    rpe = this.rpe,
    oneRepMax = this.oneRepMax,
    setMatching = this.setMatching,
    maxSets = this.maxSets,
    repFloor = this.repFloor,
    dropPercentage = this.dropPercentage,
    isDeload = this.isDeload
).apply {
    this.remoteId = this@toEntity.remoteId
    this.remoteLastUpdated = this@toEntity.lastUpdated
    this.deleted = this@toEntity.deleted
    this.synced = true
}
fun SetLogEntryEntity.toRemoteDto(): SetLogEntryRemoteDto = SetLogEntryRemoteDto(
    id = this.id,
    workoutLogEntryId = this.workoutLogEntryId,
    liftId = this.liftId,
    workoutLiftDeloadWeek = this.workoutLiftDeloadWeek,
    liftName = this.liftName,
    liftMovementPattern = this.liftMovementPattern,
    progressionScheme = this.progressionScheme,
    setType = this.setType,
    liftPosition = this.liftPosition,
    setPosition = this.setPosition,
    myoRepSetPosition = this.myoRepSetPosition,
    repRangeTop = this.repRangeTop,
    repRangeBottom = this.repRangeBottom,
    rpeTarget = this.rpeTarget,
    weightRecommendation = this.weightRecommendation,
    weight = this.weight,
    reps = this.reps,
    rpe = this.rpe,
    oneRepMax = this.oneRepMax,
    setMatching = this.setMatching,
    maxSets = this.maxSets,
    repFloor = this.repFloor,
    dropPercentage = this.dropPercentage,
    isDeload = this.isDeload
).apply {
    this.remoteId = this@toRemoteDto.remoteId
    this.lastUpdated = this@toRemoteDto.remoteLastUpdated
    this.deleted = this@toRemoteDto.deleted
    this.synced = this@toRemoteDto.synced
}
fun VolumeMetricChartRemoteDto.toEntity(): VolumeMetricChartEntity = VolumeMetricChartEntity(
    id = this.id,
    volumeType = this.volumeType,
    volumeTypeImpact = this.volumeTypeImpact
).apply {
    this.remoteId = this@toEntity.remoteId
    this.remoteLastUpdated = this@toEntity.lastUpdated
    this.deleted = this@toEntity.deleted
    this.synced = true
}
fun VolumeMetricChartEntity.toRemoteDto(): VolumeMetricChartRemoteDto = VolumeMetricChartRemoteDto(
    id = this.id,
    volumeType = this.volumeType,
    volumeTypeImpact = this.volumeTypeImpact
).apply {
    this.remoteId = this@toRemoteDto.remoteId
    this.lastUpdated = this@toRemoteDto.remoteLastUpdated
    this.deleted = this@toRemoteDto.deleted
    this.synced = this@toRemoteDto.synced
}
fun WorkoutRemoteDto.toEntity(): WorkoutEntity = WorkoutEntity(
    id = this.id,
    programId = this.programId,
    name = this.name,
    position = this.position
).apply {
    this.remoteId = this@toEntity.remoteId
    this.remoteLastUpdated = this@toEntity.lastUpdated
    this.deleted = this@toEntity.deleted
    this.synced = true
}
fun WorkoutEntity.toRemoteDto(): WorkoutRemoteDto = WorkoutRemoteDto(
    id = this.id,
    programId = this.programId,
    name = this.name,
    position = this.position
).apply {
    this.remoteId = this@toRemoteDto.remoteId
    this.lastUpdated = this@toRemoteDto.remoteLastUpdated
    this.deleted = this@toRemoteDto.deleted
    this.synced = this@toRemoteDto.synced
}
fun WorkoutInProgressRemoteDto.toEntity(): WorkoutInProgressEntity = WorkoutInProgressEntity(
    id = this.id,
    workoutId = this.workoutId,
    startTime = this.startTime
).apply {
    this.remoteId = this@toEntity.remoteId
    this.remoteLastUpdated = this@toEntity.lastUpdated
    this.deleted = this@toEntity.deleted
    this.synced = true
}
fun WorkoutInProgressEntity.toRemoteDto(): WorkoutInProgressRemoteDto = WorkoutInProgressRemoteDto(
    id = this.id,
    workoutId = this.workoutId,
    startTime = this.startTime
).apply {
    this.remoteId = this@toRemoteDto.remoteId
    this.lastUpdated = this@toRemoteDto.remoteLastUpdated
    this.deleted = this@toRemoteDto.deleted
    this.synced = this@toRemoteDto.synced
}
fun WorkoutLiftRemoteDto.toEntity(): WorkoutLiftEntity = WorkoutLiftEntity(
    id = this.id,
    workoutId = this.workoutId,
    liftId = this.liftId,
    progressionScheme = this.progressionScheme,
    position = this.position,
    setCount = this.setCount,
    deloadWeek = this.deloadWeek,
    rpeTarget = this.rpeTarget,
    repRangeBottom = this.repRangeBottom,
    repRangeTop = this.repRangeTop,
    stepSize = this.stepSize
).apply {
    this.remoteId = this@toEntity.remoteId
    this.remoteLastUpdated = this@toEntity.lastUpdated
    this.deleted = this@toEntity.deleted
    this.synced = true
}
fun WorkoutLiftEntity.toRemoteDto(): WorkoutLiftRemoteDto = WorkoutLiftRemoteDto(
    id = this.id,
    workoutId = this.workoutId,
    liftId = this.liftId,
    progressionScheme = this.progressionScheme,
    position = this.position,
    setCount = this.setCount,
    deloadWeek = this.deloadWeek,
    rpeTarget = this.rpeTarget,
    repRangeBottom = this.repRangeBottom,
    repRangeTop = this.repRangeTop,
    stepSize = this.stepSize
).apply {
    this.remoteId = this@toRemoteDto.remoteId
    this.lastUpdated = this@toRemoteDto.remoteLastUpdated
    this.deleted = this@toRemoteDto.deleted
    this.synced = this@toRemoteDto.synced
}
fun WorkoutLogEntryRemoteDto.toEntity(): WorkoutLogEntryEntity = WorkoutLogEntryEntity(
    id = this.id,
    historicalWorkoutNameId = this.historicalWorkoutNameId,
    programWorkoutCount = this.programWorkoutCount,
    programDeloadWeek = this.programDeloadWeek,
    mesoCycle = this.mesocycle,
    microCycle = this.microcycle,
    microcyclePosition = this.microcyclePosition,
    date = this.date,
    durationInMillis = this.durationInMillis
).apply {
    this.remoteId = this@toEntity.remoteId
    this.remoteLastUpdated = this@toEntity.lastUpdated
    this.deleted = this@toEntity.deleted
    this.synced = true
}
fun WorkoutLogEntryEntity.toRemoteDto(): WorkoutLogEntryRemoteDto = WorkoutLogEntryRemoteDto(
    id = this.id,
    historicalWorkoutNameId = this.historicalWorkoutNameId,
    programWorkoutCount = this.programWorkoutCount,
    programDeloadWeek = this.programDeloadWeek,
    mesocycle = this.mesoCycle,
    microcycle = this.microCycle,
    microcyclePosition = this.microcyclePosition,
    date = this.date,
    durationInMillis = this.durationInMillis
).apply {
    this.remoteId = this@toRemoteDto.remoteId
    this.lastUpdated = this@toRemoteDto.remoteLastUpdated
    this.deleted = this@toRemoteDto.deleted
    this.synced = this@toRemoteDto.synced
}
fun CustomLiftSetRemoteDto.toEntity(): CustomLiftSetEntity = CustomLiftSetEntity(
    id = this.id,
    workoutLiftId = this.workoutLiftId,
    type = this.type,
    position = this.position,
    rpeTarget = this.rpeTarget,
    repRangeBottom = this.repRangeBottom,
    repRangeTop = this.repRangeTop,
    setGoal = this.setGoal,
    repFloor = this.repFloor,
    dropPercentage = this.dropPercentage,
    maxSets = this.maxSets,
    setMatching = this.setMatching
).apply {
    this.remoteId = this@toEntity.remoteId
    this.remoteLastUpdated = this@toEntity.lastUpdated
    this.deleted = this@toEntity.deleted
    this.synced = true
}
fun CustomLiftSetEntity.toRemoteDto(): CustomLiftSetRemoteDto = CustomLiftSetRemoteDto(
    id = this.id,
    workoutLiftId = this.workoutLiftId,
    type = this.type,
    position = this.position,
    rpeTarget = this.rpeTarget,
    repRangeBottom = this.repRangeBottom,
    repRangeTop = this.repRangeTop,
    setGoal = this.setGoal,
    repFloor = this.repFloor,
    dropPercentage = this.dropPercentage,
    maxSets = this.maxSets,
    setMatching = this.setMatching
).apply {
    this.remoteId = this@toRemoteDto.remoteId
    this.lastUpdated = this@toRemoteDto.remoteLastUpdated
    this.deleted = this@toRemoteDto.deleted
    this.synced = this@toRemoteDto.synced
}
fun HistoricalWorkoutNameRemoteDto.toEntity(): HistoricalWorkoutNameEntity = HistoricalWorkoutNameEntity(
    id = this.id,
    programId = this.programId,
    workoutId = this.workoutId,
    programName = this.programName,
    workoutName = this.workoutName
).apply {
    this.remoteId = this@toEntity.remoteId
    this.remoteLastUpdated = this@toEntity.lastUpdated
    this.deleted = this@toEntity.deleted
    this.synced = true
}
fun HistoricalWorkoutNameEntity.toRemoteDto(): HistoricalWorkoutNameRemoteDto = HistoricalWorkoutNameRemoteDto(
    id = this.id,
    programId = this.programId,
    workoutId = this.workoutId,
    programName = this.programName,
    workoutName = this.workoutName
).apply {
    this.remoteId = this@toRemoteDto.remoteId
    this.lastUpdated = this@toRemoteDto.remoteLastUpdated
    this.deleted = this@toRemoteDto.deleted
    this.synced = this@toRemoteDto.synced
}
fun LiftRemoteDto.toEntity(): LiftEntity = LiftEntity(
    id = this.id,
    name = this.name,
    movementPattern = this.movementPattern,
    volumeTypesBitmask = this.volumeTypesBitmask,
    secondaryVolumeTypesBitmask = this.secondaryVolumeTypesBitmask,
    restTime = this.restTime?.toDuration(DurationUnit.MILLISECONDS),
    restTimerEnabled = this.restTimerEnabled,
    incrementOverride = this.incrementOverride,
    isBodyweight = this.isBodyweight,
    note = this.note
).apply {
    this.remoteId = this@toEntity.remoteId
    this.remoteLastUpdated = this@toEntity.lastUpdated
    this.deleted = this@toEntity.deleted
    this.synced = true
}
fun LiftEntity.toRemoteDto(): LiftRemoteDto = LiftRemoteDto(
    id = this.id,
    name = this.name,
    movementPattern = this.movementPattern,
    volumeTypesBitmask = this.volumeTypesBitmask,
    secondaryVolumeTypesBitmask = this.secondaryVolumeTypesBitmask,
    restTime = this.restTime?.inWholeMilliseconds,
    restTimerEnabled = this.restTimerEnabled,
    incrementOverride = this.incrementOverride,
    isBodyweight = this.isBodyweight,
    note = this.note
).apply {
    this.remoteId = this@toRemoteDto.remoteId
    this.lastUpdated = this@toRemoteDto.remoteLastUpdated
    this.deleted = this@toRemoteDto.deleted
    this.synced = this@toRemoteDto.synced
}
