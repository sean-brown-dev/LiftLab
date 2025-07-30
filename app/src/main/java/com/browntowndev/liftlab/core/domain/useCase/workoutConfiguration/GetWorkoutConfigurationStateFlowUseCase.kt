package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.domain.extensions.getRecalculatedWorkoutLiftStepSizeOptions
import com.browntowndev.liftlab.core.domain.models.workout.WorkoutConfigurationState
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

class GetWorkoutConfigurationStateFlowUseCase(
    private val workoutsRepository: WorkoutsRepository,
    private val programsRepository: ProgramsRepository,
) {
    operator fun invoke(
        workoutId: Long,
        liftLevelDeloadsEnabled: Boolean,
    ): Flow<WorkoutConfigurationState> {
        return workoutsRepository.getFlow(workoutId)
            .distinctUntilChanged()
            .map { workout ->
                WorkoutConfigurationState(
                    workout = workout,
                )
            }.scan(WorkoutConfigurationState()) { oldState, newState ->
                val programDeloadWeek =
                    if (newState.workout != null && newState.workout.programId != oldState.workout?.programId) {
                        programsRepository.getDeloadWeek(newState.workout.programId)
                    } else oldState.programDeloadWeek

                WorkoutConfigurationState(
                    workout = newState.workout,
                    programDeloadWeek = programDeloadWeek,
                    workoutLiftStepSizeOptions = newState.workout?.getRecalculatedWorkoutLiftStepSizeOptions(
                        programDeloadWeek = programDeloadWeek!!,
                        liftLevelDeloadsEnabled = liftLevelDeloadsEnabled,
                    ) ?: mapOf()
                )
            }
    }
}