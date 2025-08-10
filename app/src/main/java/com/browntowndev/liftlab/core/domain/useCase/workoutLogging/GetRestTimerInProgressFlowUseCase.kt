package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.models.workoutLogging.RestTimerInProgressState
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetRestTimerInProgressFlowUseCase(
    private val restTimerInProgressRepository: RestTimerInProgressRepository,
) {
    operator fun invoke(): Flow<RestTimerInProgressState> {
        return restTimerInProgressRepository.getFlow()
            .map { restTimerInProgress ->
                if (restTimerInProgress != null) {
                    RestTimerInProgressState(
                        totalRestTime = restTimerInProgress.restTime,
                        timeStartedInMillis = restTimerInProgress.timeStartedInMillis
                    )
                } else RestTimerInProgressState()
            }
    }
}