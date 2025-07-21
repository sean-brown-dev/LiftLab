package com.browntowndev.liftlab.core.domain.mapping

import com.browntowndev.liftlab.core.persistence.firestore.entities.CustomLiftSetFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.HistoricalWorkoutNameFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.LiftFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.LiftMetricChartFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.PreviousSetResultFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.ProgramFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.RestTimerInProgressFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.SetLogEntryFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.VolumeMetricChartFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.WorkoutFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.WorkoutInProgressFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.WorkoutLiftFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.WorkoutLogEntryFirestoreEntity
import com.browntowndev.liftlab.core.persistence.entities.room.CustomLiftSetEntity
import com.browntowndev.liftlab.core.persistence.entities.room.HistoricalWorkoutNameEntity
import com.browntowndev.liftlab.core.persistence.entities.room.LiftEntity
import com.browntowndev.liftlab.core.persistence.entities.room.LiftMetricChartEntity
import com.browntowndev.liftlab.core.persistence.entities.room.PreviousSetResultEntity
import com.browntowndev.liftlab.core.persistence.entities.room.ProgramEntity
import com.browntowndev.liftlab.core.persistence.entities.room.RestTimerInProgressEntity
import com.browntowndev.liftlab.core.persistence.entities.room.SetLogEntryEntity
import com.browntowndev.liftlab.core.persistence.entities.room.VolumeMetricChartEntity
import com.browntowndev.liftlab.core.persistence.entities.room.WorkoutEntity
import com.browntowndev.liftlab.core.persistence.entities.room.WorkoutInProgressEntity
import com.browntowndev.liftlab.core.persistence.entities.room.WorkoutLiftEntity
import com.browntowndev.liftlab.core.persistence.entities.room.WorkoutLogEntryEntity
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object FirestoreMappingExtensions {

    fun LiftMetricChartFirestoreEntity.toEntity(): LiftMetricChartEntity = LiftMetricChartEntity(
        id = this.id,
        liftId = this.liftId,
        chartType = this.chartType,

        ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun LiftMetricChartEntity.toFirestoreDto(): LiftMetricChartFirestoreEntity = LiftMetricChartFirestoreEntity(
        id = this.id,
        liftId = this.liftId,
        chartType = this.chartType
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun PreviousSetResultFirestoreEntity.toEntity(): PreviousSetResultEntity = PreviousSetResultEntity(
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
    fun PreviousSetResultEntity.toFirestoreDto(): PreviousSetResultFirestoreEntity = PreviousSetResultFirestoreEntity(
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
    fun ProgramFirestoreEntity.toEntity(): ProgramEntity = ProgramEntity(
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
    fun ProgramEntity.toFirestoreDto(): ProgramFirestoreEntity = ProgramFirestoreEntity(
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
    fun RestTimerInProgressFirestoreEntity.toEntity(): RestTimerInProgressEntity = RestTimerInProgressEntity(
        id = this.id,
        timeStartedInMillis = this.timeStartedInMillis,
        restTime = this.restTime
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun RestTimerInProgressEntity.toFirestoreDto(): RestTimerInProgressFirestoreEntity = RestTimerInProgressFirestoreEntity(
        id = this.id,
        timeStartedInMillis = this.timeStartedInMillis,
        restTime = this.restTime
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun SetLogEntryFirestoreEntity.toEntity(): SetLogEntryEntity = SetLogEntryEntity(
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
    fun SetLogEntryEntity.toFirestoreDto(): SetLogEntryFirestoreEntity = SetLogEntryFirestoreEntity(
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
    fun VolumeMetricChartFirestoreEntity.toEntity(): VolumeMetricChartEntity = VolumeMetricChartEntity(
        id = this.id,
        volumeType = this.volumeType,
        volumeTypeImpact = this.volumeTypeImpact
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun VolumeMetricChartEntity.toFirestoreDto(): VolumeMetricChartFirestoreEntity = VolumeMetricChartFirestoreEntity(
        id = this.id,
        volumeType = this.volumeType,
        volumeTypeImpact = this.volumeTypeImpact
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun WorkoutFirestoreEntity.toEntity(): WorkoutEntity = WorkoutEntity(
        id = this.id,
        programId = this.programId,
        name = this.name,
        position = this.position
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun WorkoutEntity.toFirestoreDto(): WorkoutFirestoreEntity = WorkoutFirestoreEntity(
        id = this.id,
        programId = this.programId,
        name = this.name,
        position = this.position
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun WorkoutInProgressFirestoreEntity.toEntity(): WorkoutInProgressEntity = WorkoutInProgressEntity(
        id = this.id,
        workoutId = this.workoutId,
        startTime = this.startTime
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun WorkoutInProgressEntity.toFirestoreDto(): WorkoutInProgressFirestoreEntity = WorkoutInProgressFirestoreEntity(
        id = this.id,
        workoutId = this.workoutId,
        startTime = this.startTime
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun WorkoutLiftFirestoreEntity.toEntity(): WorkoutLiftEntity = WorkoutLiftEntity(
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
    fun WorkoutLiftEntity.toFirestoreDto(): WorkoutLiftFirestoreEntity = WorkoutLiftFirestoreEntity(
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
    fun WorkoutLogEntryFirestoreEntity.toEntity(): WorkoutLogEntryEntity = WorkoutLogEntryEntity(
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
    fun WorkoutLogEntryEntity.toFirestoreDto(): WorkoutLogEntryFirestoreEntity = WorkoutLogEntryFirestoreEntity(
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
    fun CustomLiftSetFirestoreEntity.toEntity(): CustomLiftSetEntity = CustomLiftSetEntity(
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
    fun CustomLiftSetEntity.toFirestoreDto(): CustomLiftSetFirestoreEntity = CustomLiftSetFirestoreEntity(
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
    fun HistoricalWorkoutNameFirestoreEntity.toEntity(): HistoricalWorkoutNameEntity = HistoricalWorkoutNameEntity(
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
    fun HistoricalWorkoutNameEntity.toFirestoreDto(): HistoricalWorkoutNameFirestoreEntity = HistoricalWorkoutNameFirestoreEntity(
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
    fun LiftFirestoreEntity.toEntity(): LiftEntity = LiftEntity(
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
    fun LiftEntity.toFirestoreDto(): LiftFirestoreEntity = LiftFirestoreEntity(
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
