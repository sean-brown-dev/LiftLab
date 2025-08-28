package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.extensions.getRecalculatedWorkoutLiftStepSizeOptions
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.WorkoutConfigurationState
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
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

                val stepSizeOptions = newState.workout?.getRecalculatedWorkoutLiftStepSizeOptions(
                    programDeloadWeek = programDeloadWeek!!,
                    liftLevelDeloadsEnabled = liftLevelDeloadsEnabled,
                ) ?: mapOf()
                val workout = newState.workout?.let { workout ->
                    workout.copy(
                        lifts = workout.lifts.fastMap { lift ->
                            if (lift is StandardWorkoutLift &&
                                lift.progressionScheme == ProgressionScheme.WAVE_LOADING_PROGRESSION &&
                                lift.stepSize == null
                            ) {
                                lift.copy(stepSize = stepSizeOptions[lift.id]?.keys?.firstOrNull())
                            } else {
                                lift
                            }
                        }
                    )
                }

                WorkoutConfigurationState(
                    workout = workout,
                    programDeloadWeek = programDeloadWeek,
                    workoutLiftStepSizeOptions = stepSizeOptions,
                )
            }.drop(1)
    }
}