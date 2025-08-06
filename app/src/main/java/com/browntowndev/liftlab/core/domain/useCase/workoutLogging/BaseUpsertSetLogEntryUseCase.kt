package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.extensions.findSet
import com.browntowndev.liftlab.core.domain.extensions.toSetLogEntry
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry

abstract class BaseUpsertSetLogEntryUseCase {
    protected fun getSetLogEntryFromSetResult(
        loggingWorkoutLift: LoggingWorkoutLift,
        setResult: SetResult,
        workoutLogEntryId: Long,
    ): SetLogEntry {
        val set = loggingWorkoutLift.findSet(
            setPosition = setResult.setPosition,
            myoRepSetPosition = (setResult as? MyoRepSetResult)?.myoRepSetPosition
        ) ?: loggingWorkoutLift.findSet(
            setPosition = setResult.setPosition - 1, // Newly added results have incremented position
            myoRepSetPosition = (setResult as? MyoRepSetResult)?.myoRepSetPosition
        ) ?: throw Exception("Set not found")

        val setLogEntry = setResult.toSetLogEntry(
            liftName = loggingWorkoutLift.liftName,
            liftMovementPattern = loggingWorkoutLift.liftMovementPattern,
            progressionScheme = loggingWorkoutLift.progressionScheme,
            workoutLogEntryId = workoutLogEntryId,
            repRangeTop = set.repRangeTop,
            repRangeBottom = set.repRangeBottom,
            rpeTarget = set.rpeTarget,
            weightRecommendation = set.weightRecommendation,
        )

        return setLogEntry
    }
}