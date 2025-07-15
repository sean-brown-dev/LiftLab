package com.browntowndev.liftlab.core.persistence.mapping

import com.browntowndev.liftlab.core.persistence.dtos.firestore.*
import com.browntowndev.liftlab.core.persistence.entities.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object FirebaseMappers {

    fun LiftMetricChartFirestoreDto.toEntity(): LiftMetricChart = LiftMetricChart(
        id = this.id,
        liftId = this.liftId,
        chartType = this.chartType,

    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun LiftMetricChart.toFirestoreDto(): LiftMetricChartFirestoreDto = LiftMetricChartFirestoreDto(
        id = this.id,
        liftId = this.liftId,
        chartType = this.chartType
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun PreviousSetResultFirestoreDto.toEntity(): PreviousSetResult = PreviousSetResult(
        id = this.id,
        workoutId = this.workoutId,
        liftId = this.liftId,
        setType = this.setType,
        liftPosition = this.liftPosition,
        setPosition = this.setPosition,
        myoRepSetPosition = this.myoRepSetPosition,
        weightRecommendation = this.weightRecommendation,
        weight = this.weight,
        reps = this.reps,
        rpe = this.rpe,
        oneRepMax = this.oneRepMax,
        mesoCycle = this.mesoCycle,
        microCycle = this.microCycle,
        missedLpGoals = this.missedLpGoals,
        isDeload = this.isDeload
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun PreviousSetResult.toFirestoreDto(): PreviousSetResultFirestoreDto = PreviousSetResultFirestoreDto(
        id = this.id,
        workoutId = this.workoutId,
        liftId = this.liftId,
        setType = this.setType,
        liftPosition = this.liftPosition,
        setPosition = this.setPosition,
        myoRepSetPosition = this.myoRepSetPosition,
        weightRecommendation = this.weightRecommendation,
        weight = this.weight,
        reps = this.reps,
        rpe = this.rpe,
        oneRepMax = this.oneRepMax,
        mesoCycle = this.mesoCycle,
        microCycle = this.microCycle,
        missedLpGoals = this.missedLpGoals,
        isDeload = this.isDeload
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun ProgramFirestoreDto.toEntity(): Program = Program(
        id = this.id,
        name = this.name,
        deloadWeek = this.deloadWeek,
        isActive = this.isActive,
        currentMicrocycle = this.currentMicrocycle,
        currentMicrocyclePosition = this.currentMicrocyclePosition,
        currentMesocycle = this.currentMesocycle
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun Program.toFirestoreDto(): ProgramFirestoreDto = ProgramFirestoreDto(
        id = this.id,
        name = this.name,
        deloadWeek = this.deloadWeek,
        isActive = this.isActive,
        currentMicrocycle = this.currentMicrocycle,
        currentMicrocyclePosition = this.currentMicrocyclePosition,
        currentMesocycle = this.currentMesocycle
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun RestTimerInProgressFirestoreDto.toEntity(): RestTimerInProgress = RestTimerInProgress(
        id = this.id,
        timeStartedInMillis = this.timeStartedInMillis,
        restTime = this.restTime
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun RestTimerInProgress.toFirestoreDto(): RestTimerInProgressFirestoreDto = RestTimerInProgressFirestoreDto(
        id = this.id,
        timeStartedInMillis = this.timeStartedInMillis,
        restTime = this.restTime
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun SetLogEntryFirestoreDto.toEntity(): SetLogEntry = SetLogEntry(
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
        mesoCycle = this.mesoCycle,
        microCycle = this.microCycle,
        setMatching = this.setMatching,
        maxSets = this.maxSets,
        repFloor = this.repFloor,
        dropPercentage = this.dropPercentage,
        isDeload = this.isDeload
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun SetLogEntry.toFirestoreDto(): SetLogEntryFirestoreDto = SetLogEntryFirestoreDto(
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
        mesoCycle = this.mesoCycle,
        microCycle = this.microCycle,
        setMatching = this.setMatching,
        maxSets = this.maxSets,
        repFloor = this.repFloor,
        dropPercentage = this.dropPercentage,
        isDeload = this.isDeload
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun VolumeMetricChartFirestoreDto.toEntity(): VolumeMetricChart = VolumeMetricChart(
        id = this.id,
        volumeType = this.volumeType,
        volumeTypeImpact = this.volumeTypeImpact
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun VolumeMetricChart.toFirestoreDto(): VolumeMetricChartFirestoreDto = VolumeMetricChartFirestoreDto(
        id = this.id,
        volumeType = this.volumeType,
        volumeTypeImpact = this.volumeTypeImpact
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun WorkoutFirestoreDto.toEntity(): Workout = Workout(
        id = this.id,
        programId = this.programId,
        name = this.name,
        position = this.position
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun Workout.toFirestoreDto(): WorkoutFirestoreDto = WorkoutFirestoreDto(
        id = this.id,
        programId = this.programId,
        name = this.name,
        position = this.position
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun WorkoutInProgressFirestoreDto.toEntity(): WorkoutInProgress = WorkoutInProgress(
        id = this.id,
        workoutId = this.workoutId,
        startTime = this.startTime
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun WorkoutInProgress.toFirestoreDto(): WorkoutInProgressFirestoreDto = WorkoutInProgressFirestoreDto(
        id = this.id,
        workoutId = this.workoutId,
        startTime = this.startTime
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun WorkoutLiftFirestoreDto.toEntity(): WorkoutLift = WorkoutLift(
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
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun WorkoutLift.toFirestoreDto(): WorkoutLiftFirestoreDto = WorkoutLiftFirestoreDto(
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
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun WorkoutLogEntryFirestoreDto.toEntity(): WorkoutLogEntry = WorkoutLogEntry(
        id = this.id,
        historicalWorkoutNameId = this.historicalWorkoutNameId,
        programWorkoutCount = this.programWorkoutCount,
        programDeloadWeek = this.programDeloadWeek,
        mesocycle = this.mesocycle,
        microcycle = this.microcycle,
        microcyclePosition = this.microcyclePosition,
        date = this.date,
        durationInMillis = this.durationInMillis
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun WorkoutLogEntry.toFirestoreDto(): WorkoutLogEntryFirestoreDto = WorkoutLogEntryFirestoreDto(
        id = this.id,
        historicalWorkoutNameId = this.historicalWorkoutNameId,
        programWorkoutCount = this.programWorkoutCount,
        programDeloadWeek = this.programDeloadWeek,
        mesocycle = this.mesocycle,
        microcycle = this.microcycle,
        microcyclePosition = this.microcyclePosition,
        date = this.date,
        durationInMillis = this.durationInMillis
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun CustomLiftSetFirestoreDto.toEntity(): CustomLiftSet = CustomLiftSet(
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
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun CustomLiftSet.toFirestoreDto(): CustomLiftSetFirestoreDto = CustomLiftSetFirestoreDto(
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
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun HistoricalWorkoutNameFirestoreDto.toEntity(): HistoricalWorkoutName = HistoricalWorkoutName(
        id = this.id,
        programId = this.programId,
        workoutId = this.workoutId,
        programName = this.programName,
        workoutName = this.workoutName
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun HistoricalWorkoutName.toFirestoreDto(): HistoricalWorkoutNameFirestoreDto = HistoricalWorkoutNameFirestoreDto(
        id = this.id,
        programId = this.programId,
        workoutId = this.workoutId,
        programName = this.programName,
        workoutName = this.workoutName
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun LiftFirestoreDto.toEntity(): Lift = Lift(
        id = this.id,
        name = this.name,
        movementPattern = this.movementPattern,
        volumeTypesBitmask = this.volumeTypesBitmask,
        secondaryVolumeTypesBitmask = this.secondaryVolumeTypesBitmask,
        restTime = this.restTime?.toDuration(DurationUnit.MILLISECONDS),
        restTimerEnabled = this.restTimerEnabled,
        incrementOverride = this.incrementOverride,
        isHidden = this.isHidden,
        isBodyweight = this.isBodyweight,
        note = this.note
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun Lift.toFirestoreDto(): LiftFirestoreDto = LiftFirestoreDto(
        id = this.id,
        name = this.name,
        movementPattern = this.movementPattern,
        volumeTypesBitmask = this.volumeTypesBitmask,
        secondaryVolumeTypesBitmask = this.secondaryVolumeTypesBitmask,
        restTime = this.restTime?.inWholeMilliseconds,
        restTimerEnabled = this.restTimerEnabled,
        incrementOverride = this.incrementOverride,
        isHidden = this.isHidden,
        isBodyweight = this.isBodyweight,
        note = this.note
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
}
