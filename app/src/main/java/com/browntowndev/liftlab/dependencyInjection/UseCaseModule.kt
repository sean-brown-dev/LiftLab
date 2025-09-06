package com.browntowndev.liftlab.dependencyInjection

import com.browntowndev.liftlab.core.domain.models.workoutLogging.BuildSetResultUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.AddVolumeTypeUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.CreateLiftUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.DeleteLiftUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.GetFilterableLiftsStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.GetLiftWithHistoryStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.MergeLiftsUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.RemoveVolumeTypeUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.UpdateLiftNameUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.UpdateMovementPatternUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.UpdateVolumeTypeUseCase
import com.browntowndev.liftlab.core.domain.useCase.metrics.CreateLiftMetricChartsUseCase
import com.browntowndev.liftlab.core.domain.useCase.metrics.DeleteLiftMetricChartByIdUseCase
import com.browntowndev.liftlab.core.domain.useCase.metrics.DeleteVolumeMetricChartByIdUseCase
import com.browntowndev.liftlab.core.domain.useCase.metrics.GetConfiguredMetricsStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.metrics.GetGroupedLiftMetricChartDataUseCase
import com.browntowndev.liftlab.core.domain.useCase.metrics.GetGroupedVolumeMetricChartDataUseCase
import com.browntowndev.liftlab.core.domain.useCase.metrics.GetSummarizedWorkoutMetricsStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.metrics.InsertManyLiftMetricChartsUseCase
import com.browntowndev.liftlab.core.domain.useCase.metrics.UpsertManyVolumeMetricChartsUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.CreateProgramUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.CreateWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.DeleteProgramUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.DeleteWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.GetActiveProgramWorkoutCountFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.GetProgramConfigurationStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.ReorderWorkoutsUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.SetProgramAsActiveUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.UpdateProgramDeloadWeekUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.UpdateProgramNameUseCase
import com.browntowndev.liftlab.core.domain.useCase.progression.CalculateLoggingWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.settings.GetSettingConfigurationStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.settings.UpdateLiftSpecificDeloadSettingUseCase
import com.browntowndev.liftlab.core.domain.useCase.settings.UpdateSettingUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.AddSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.ConvertWorkoutLiftTypeUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.CreateWorkoutLiftsFromLiftsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.DeleteCustomSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.DeleteWorkoutLiftUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.GetWorkoutConfigurationStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.ReorderWorkoutBuilderLiftsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.ReorderWorkoutLiftsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.ReplaceWorkoutLiftUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateCustomLiftSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateLiftIncrementOverrideUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateManyCustomLiftSetsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateRestTimeUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateWorkoutLiftDeloadWeekUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateWorkoutLiftUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateWorkoutNameUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.CancelWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.CompleteSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.CompleteWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.DeleteSetLogEntryByIdUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.DeleteSetResultByIdUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.DeleteWorkoutLogEntryUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.GetActiveWorkoutStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.GetCompletedWorkoutStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.GetNewestSetResultsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.GetPersonalRecordsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.GetRestTimerInProgressFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.GetWorkoutCompletionSummaryUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.GetWorkoutStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.HydrateLoggingWorkoutWithCompletedSetsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.HydrateLoggingWorkoutWithExistingLiftDataUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.InsertRestTimerInProgressUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.RestTimerCompletedUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.SkipDeloadAndStartWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.StartWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UndoSetCompletionUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpdateLiftNoteUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpsertExistingSetResultUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpsertManySetResultsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpsertSetLogEntriesFromSetResultsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpsertSetResultUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val useCaseModule = module {
    // Workout Logging
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
    singleOf(::CompleteSetUseCase)
    singleOf(::UndoSetCompletionUseCase)
    singleOf(::BuildSetResultUseCase)
    singleOf(::GetCompletedWorkoutStateFlowUseCase)
    singleOf(::UpsertSetLogEntriesFromSetResultsUseCase)
    singleOf(::UpsertExistingSetResultUseCase)
    singleOf(::DeleteSetLogEntryByIdUseCase)
    singleOf(::DeleteWorkoutLogEntryUseCase)
    singleOf(::GetRestTimerInProgressFlowUseCase)

    // Workout Configuration
    singleOf(::AddSetUseCase)
    singleOf(::ConvertWorkoutLiftTypeUseCase)
    singleOf(::DeleteCustomSetUseCase)
    singleOf(::DeleteWorkoutLiftUseCase)
    singleOf(::GetWorkoutConfigurationStateFlowUseCase)
    singleOf(::ReorderWorkoutBuilderLiftsUseCase)
    singleOf(::UpdateCustomLiftSetUseCase)
    singleOf(::UpdateManyCustomLiftSetsUseCase)
    singleOf(::UpdateLiftIncrementOverrideUseCase)
    singleOf(::UpdateWorkoutLiftUseCase)
    singleOf(::UpdateWorkoutNameUseCase)
    singleOf(::UpdateWorkoutLiftDeloadWeekUseCase)
    singleOf(::UpdateRestTimeUseCase)
    singleOf(::CreateWorkoutLiftsFromLiftsUseCase)
    singleOf(::ReplaceWorkoutLiftUseCase)

    // Program Configuration
    singleOf(::CreateProgramUseCase)
    singleOf(::CreateWorkoutUseCase)
    singleOf(::DeleteProgramUseCase)
    singleOf(::DeleteWorkoutUseCase)
    singleOf(::GetProgramConfigurationStateFlowUseCase)
    singleOf(::ReorderWorkoutsUseCase)
    singleOf(::SetProgramAsActiveUseCase)
    singleOf(::UpdateProgramDeloadWeekUseCase)
    singleOf(::UpdateProgramNameUseCase)
    singleOf(::GetActiveProgramWorkoutCountFlowUseCase)

    // Lift Configuration
    singleOf(::GetFilterableLiftsStateFlowUseCase)
    singleOf(::GetLiftWithHistoryStateFlowUseCase)
    singleOf(::DeleteLiftUseCase)
    singleOf(::CreateLiftUseCase)
    singleOf(::UpdateLiftNameUseCase)
    singleOf(::UpdateMovementPatternUseCase)
    singleOf(::UpdateVolumeTypeUseCase)
    singleOf(::AddVolumeTypeUseCase)
    singleOf(::RemoveVolumeTypeUseCase)
    singleOf(::MergeLiftsUseCase)

    // Metrics
    singleOf(::GetGroupedLiftMetricChartDataUseCase)
    singleOf(::GetGroupedVolumeMetricChartDataUseCase)
    singleOf(::CreateLiftMetricChartsUseCase)
    singleOf(::GetConfiguredMetricsStateFlowUseCase)
    singleOf(::UpsertManyVolumeMetricChartsUseCase)
    singleOf(::InsertManyLiftMetricChartsUseCase)
    singleOf(::DeleteVolumeMetricChartByIdUseCase)
    singleOf(::DeleteLiftMetricChartByIdUseCase)
    singleOf(::GetSummarizedWorkoutMetricsStateFlowUseCase)

    // Settings
    singleOf(::GetSettingConfigurationStateFlowUseCase)
    singleOf(::UpdateLiftSpecificDeloadSettingUseCase)
    singleOf(::UpdateSettingUseCase)
}