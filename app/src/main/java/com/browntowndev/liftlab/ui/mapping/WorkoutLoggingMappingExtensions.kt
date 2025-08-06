package com.browntowndev.liftlab.ui.mapping

import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingDropSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.StandardSetResult
import com.browntowndev.liftlab.ui.models.workoutLogging.LinearProgressionSetResultUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingDropSetUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingMyoRepSetUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingSetUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingStandardSetUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingWorkoutLiftUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingWorkoutUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.MyoRepSetResultUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.SetResultUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.StandardSetResultUiModel

object WorkoutLoggingMappingExtensions {
    fun LoggingWorkout.toUiModel(): LoggingWorkoutUiModel {
        return LoggingWorkoutUiModel(
            id = this.id,
            name = this.name,
            lifts = this.lifts.map { it.toUiModel() }
        )
    }

    fun LoggingWorkoutLift.toUiModel(): LoggingWorkoutLiftUiModel {
        return LoggingWorkoutLiftUiModel(
            id = id,
            liftId = liftId,
            liftName = liftName,
            liftMovementPattern = liftMovementPattern,
            liftVolumeTypes = liftVolumeTypes,
            liftSecondaryVolumeTypes = liftSecondaryVolumeTypes,
            note = note,
            position = position,
            progressionScheme = progressionScheme,
            deloadWeek = deloadWeek,
            incrementOverride = incrementOverride,
            restTime = restTime,
            restTimerEnabled = restTimerEnabled,
            sets = sets.map { it.toUiModel() }
        )
    }

    fun GenericLoggingSet.toUiModel(): LoggingSetUiModel {
        return when (this) {
            is LoggingStandardSet -> toUiModel()
            is LoggingDropSet -> toUiModel()
            is LoggingMyoRepSet -> toUiModel()
            else -> throw IllegalArgumentException("Unknown type of GenericLoggingSet")
        }
    }

    fun LoggingStandardSet.toUiModel(): LoggingStandardSetUiModel {
        return LoggingStandardSetUiModel(
            position = position,
            rpeTarget = rpeTarget,
            repRangeBottom = repRangeBottom,
            repRangeTop = repRangeTop,
            weightRecommendation = weightRecommendation,
            hadInitialWeightRecommendation = hadInitialWeightRecommendation,
            previousSetResultLabel = previousSetResultLabel,
            repRangePlaceholder = repRangePlaceholder,
            setNumberLabel = setNumberLabel,
            complete = complete,
            completedWeight = completedWeight,
            completedReps = completedReps,
            completedRpe = completedRpe
        )
    }

    fun LoggingDropSet.toUiModel(): LoggingDropSetUiModel {
        return LoggingDropSetUiModel(
            position = position,
            rpeTarget = rpeTarget,
            repRangeBottom = repRangeBottom,
            repRangeTop = repRangeTop,
            weightRecommendation = weightRecommendation,
            hadInitialWeightRecommendation = hadInitialWeightRecommendation,
            previousSetResultLabel = previousSetResultLabel,
            repRangePlaceholder = repRangePlaceholder,
            setNumberLabel = setNumberLabel,
            complete = complete,
            completedWeight = completedWeight,
            completedReps = completedReps,
            completedRpe = completedRpe,
            dropPercentage = dropPercentage
        )
    }

    fun LoggingMyoRepSet.toUiModel(): LoggingMyoRepSetUiModel {
        return LoggingMyoRepSetUiModel(
            position = position,
            rpeTarget = rpeTarget,
            repRangeBottom = repRangeBottom,
            repRangeTop = repRangeTop,
            weightRecommendation = weightRecommendation,
            hadInitialWeightRecommendation = hadInitialWeightRecommendation,
            previousSetResultLabel = previousSetResultLabel,
            repRangePlaceholder = repRangePlaceholder,
            setNumberLabel = setNumberLabel,
            complete = complete,
            completedWeight = completedWeight,
            completedReps = completedReps,
            completedRpe = completedRpe,
            myoRepSetPosition = myoRepSetPosition,
            setMatching = setMatching,
            maxSets = maxSets,
            repFloor = repFloor
        )
    }

    fun SetResult.toUiModel(): SetResultUiModel {
        return when (this) {
            is StandardSetResult -> toUiModel()
            is MyoRepSetResult -> toUiModel()
            is LinearProgressionSetResult -> toUiModel()
            else -> throw IllegalArgumentException("Unknown type of SetResult")
        }
    }

    fun StandardSetResult.toUiModel(): StandardSetResultUiModel {
        return StandardSetResultUiModel(
            id = id,
            workoutId = workoutId,
            liftId = liftId,
            liftPosition = liftPosition,
            setPosition = setPosition,
            weight = weight,
            reps = reps,
            rpe = rpe,
            oneRepMax = oneRepMax,
            setType = setType,
            isDeload = isDeload
        )
    }

    fun MyoRepSetResult.toUiModel(): MyoRepSetResultUiModel {
        return MyoRepSetResultUiModel(
            id = id,
            workoutId = workoutId,
            liftId = liftId,
            liftPosition = liftPosition,
            setPosition = setPosition,
            weight = weight,
            reps = reps,
            rpe = rpe,
            oneRepMax = oneRepMax,
            myoRepSetPosition = myoRepSetPosition,
            isDeload = isDeload
        )
    }

    fun LinearProgressionSetResult.toUiModel(): LinearProgressionSetResultUiModel {
        return LinearProgressionSetResultUiModel(
            id = id,
            workoutId = workoutId,
            liftId = liftId,
            liftPosition = liftPosition,
            setPosition = setPosition,
            weight = weight,
            reps = reps,
            rpe = rpe,
            oneRepMax = oneRepMax,
            missedLpGoals = missedLpGoals,
            isDeload = isDeload
        )
    }
}
