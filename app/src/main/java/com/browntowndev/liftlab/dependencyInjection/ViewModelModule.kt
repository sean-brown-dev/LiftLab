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
            onNavigateBack = params.get(),
            liftId = params.get(),
            liftsRepository = get(),
            workoutLogRepository = get(),
            transactionScope = get(),
            eventBus = get()
        )
    }
    factory { params ->
        WorkoutBuilderViewModel(
            workoutId = params.get(),
            onNavigateBack = params.get(),
            programsRepository = get(),
            workoutsRepository = get(),
            workoutLiftsRepository = get(),
            customLiftSetsRepository = get(),
            liftsRepository = get(),
            convertWorkoutLiftTypeUseCase = get(),
            reorderWorkoutBuilderLiftsUseCase = get(),
            addSetUseCase = get(),
            liftLevelDeloadsEnabled = params.get(),
            transactionScope = get(),
            eventBus = get()
        )
    }
    factory { params ->
        LiftLibraryViewModel(
            liftsRepository = get(),
            workoutLiftsRepositoryImpl = get(),
            liftMetricChartsRepository = get(),
            onNavigateHome = params[0],
            onNavigateToWorkoutBuilder = params[1],
            onNavigateToActiveWorkout = params[2],
            onNavigateToLiftDetails = params[3],
            workoutId = params[4],
            addAtPosition = params[5],
            initialMovementPatternFilter = params.get(),
            newLiftMetricChartIds = params.get(),
            transactionScope = get(),
            eventBus = get()
        )
    }
    factory { params ->
        WorkoutViewModel(
            getWorkoutStateFlowUseCase = get(),
            getWorkoutCompletionSummaryUseCase = get(),
            reorderWorkoutLiftsUseCase = get(),
            startWorkoutUseCase = get(),
            skipDeloadAndStartWorkoutUseCase = get(),
            completeWorkoutUseCase = get(),
            cancelWorkoutUseCase = get(),
            programsRepository = get(),
            setResultsRepository = get(),
            workoutInProgressRepository = get(),
            restTimerInProgressRepository = get(),
            liftsRepository = get(),
            navigateToWorkoutHistory = params[0],
            cancelRestTimer = params[1],
            transactionScope = get(),
            eventBus = get(),
        )
    }
    factory { params ->
        EditWorkoutViewModel(
            workoutLogEntryId = params.get(),
            workoutLogRepository = get(),
            setResultsRepository = get(),
            setLogEntryRepository = get(),
            onNavigateBack = params.get(),
            transactionScope = get(),
            eventBus = get()
        )
    }
    factory { params ->
        CountdownTimerViewModel(onComplete = params.get())
    }
    factory { params ->
        HomeViewModel(
            programsRepository = get(),
            workoutLogRepository = get(),
            liftMetricChartsRepository = get(),
            volumeMetricChartsRepository = get(),
            liftsRepository = get(),
            getGroupedLiftMetricChartDataUseCase = get(),
            getGroupedVolumeMetricChartDataUseCase = get(),
            onNavigateToSettingsMenu = params[0],
            onNavigateToLiftLibrary = params[1],
            onUserLoggedIn = params[2],
            firebaseAuth = get(),
            transactionScope = get(),
            eventBus = get()
        )
    }
    factory { params ->
        SettingsViewModel(
            programsRepository = get(),
            workoutLiftsRepository = get(),
            onNavigateBack = params.get(),
            transactionScope = get(),
            eventBus = get()
        )
    }
    factory { params ->
        WorkoutHistoryViewModel(
            workoutLogRepository = get(),
            onNavigateBack = params.get(),
            transactionScope = get(),
            eventBus = get()
        )
    }
    factory { params ->
        DonationViewModel(
            billingClientBuilder = params.get(),
            transactionScope = get(),
            eventBus = get()
        )
    }

    viewModelOf(::LabViewModel)
    viewModelOf(::TopAppBarViewModel)
    viewModelOf(::BottomNavBarViewModel)
    viewModelOf(::TimerViewModel)
    viewModelOf(::PickerViewModel)
    viewModelOf(::RemoteSyncViewModel)
}