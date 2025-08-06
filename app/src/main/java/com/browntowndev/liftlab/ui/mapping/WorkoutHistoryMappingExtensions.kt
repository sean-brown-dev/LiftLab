package com.browntowndev.liftlab.ui.mapping

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.domain.models.metrics.AllWorkoutTopSets
import com.browntowndev.liftlab.core.domain.models.workoutLogging.PersonalRecord
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry
import com.browntowndev.liftlab.ui.models.metrics.AllWorkoutTopSetsUiModel
import com.browntowndev.liftlab.ui.models.metrics.PersonalRecordUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.SetLogEntryUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.WorkoutLogEntryUiModel

object WorkoutHistoryMappingExtensions {
    fun AllWorkoutTopSets.toUiModel(): AllWorkoutTopSetsUiModel {
        val topSetsByWorkoutUiModel = mutableMapOf<Long, AllWorkoutTopSetsUiModel.WorkoutTopSetsUiModel>()
        this.javaClass.getDeclaredField("topSetsByWorkout").let { field ->
            field.isAccessible = true
            val topSetsByWorkout = field.get(this) as Map<Long, AllWorkoutTopSets.WorkoutTopSets>
            topSetsByWorkout.forEach { (workoutLogId, workoutTopSets) ->
                topSetsByWorkoutUiModel[workoutLogId] = workoutTopSets.toUiModel()
            }
        }
        return AllWorkoutTopSetsUiModel(topSetsByWorkoutUiModel)
    }


    fun AllWorkoutTopSets.WorkoutTopSets.toUiModel(): AllWorkoutTopSetsUiModel.WorkoutTopSetsUiModel {
        val recordsByExerciseUiModel = mutableMapOf<Long, AllWorkoutTopSetsUiModel.WorkoutTopSetsUiModel.TopSetUiModel>()
        this.javaClass.getDeclaredField("recordsByExercise").let { field ->
            field.isAccessible = true
            val recordsByExercise = field.get(this) as Map<Long, AllWorkoutTopSets.WorkoutTopSets.TopSet>
            recordsByExercise.forEach { (liftId, topSet) ->
                recordsByExerciseUiModel[liftId] = topSet.toUiModel()
            }
        }
        return AllWorkoutTopSetsUiModel.WorkoutTopSetsUiModel(recordsByExerciseUiModel)
    }

    fun AllWorkoutTopSets.WorkoutTopSets.TopSet.toUiModel(): AllWorkoutTopSetsUiModel.WorkoutTopSetsUiModel.TopSetUiModel {
        return AllWorkoutTopSetsUiModel.WorkoutTopSetsUiModel.TopSetUiModel(
            setCount = this.setCount,
            setLog = this.setLog
        )
    }

    fun PersonalRecord.toUiModel(): PersonalRecordUiModel {
        return PersonalRecordUiModel(
            liftId = this.liftId,
            personalRecord = this.personalRecord,
        )
    }

    fun PersonalRecordUiModel.toDomainModel(): PersonalRecord {
        return PersonalRecord(
            liftId = this.liftId,
            personalRecord = this.personalRecord,
        )
    }

    fun WorkoutLogEntry.toUiModel(): WorkoutLogEntryUiModel {
        return WorkoutLogEntryUiModel(
            id = this.id,
            historicalWorkoutNameId = this.historicalWorkoutNameId,
            programWorkoutCount = this.programWorkoutCount,
            programDeloadWeek = this.programDeloadWeek,
            programName = this.programName,
            workoutName = this.workoutName,
            programId = this.programId,
            workoutId = this.workoutId,
            mesocycle = this.mesocycle,
            microcycle = this.microcycle,
            microcyclePosition = this.microcyclePosition,
            date = this.date,
            durationInMillis = this.durationInMillis,
            setLogEntries = this.setLogEntries.fastMap { it.toUiModel() },
        )
    }

    fun WorkoutLogEntryUiModel.toDomainModel(): WorkoutLogEntry {
        return WorkoutLogEntry(
            id = this.id,
            historicalWorkoutNameId = this.historicalWorkoutNameId,
            programWorkoutCount = this.programWorkoutCount,
            programDeloadWeek = this.programDeloadWeek,
            programName = this.programName,
            workoutName = this.workoutName,
            programId = this.programId,
            workoutId = this.workoutId,
            mesocycle = this.mesocycle,
            microcycle = this.microcycle,
            microcyclePosition = this.microcyclePosition,
            date = this.date,
            durationInMillis = this.durationInMillis,
            setLogEntries = this.setLogEntries.fastMap { it.toDomainModel() },
        )
    }

    fun SetLogEntry.toUiModel(): SetLogEntryUiModel =
        SetLogEntryUiModel(
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
            isPersonalRecord = this.isPersonalRecord,
            setMatching = this.setMatching,
            maxSets = this.maxSets,
            repFloor = this.repFloor,
            dropPercentage = this.dropPercentage,
            isDeload = this.isDeload
        )

    fun SetLogEntryUiModel.toDomainModel(): SetLogEntry =
        SetLogEntry(
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
            isPersonalRecord = this.isPersonalRecord,
            setMatching = this.setMatching,
            maxSets = this.maxSets,
            repFloor = this.repFloor,
            dropPercentage = this.dropPercentage,
            isDeload = this.isDeload
        )
}
