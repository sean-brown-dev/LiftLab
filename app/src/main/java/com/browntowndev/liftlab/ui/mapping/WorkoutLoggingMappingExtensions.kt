package com.browntowndev.liftlab.ui.mapping

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
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
import com.browntowndev.liftlab.ui.utils.getRpeTargetPlaceholder
import kotlin.time.Duration

fun LoggingWorkout.toUiModel(defaultRestTime: Duration): LoggingWorkoutUiModel {
    return LoggingWorkoutUiModel(
        id = this.id,
        name = this.name,
        lifts = this.lifts.fastMap { it.toUiModel(defaultRestTime) }.sortedBy { it.position }
    )
}

fun LoggingWorkoutUiModel.toDomainModel(): LoggingWorkout {
    return LoggingWorkout(
        id = this.id,
        name = this.name,
        lifts = this.lifts.fastMap { it.toDomainModel() }
    )
}

fun LoggingWorkoutLift.toUiModel(defaultRestTime: Duration): LoggingWorkoutLiftUiModel {
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
        restTime = restTime ?: defaultRestTime,
        restTimerEnabled = restTimerEnabled,
        sets = sets.fastMap { it.toUiModel(progressionScheme) }.sortedWith(
            compareBy(
                { it.position },
                { (it as? LoggingMyoRepSetUiModel)?.myoRepSetPosition ?: 0 }
            )
        )
    )
}

fun LoggingWorkoutLiftUiModel.toDomainModel(): LoggingWorkoutLift {
    return LoggingWorkoutLift(
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
        sets = sets.fastMap { it.toDomainModel() }
    )
}

fun GenericLoggingSet.toUiModel(progressionScheme: ProgressionScheme): LoggingSetUiModel {
    return when (this) {
        is LoggingStandardSet -> toUiModel(progressionScheme)
        is LoggingDropSet -> toUiModel(progressionScheme)
        is LoggingMyoRepSet -> toUiModel(progressionScheme)
        else -> throw IllegalArgumentException("Unknown type of GenericLoggingSet")
    }
}

fun LoggingSetUiModel.toDomainModel(): GenericLoggingSet {
    return when (this) {
        is LoggingStandardSetUiModel -> toDomainModel()
        is LoggingDropSetUiModel -> toDomainModel()
        is LoggingMyoRepSetUiModel -> toDomainModel()
        else -> throw IllegalArgumentException("Unknown type of LoggingSetUiModel")
    }
}

fun LoggingStandardSet.toUiModel(progressionScheme: ProgressionScheme): LoggingStandardSetUiModel {
    return LoggingStandardSetUiModel(
        position = position,
        rpeTarget = rpeTarget,
        rpeTargetPlaceholder = getRpeTargetPlaceholder(rpeTarget, position, progressionScheme),
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

fun LoggingStandardSetUiModel.toDomainModel(): LoggingStandardSet {
    return LoggingStandardSet(
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

fun LoggingDropSet.toUiModel(progressionScheme: ProgressionScheme): LoggingDropSetUiModel {
    return LoggingDropSetUiModel(
        position = position,
        rpeTarget = rpeTarget,
        rpeTargetPlaceholder = getRpeTargetPlaceholder(rpeTarget, position, progressionScheme),
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

fun LoggingDropSetUiModel.toDomainModel(): LoggingDropSet {
    return LoggingDropSet(
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

fun LoggingMyoRepSet.toUiModel(progressionScheme: ProgressionScheme): LoggingMyoRepSetUiModel {
    return LoggingMyoRepSetUiModel(
        position = position,
        rpeTarget = rpeTarget,
        rpeTargetPlaceholder = getRpeTargetPlaceholder(rpeTarget, position, progressionScheme),
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
        repFloor = repFloor,
        isNew = isNew,
    )
}

fun LoggingMyoRepSetUiModel.toDomainModel(): LoggingMyoRepSet {
    return LoggingMyoRepSet(
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
        repFloor = repFloor,
        isNew = isNew,
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

fun SetResultUiModel.toDomainModel(): SetResult {
    return when (this) {
        is StandardSetResultUiModel -> toDomainModel()
        is MyoRepSetResultUiModel -> toDomainModel()
        is LinearProgressionSetResultUiModel -> toDomainModel()
        else -> throw IllegalArgumentException("Unknown type of SetResultUiModel")
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
        persistedOneRepMax = oneRepMax,
        setType = setType,
        isDeload = isDeload
    )
}

fun StandardSetResultUiModel.toDomainModel(): StandardSetResult {
    return StandardSetResult(
        id = id,
        workoutId = workoutId,
        liftId = liftId,
        liftPosition = liftPosition,
        setPosition = setPosition,
        weight = weight,
        reps = reps,
        rpe = rpe,
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
        persistedOneRepMax = oneRepMax,
        myoRepSetPosition = myoRepSetPosition,
        isDeload = isDeload
    )
}

fun MyoRepSetResultUiModel.toDomainModel(): MyoRepSetResult {
    return MyoRepSetResult(
        id = id,
        workoutId = workoutId,
        liftId = liftId,
        liftPosition = liftPosition,
        setPosition = setPosition,
        weight = weight,
        reps = reps,
        rpe = rpe,
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
        persistedOneRepMax = oneRepMax,
        missedLpGoals = missedLpGoals,
        isDeload = isDeload
    )
}

fun LinearProgressionSetResultUiModel.toDomainModel(): LinearProgressionSetResult {
    return LinearProgressionSetResult(
        id = id,
        workoutId = workoutId,
        liftId = liftId,
        liftPosition = liftPosition,
        setPosition = setPosition,
        weight = weight,
        reps = reps,
        rpe = rpe,
        missedLpGoals = missedLpGoals,
        isDeload = isDeload
    )
}
