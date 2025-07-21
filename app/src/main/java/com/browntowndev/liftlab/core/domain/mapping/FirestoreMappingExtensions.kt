package com.browntowndev.liftlab.core.domain.mapping

import com.browntowndev.liftlab.core.persistence.firestore.documents.CustomLiftSetFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.HistoricalWorkoutNameFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.LiftFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.LiftMetricChartFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.PreviousSetResultFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.ProgramFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.RestTimerInProgressFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.SetLogEntryFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.VolumeMetricChartFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.WorkoutFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.WorkoutInProgressFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.WorkoutLiftFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.WorkoutLogEntryFirestoreDoc
import com.browntowndev.liftlab.core.persistence.room.entities.CustomLiftSetEntity
import com.browntowndev.liftlab.core.persistence.room.entities.HistoricalWorkoutNameEntity
import com.browntowndev.liftlab.core.persistence.room.entities.LiftEntity
import com.browntowndev.liftlab.core.persistence.room.entities.LiftMetricChartEntity
import com.browntowndev.liftlab.core.persistence.room.entities.PreviousSetResultEntity
import com.browntowndev.liftlab.core.persistence.room.entities.ProgramEntity
import com.browntowndev.liftlab.core.persistence.room.entities.RestTimerInProgressEntity
import com.browntowndev.liftlab.core.persistence.room.entities.SetLogEntryEntity
import com.browntowndev.liftlab.core.persistence.room.entities.VolumeMetricChartEntity
import com.browntowndev.liftlab.core.persistence.room.entities.WorkoutEntity
import com.browntowndev.liftlab.core.persistence.room.entities.WorkoutInProgressEntity
import com.browntowndev.liftlab.core.persistence.room.entities.WorkoutLiftEntity
import com.browntowndev.liftlab.core.persistence.room.entities.WorkoutLogEntryEntity
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object FirestoreMappingExtensions {

    fun LiftMetricChartFirestoreDoc.toEntity(): LiftMetricChartEntity = LiftMetricChartEntity(
        id = this.id,
        liftId = this.liftId,
        chartType = this.chartType,

        ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun LiftMetricChartEntity.toFirestoreDto(): LiftMetricChartFirestoreDoc = LiftMetricChartFirestoreDoc(
        id = this.id,
        liftId = this.liftId,
        chartType = this.chartType
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun PreviousSetResultFirestoreDoc.toEntity(): PreviousSetResultEntity = PreviousSetResultEntity(
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
    fun PreviousSetResultEntity.toFirestoreDto(): PreviousSetResultFirestoreDoc = PreviousSetResultFirestoreDoc(
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
    fun ProgramFirestoreDoc.toEntity(): ProgramEntity = ProgramEntity(
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
    fun ProgramEntity.toFirestoreDto(): ProgramFirestoreDoc = ProgramFirestoreDoc(
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
    fun RestTimerInProgressFirestoreDoc.toEntity(): RestTimerInProgressEntity = RestTimerInProgressEntity(
        id = this.id,
        timeStartedInMillis = this.timeStartedInMillis,
        restTime = this.restTime
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun RestTimerInProgressEntity.toFirestoreDto(): RestTimerInProgressFirestoreDoc = RestTimerInProgressFirestoreDoc(
        id = this.id,
        timeStartedInMillis = this.timeStartedInMillis,
        restTime = this.restTime
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun SetLogEntryFirestoreDoc.toEntity(): SetLogEntryEntity = SetLogEntryEntity(
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
    fun SetLogEntryEntity.toFirestoreDto(): SetLogEntryFirestoreDoc = SetLogEntryFirestoreDoc(
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
    fun VolumeMetricChartFirestoreDoc.toEntity(): VolumeMetricChartEntity = VolumeMetricChartEntity(
        id = this.id,
        volumeType = this.volumeType,
        volumeTypeImpact = this.volumeTypeImpact
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun VolumeMetricChartEntity.toFirestoreDto(): VolumeMetricChartFirestoreDoc = VolumeMetricChartFirestoreDoc(
        id = this.id,
        volumeType = this.volumeType,
        volumeTypeImpact = this.volumeTypeImpact
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun WorkoutFirestoreDoc.toEntity(): WorkoutEntity = WorkoutEntity(
        id = this.id,
        programId = this.programId,
        name = this.name,
        position = this.position
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun WorkoutEntity.toFirestoreDto(): WorkoutFirestoreDoc = WorkoutFirestoreDoc(
        id = this.id,
        programId = this.programId,
        name = this.name,
        position = this.position
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun WorkoutInProgressFirestoreDoc.toEntity(): WorkoutInProgressEntity = WorkoutInProgressEntity(
        id = this.id,
        workoutId = this.workoutId,
        startTime = this.startTime
    ).apply {
        this.firestoreId = this@toEntity.firestoreId
        this.lastUpdated = this@toEntity.lastUpdated
        this.synced = true
    }
    fun WorkoutInProgressEntity.toFirestoreDto(): WorkoutInProgressFirestoreDoc = WorkoutInProgressFirestoreDoc(
        id = this.id,
        workoutId = this.workoutId,
        startTime = this.startTime
    ).apply {
        this.firestoreId = this@toFirestoreDto.firestoreId
        this.lastUpdated = this@toFirestoreDto.lastUpdated
        this.synced = this@toFirestoreDto.synced
    }
    fun WorkoutLiftFirestoreDoc.toEntity(): WorkoutLiftEntity = WorkoutLiftEntity(
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
    fun WorkoutLiftEntity.toFirestoreDto(): WorkoutLiftFirestoreDoc = WorkoutLiftFirestoreDoc(
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
    fun WorkoutLogEntryFirestoreDoc.toEntity(): WorkoutLogEntryEntity = WorkoutLogEntryEntity(
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
    fun WorkoutLogEntryEntity.toFirestoreDto(): WorkoutLogEntryFirestoreDoc = WorkoutLogEntryFirestoreDoc(
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
    fun CustomLiftSetFirestoreDoc.toEntity(): CustomLiftSetEntity = CustomLiftSetEntity(
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
    fun CustomLiftSetEntity.toFirestoreDto(): CustomLiftSetFirestoreDoc = CustomLiftSetFirestoreDoc(
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
    fun HistoricalWorkoutNameFirestoreDoc.toEntity(): HistoricalWorkoutNameEntity = HistoricalWorkoutNameEntity(
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
    fun HistoricalWorkoutNameEntity.toFirestoreDto(): HistoricalWorkoutNameFirestoreDoc = HistoricalWorkoutNameFirestoreDoc(
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
    fun LiftFirestoreDoc.toEntity(): LiftEntity = LiftEntity(
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
    fun LiftEntity.toFirestoreDto(): LiftFirestoreDoc = LiftFirestoreDoc(
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
