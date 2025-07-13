package com.browntowndev.liftlab.core.persistence.mapping

import com.browntowndev.liftlab.core.persistence.dtos.firebase.*
import com.browntowndev.liftlab.core.persistence.entities.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object FirebaseMappers {

    fun LiftMetricChartFirebaseDto.toEntity(): LiftMetricChart = LiftMetricChart(
        id = this.id,
        liftId = this.liftId,
        chartType = this.chartType,

    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
    }
    fun LiftMetricChart.toFirebaseDto(): LiftMetricChartFirebaseDto = LiftMetricChartFirebaseDto(
        id = this.id,
        liftId = this.liftId,
        chartType = this.chartType
    ).apply {
        this.firestoreId = this@toFirebaseDto.firestoreId
        this.lastUpdated = this@toFirebaseDto.lastUpdated
    }
    fun PreviousSetResultFirebaseDto.toEntity(): PreviousSetResult = PreviousSetResult(
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
    }
    fun PreviousSetResult.toFirebaseDto(): PreviousSetResultFirebaseDto = PreviousSetResultFirebaseDto(
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
        this.firestoreId = this@toFirebaseDto.firestoreId
        this.lastUpdated = this@toFirebaseDto.lastUpdated
    }
    fun ProgramFirebaseDto.toEntity(): Program = Program(
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
    }
    fun Program.toFirebaseDto(): ProgramFirebaseDto = ProgramFirebaseDto(
        id = this.id,
        name = this.name,
        deloadWeek = this.deloadWeek,
        isActive = this.isActive,
        currentMicrocycle = this.currentMicrocycle,
        currentMicrocyclePosition = this.currentMicrocyclePosition,
        currentMesocycle = this.currentMesocycle
    ).apply {
        this.firestoreId = this@toFirebaseDto.firestoreId
        this.lastUpdated = this@toFirebaseDto.lastUpdated
    }
    fun RestTimerInProgressFirebaseDto.toEntity(): RestTimerInProgress = RestTimerInProgress(
        id = this.id,
        timeStartedInMillis = this.timeStartedInMillis,
        restTime = this.restTime
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
    }
    fun RestTimerInProgress.toFirebaseDto(): RestTimerInProgressFirebaseDto = RestTimerInProgressFirebaseDto(
        id = this.id,
        timeStartedInMillis = this.timeStartedInMillis,
        restTime = this.restTime
    ).apply {
        this.firestoreId = this@toFirebaseDto.firestoreId
        this.lastUpdated = this@toFirebaseDto.lastUpdated
    }
    fun SetLogEntryFirebaseDto.toEntity(): SetLogEntry = SetLogEntry(
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
    }
    fun SetLogEntry.toFirebaseDto(): SetLogEntryFirebaseDto = SetLogEntryFirebaseDto(
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
        this.firestoreId = this@toFirebaseDto.firestoreId
        this.lastUpdated = this@toFirebaseDto.lastUpdated
    }
    fun VolumeMetricChartFirebaseDto.toEntity(): VolumeMetricChart = VolumeMetricChart(
        id = this.id,
        volumeType = this.volumeType,
        volumeTypeImpact = this.volumeTypeImpact
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
    }
    fun VolumeMetricChart.toFirebaseDto(): VolumeMetricChartFirebaseDto = VolumeMetricChartFirebaseDto(
        id = this.id,
        volumeType = this.volumeType,
        volumeTypeImpact = this.volumeTypeImpact
    ).apply {
        this.firestoreId = this@toFirebaseDto.firestoreId
        this.lastUpdated = this@toFirebaseDto.lastUpdated
    }
    fun WorkoutFirebaseDto.toEntity(): Workout = Workout(
        id = this.id,
        programId = this.programId,
        name = this.name,
        position = this.position
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
    }
    fun Workout.toFirebaseDto(): WorkoutFirebaseDto = WorkoutFirebaseDto(
        id = this.id,
        programId = this.programId,
        name = this.name,
        position = this.position
    ).apply {
        this.firestoreId = this@toFirebaseDto.firestoreId
        this.lastUpdated = this@toFirebaseDto.lastUpdated
    }
    fun WorkoutInProgressFirebaseDto.toEntity(): WorkoutInProgress = WorkoutInProgress(
        id = this.id,
        workoutId = this.workoutId,
        startTime = this.startTime
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
    }
    fun WorkoutInProgress.toFirebaseDto(): WorkoutInProgressFirebaseDto = WorkoutInProgressFirebaseDto(
        id = this.id,
        workoutId = this.workoutId,
        startTime = this.startTime
    ).apply {
        this.firestoreId = this@toFirebaseDto.firestoreId
        this.lastUpdated = this@toFirebaseDto.lastUpdated
    }
    fun WorkoutLiftFirebaseDto.toEntity(): WorkoutLift = WorkoutLift(
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
    }
    fun WorkoutLift.toFirebaseDto(): WorkoutLiftFirebaseDto = WorkoutLiftFirebaseDto(
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
        this.firestoreId = this@toFirebaseDto.firestoreId
        this.lastUpdated = this@toFirebaseDto.lastUpdated
    }
    fun WorkoutLogEntryFirebaseDto.toEntity(): WorkoutLogEntry = WorkoutLogEntry(
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
    }
    fun WorkoutLogEntry.toFirebaseDto(): WorkoutLogEntryFirebaseDto = WorkoutLogEntryFirebaseDto(
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
        this.firestoreId = this@toFirebaseDto.firestoreId
        this.lastUpdated = this@toFirebaseDto.lastUpdated
    }
    fun CustomLiftSetFirebaseDto.toEntity(): CustomLiftSet = CustomLiftSet(
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
    }
    fun CustomLiftSet.toFirebaseDto(): CustomLiftSetFirebaseDto = CustomLiftSetFirebaseDto(
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
        this.firestoreId = this@toFirebaseDto.firestoreId
        this.lastUpdated = this@toFirebaseDto.lastUpdated
    }
    fun HistoricalWorkoutNameFirebaseDto.toEntity(): HistoricalWorkoutName = HistoricalWorkoutName(
        id = this.id,
        programId = this.programId,
        workoutId = this.workoutId,
        programName = this.programName,
        workoutName = this.workoutName
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
    }
    fun HistoricalWorkoutName.toFirebaseDto(): HistoricalWorkoutNameFirebaseDto = HistoricalWorkoutNameFirebaseDto(
        id = this.id,
        programId = this.programId,
        workoutId = this.workoutId,
        programName = this.programName,
        workoutName = this.workoutName
    ).apply {
        this.firestoreId = this@toFirebaseDto.firestoreId
        this.lastUpdated = this@toFirebaseDto.lastUpdated
    }
    fun LiftFirebaseDto.toEntity(): Lift = Lift(
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
    }
    fun Lift.toFirebaseDto(): LiftFirebaseDto = LiftFirebaseDto(
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
        this.firestoreId = this@toFirebaseDto.firestoreId
        this.lastUpdated = this@toFirebaseDto.lastUpdated
    }}
