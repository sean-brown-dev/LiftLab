package com.browntowndev.liftlab.dependencyInjection

import com.browntowndev.liftlab.core.domain.useCase.charts.GetGroupedLiftMetricChartDataUseCase
import com.browntowndev.liftlab.core.domain.useCase.charts.GetGroupedVolumeMetricChartDataUseCase
import com.browntowndev.liftlab.core.domain.useCase.shared.UpdateRestTimeUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.CancelWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.CompleteWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.DeleteSetResultByIdUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.GetActiveWorkoutStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.GetNewestSetResultsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.GetPersonalRecordsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.GetWorkoutCompletionSummaryUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.GetWorkoutStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.HydrateLoggingWorkoutWithCompletedSetsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.HydrateLoggingWorkoutWithExistingLiftDataUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.InsertRestTimerInProgressUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.ReorderWorkoutLiftsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.RestTimerCompletedUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.SkipDeloadAndStartWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.StartWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.UpdateLiftNoteUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.UpsertManySetResultsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.UpsertSetResultUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.progression.CalculateLoggingWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutBuilder.AddSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutBuilder.ConvertWorkoutLiftTypeUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutBuilder.ReorderWorkoutBuilderLiftsUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val useCaseModule = module {
    // Workout
    singleOf(::CalculateLoggingWorkoutUseCase)
    singleOf(::HydrateLoggingWorkoutWithCompletedSetsUseCase)
    singleOf(::HydrateLoggingWorkoutWithExistingLiftDataUseCase)
    singleOf(::GetWorkoutCompletionSummaryUseCase)
    singleOf(::GetNewestSetResultsUseCase)
    singleOf(::GetPersonalRecordsUseCase)
    singleOf(::ReorderWorkoutLiftsUseCase)
    singleOf(::SkipDeloadAndStartWorkoutUseCase)
    singleOf(::StartWorkoutUseCase)
    singleOf(::GetWorkoutStateFlowUseCase)
    singleOf(::CompleteWorkoutUseCase)
    singleOf(::CancelWorkoutUseCase)
    singleOf(::DeleteSetResultByIdUseCase)
    singleOf(::GetActiveWorkoutStateFlowUseCase)
    singleOf(::InsertRestTimerInProgressUseCase)
    singleOf(::RestTimerCompletedUseCase)
    singleOf(::UpdateLiftNoteUseCase)
    singleOf(::UpsertManySetResultsUseCase)
    singleOf(::UpsertSetResultUseCase)

    // Home
    singleOf(::GetGroupedLiftMetricChartDataUseCase)
    singleOf(::GetGroupedVolumeMetricChartDataUseCase)

    // Workout Builder
    singleOf(::ConvertWorkoutLiftTypeUseCase)
    singleOf(::ReorderWorkoutBuilderLiftsUseCase)
    singleOf(::AddSetUseCase)

    // Shared
    singleOf(::UpdateRestTimeUseCase)
}