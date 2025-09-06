package com.browntowndev.liftlab.dependencyInjection

import com.browntowndev.liftlab.core.common.NetworkMonitor
import com.browntowndev.liftlab.core.data.remote.client.FirestoreClient
import com.browntowndev.liftlab.ui.viewmodels.appBar.TopAppBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.bottomNav.BottomNavBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.donation.DonationViewModel
import com.browntowndev.liftlab.ui.viewmodels.home.HomeViewModel
import com.browntowndev.liftlab.ui.viewmodels.lab.LabViewModel
import com.browntowndev.liftlab.ui.viewmodels.liftDetails.LiftDetailsViewModel
import com.browntowndev.liftlab.ui.viewmodels.liftLibrary.LiftLibraryViewModel
import com.browntowndev.liftlab.ui.viewmodels.picker.PickerViewModel
import com.browntowndev.liftlab.ui.viewmodels.remoteSync.RemoteSyncViewModel
import com.browntowndev.liftlab.ui.viewmodels.settings.SettingsViewModel
import com.browntowndev.liftlab.ui.viewmodels.startup.StartupViewModel
import com.browntowndev.liftlab.ui.viewmodels.timer.DurationTimerViewModel
import com.browntowndev.liftlab.ui.viewmodels.workout.EditWorkoutViewModel
import com.browntowndev.liftlab.ui.viewmodels.workout.WorkoutViewModel
import com.browntowndev.liftlab.ui.viewmodels.workoutBuilder.WorkoutBuilderViewModel
import com.browntowndev.liftlab.ui.viewmodels.workoutHistory.WorkoutHistoryViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    factory { params ->
        LiftDetailsViewModel(
            liftId = params.getOrNull(),
            onNavigateBack = params[1],
            onMergeLift = params[2],
            getLiftWithHistoryStateFlowUseCase = get(),
            updateLiftNameUseCase = get(),
            updateMovementPatternUseCase = get(),
            updateVolumeTypeUseCase = get(),
            addVolumeTypeUseCase = get(),
            removeVolumeTypeUseCase = get(),
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
            deleteCustomSetUseCase = get(),
            updateCustomLiftSetUseCase = get(),
            updateManyCustomLiftSetsUseCase = get(),
            addSetUseCase = get(),
            updateWorkoutLiftDeloadWeekUseCase = get(),
            liftLevelDeloadsEnabled = params.get(),
            eventBus = get()
        )
    }
    factory { params ->
        LiftLibraryViewModel(
            deleteLiftUseCase = get(),
            replaceWorkoutLiftUseCase = get(),
            createLiftMetricChartsUseCase = get(),
            createWorkoutLiftsFromLiftsUseCase = get(),
            getFilterableLiftsStateFlowUseCase = get(),
            mergeLiftsUseCase = get(),
            onNavigateHome = params[0],
            onNavigateToWorkoutBuilder = params[1],
            onNavigateToActiveWorkout = params[2],
            onNavigateToLiftDetails = params[3],
            workoutId = params[4],
            addAtPosition = params[5],
            mergeLiftId = params[6],
            initialMovementPatternFilter = params.get(),
            newLiftMetricChartIds = params.get(),
            eventBus = get()
        )
    }
    factory { params ->
        WorkoutViewModel(
            getWorkoutCompletionSummaryUseCase = get(),
            getActiveProgramWorkoutCountFlowUseCase = get(),
            hydrateLoggingWorkoutWithExistingLiftDataUseCase = get(),
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
            updateLiftNoteUseCase = get(),
            completeSetUseCase = get(),
            undoSetCompletionUseCase = get(),
            navigateToWorkoutHistory = params.get(),
            eventBus = get(),
        )
    }
    factory { params ->
        EditWorkoutViewModel(
            workoutLogEntryId = params.get(),
            upsertSetLogEntriesFromSetResultsUseCase = get(),
            upsertExistingSetResultUseCase = get(),
            deleteSetLogEntryByIdUseCase = get(),
            onNavigateBack = params.get(),
            completeSetUseCase = get(),
            undoSetCompletionUseCase = get(),
            getCompletedWorkoutStateFlowUseCase = get(),
            eventBus = get()
        )
    }
    factory {
        DurationTimerViewModel(liftLabTimer = get(DurationTimer))
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
    factory {
        val isLoggedInFlow = get<FirestoreClient>().isUserLoggedInFlow
        val isOnlineFlow = get<NetworkMonitor>().isOnlineFlow
        RemoteSyncViewModel(
            syncOrchestrator = get(),
            isOnlineFlow = isOnlineFlow,
            isLoggedInFlow = isLoggedInFlow,
        )
    }

    factory {
        TopAppBarViewModel(
            context = get(),
            getRestTimerInProgressFlowUseCase = get(),
            restTimerCompletedUseCase = get(),
            liftLabTimer = get(CountdownTimer)
        )
    }

    viewModelOf(::DonationViewModel)
    viewModelOf(::LabViewModel)
    viewModelOf(::BottomNavBarViewModel)
    viewModelOf(::PickerViewModel)
    viewModelOf(::StartupViewModel)
}