package com.browntowndev.liftlab.dependencyInjection

import com.browntowndev.liftlab.ui.viewmodels.BottomNavBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.CountdownTimerViewModel
import com.browntowndev.liftlab.ui.viewmodels.DonationViewModel
import com.browntowndev.liftlab.ui.viewmodels.EditWorkoutViewModel
import com.browntowndev.liftlab.ui.viewmodels.RemoteSyncViewModel
import com.browntowndev.liftlab.ui.viewmodels.HomeViewModel
import com.browntowndev.liftlab.ui.viewmodels.LabViewModel
import com.browntowndev.liftlab.ui.viewmodels.LiftDetailsViewModel
import com.browntowndev.liftlab.ui.viewmodels.LiftLibraryViewModel
import com.browntowndev.liftlab.ui.viewmodels.PickerViewModel
import com.browntowndev.liftlab.ui.viewmodels.SettingsViewModel
import com.browntowndev.liftlab.ui.viewmodels.TimerViewModel
import com.browntowndev.liftlab.ui.viewmodels.TopAppBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.WorkoutBuilderViewModel
import com.browntowndev.liftlab.ui.viewmodels.WorkoutHistoryViewModel
import com.browntowndev.liftlab.ui.viewmodels.WorkoutViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    factory { params ->
        LiftDetailsViewModel(
            liftId = params.getOrNull(),
            onNavigateBack = params.get(),
            getLiftWithHistoryStateFlowUseCase = get(),
            updateLiftNameUseCase = get(),
            updateMovementPatternUseCase = get(),
            updateVolumeTypeUseCase = get(),
            createLiftUseCase = get(),
            eventBus = get()
        )
    }
    factory { params ->
        WorkoutBuilderViewModel(
            workoutId = params.get(),
            onNavigateBack = params.get(),
            convertWorkoutLiftTypeUseCase = get(),
            reorderWorkoutBuilderLiftsUseCase = get(),
            getWorkoutConfigurationStateFlowUseCase = get(),
            deleteWorkoutLiftUseCase = get(),
            updateWorkoutNameUseCase = get(),
            updateRestTimeUseCase = get(),
            updateLiftIncrementOverrideUseCase = get(),
            updateWorkoutLiftUseCase = get(),
            deleteCustomLiftSetByPositionUseCase = get(),
            updateCustomLiftSetUseCase = get(),
            addSetUseCase = get(),
            updateWorkoutLiftDeloadWeekUseCase = get(),
            liftLevelDeloadsEnabled = params.get(),
            eventBus = get()
        )
    }
    factory { params ->
        LiftLibraryViewModel(
            deleteLiftUseCase = get(),
            replaceWorkoutLIftUseCase = get(),
            createLiftMetricChartsUseCase = get(),
            createWorkoutLiftsFromLiftsUseCase = get(),
            getFilterableLiftsStateFlowUseCase = get(),
            onNavigateHome = params[0],
            onNavigateToWorkoutBuilder = params[1],
            onNavigateToActiveWorkout = params[2],
            onNavigateToLiftDetails = params[3],
            workoutId = params[4],
            addAtPosition = params[5],
            initialMovementPatternFilter = params.get(),
            newLiftMetricChartIds = params.get(),
            eventBus = get()
        )
    }
    factory { params ->
        WorkoutViewModel(
            getWorkoutCompletionSummaryUseCase = get(),
            reorderWorkoutLiftsUseCase = get(),
            startWorkoutUseCase = get(),
            skipDeloadAndStartWorkoutUseCase = get(),
            completeWorkoutUseCase = get(),
            cancelWorkoutUseCase = get(),
            getActiveWorkoutStateFlowUseCase = get(),
            upsertManySetResultsUseCase = get(),
            upsertSetResultUseCase = get(),
            deleteSetResultByIdUseCase = get(),
            insertRestTimerInProgressUseCase = get(),
            updateRestTimeUseCase = get(),
            restTimerCompletedUseCase = get(),
            updateLiftNoteUseCase = get(),
            completeSetUseCase = get(),
            undoSetCompletionUseCase = get(),
            navigateToWorkoutHistory = params[0],
            cancelRestTimer = params[1],
            eventBus = get(),
        )
    }
    factory { params ->
        EditWorkoutViewModel(
            workoutLogEntryId = params.get(),
            upsertSetResultUseCase = get(),
            upsertManySetLogEntriesUseCase = get(),
            upsertSetLogEntryUseCase = get(),
            deleteSetLogEntryByIdUseCase = get(),
            onNavigateBack = params.get(),
            completeSetUseCase = get(),
            undoSetCompletionUseCase = get(),
            getCompletedWorkoutStateFlowUseCase = get(),
            eventBus = get()
        )
    }
    factory { params ->
        CountdownTimerViewModel(onComplete = params.get())
    }
    factory { params ->
        HomeViewModel(
            getConfiguredMetricsStateFlowUseCase = get(),
            upsertManyVolumeMetricChartsUseCase = get(),
            insertManyLiftMetricChartsUseCase = get(),
            deleteVolumeMetricChartByIdUseCase = get(),
            deleteLiftMetricChartByIdUseCase = get(),
            onNavigateToSettingsMenu = params[0],
            onNavigateToLiftLibrary = params[1],
            onUserLoggedIn = params[2],
            firebaseAuth = get(),
            eventBus = get()
        )
    }
    factory { params ->
        SettingsViewModel(
            getSettingConfigurationStateFlowUseCase = get(),
            updateLiftSpecificDeloadSettingUseCase = get(),
            updateSettingUseCase = get(),
            onNavigateBack = params.get(),
            eventBus = get()
        )
    }
    factory { params ->
        WorkoutHistoryViewModel(
            getSummarizedWorkoutMetricsStateFlowUseCase = get(),
            deleteWorkoutLogEntryUseCase = get(),
            onNavigateBack = params.get(),
            eventBus = get()
        )
    }

    viewModelOf(::DonationViewModel)
    viewModelOf(::LabViewModel)
    viewModelOf(::TopAppBarViewModel)
    viewModelOf(::BottomNavBarViewModel)
    viewModelOf(::TimerViewModel)
    viewModelOf(::PickerViewModel)
    viewModelOf(::RemoteSyncViewModel)
}