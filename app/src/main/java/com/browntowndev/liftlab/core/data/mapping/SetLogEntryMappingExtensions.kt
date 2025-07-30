package com.browntowndev.liftlab.core.data.mapping

import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.data.local.entities.SetLogEntryEntity

object SetLogEntryMappingExtensions {
    fun SetLogEntry.toEntity(): SetLogEntryEntity {
        return SetLogEntryEntity(
            id = id,
            workoutLogEntryId = workoutLogEntryId,
            liftId = liftId,
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
            weight = weight,
            reps = reps,
            rpe = rpe,
            oneRepMax = oneRepMax,
            mesoCycle = mesoCycle,
            microCycle = microCycle,
            setMatching = setMatching,
            maxSets = maxSets,
            repFloor = repFloor,
            dropPercentage = dropPercentage,
            isDeload = isDeload
        )
    }

    fun SetLogEntryEntity.toDomainModel(): SetLogEntry {
        return SetLogEntry(
            id = id,
            workoutLogEntryId = workoutLogEntryId,
            liftId = liftId,
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
            weight = weight,
            reps = reps,
            rpe = rpe,
            persistedOneRepMax = oneRepMax,
            mesoCycle = mesoCycle,
            microCycle = microCycle,
            setMatching = setMatching,
            maxSets = maxSets,
            repFloor = repFloor,
            dropPercentage = dropPercentage,
            isDeload = isDeload
        )
    }
}