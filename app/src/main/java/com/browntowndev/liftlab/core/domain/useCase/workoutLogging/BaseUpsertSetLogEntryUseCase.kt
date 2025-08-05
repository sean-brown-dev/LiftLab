package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.extensions.copyCompletionDataFrom
import com.browntowndev.liftlab.core.domain.extensions.findSet
import com.browntowndev.liftlab.core.domain.extensions.toSetLogEntry
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.useCase.utils.SetResultKey

abstract class BaseUpsertSetLogEntryUseCase {
    protected fun getSetLogEntryFromSetResult(
        loggingWorkoutLift: LoggingWorkoutLift,
        setResult: SetResult,
        workoutLogEntryId: Long,
        mesoCycle: Int,
        microCycle: Int
    ): SetLogEntry {
        val set = loggingWorkoutLift.findSet(
            setPosition = setResult.setPosition,
            myoRepSetPosition = (setResult as? MyoRepSetResult)?.myoRepSetPosition
        )
        val setLogEntry = setResult.toSetLogEntry(
            liftName = loggingWorkoutLift.liftName,
            liftMovementPattern = loggingWorkoutLift.liftMovementPattern,
            progressionScheme = loggingWorkoutLift.progressionScheme,
            workoutLogEntryId = workoutLogEntryId,
            repRangeTop = set.repRangeTop,
            repRangeBottom = set.repRangeBottom,
            rpeTarget = set.rpeTarget,
            weightRecommendation = set.weightRecommendation,
            mesoCycle = mesoCycle,
            microCycle = microCycle,
        )

        return setLogEntry
    }

    protected fun getUpdatedSetResultIfExistsOrNull(
        allSetResults: List<SetResult>,
        setResult: SetResult
    ): SetResult? {
        // Have to look up by key because SetResult's id here is SetLogEntry table
        val setResultsByResultKey = allSetResults.associateBy {
            SetResultKey(
                liftId = it.liftId,
                liftPosition = it.liftPosition,
                setPosition = it.setPosition,
                myoRepSetPosition = (it as? MyoRepSetResult)?.myoRepSetPosition
            )
        }
        val setResultKey = SetResultKey(
            liftId = setResult.liftId,
            liftPosition = setResult.liftPosition,
            setPosition = setResult.setPosition,
            myoRepSetPosition = (setResult as? MyoRepSetResult)?.myoRepSetPosition
        )
        val updatedResult = if (setResultKey in setResultsByResultKey) {
            val existingSetResult = setResultsByResultKey[setResultKey]!!
            setResult.copyCompletionDataFrom(existingSetResult)
        } else null

        return updatedResult
    }
}