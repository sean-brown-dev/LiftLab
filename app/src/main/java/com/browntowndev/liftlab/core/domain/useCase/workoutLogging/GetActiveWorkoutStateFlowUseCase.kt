package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.models.workoutLogging.ActiveWorkoutState
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class GetActiveWorkoutStateFlowUseCase(
    private val programsRepository: ProgramsRepository,
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val getWorkoutStateFlowUseCase: GetWorkoutStateFlowUseCase
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<ActiveWorkoutState> {
        return programsRepository.getActiveProgramMetadataFlow()
            .distinctUntilChanged()
            .flatMapLatest { programMetadata ->
                if (programMetadata == null) flowOf(ActiveWorkoutState())
                else {
                    val workoutInProgressFlow = workoutInProgressRepository
                        .getFlow()
                        .distinctUntilChanged()

                    combine(
                        workoutInProgressFlow,
                        getWorkoutStateFlowUseCase(programMetadata),
                    ) { inProgressWorkout, calculatedWorkoutData ->
                        val workoutStateFromCalculatedData = calculatedWorkoutData
                        ActiveWorkoutState(
                            programMetadata = programMetadata,
                            inProgressWorkout = inProgressWorkout,
                            workout = workoutStateFromCalculatedData.calculatedWorkoutPlan,
                            completedSets = workoutStateFromCalculatedData.completedSetsForSession,
                            personalRecords = calculatedWorkoutData.personalRecords,
                        )
                    }
                }
            }
    }
}