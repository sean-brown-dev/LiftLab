package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import android.util.Log
import com.browntowndev.liftlab.core.domain.models.workoutLogging.ActiveWorkoutState
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class GetActiveWorkoutStateFlowUseCase(
    private val programsRepository: ProgramsRepository,
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val getWorkoutStateFlowUseCase: GetWorkoutStateFlowUseCase
) {
    companion object {
        private const val TAG = "GetActiveWorkoutStateFlowUseCase"
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<ActiveWorkoutState> {
        val workoutInProgressFlow =
            workoutInProgressRepository.getFlow().distinctUntilChanged()

        val activeProgramMetadataFlow =
            programsRepository.getActiveProgramMetadataFlow().distinctUntilChanged()

        return combine(
            workoutInProgressFlow,
            activeProgramMetadataFlow
        ) { inProgressWorkout, programMetadata ->
            inProgressWorkout to programMetadata
        }.flatMapLatest { (inProgressWorkout, programMetadata) ->
            Log.d(TAG, "inProgressWorkout=$inProgressWorkout, programMetadata=$programMetadata")
            if (programMetadata == null || programMetadata.workoutCount == 0) {
                flowOf(ActiveWorkoutState())
            } else {
                getWorkoutStateFlowUseCase(programMetadata).map { calculated ->
                    Log.d(TAG, "calculated=$calculated")
                    ActiveWorkoutState(
                        programMetadata = programMetadata,
                        inProgressWorkout = inProgressWorkout,
                        workout = calculated.calculatedWorkoutPlan,
                        completedSets = calculated.completedSetsForSession,
                        personalRecords = calculated.personalRecords,
                    )
                }
            }
        }.distinctUntilChanged()
    }
}