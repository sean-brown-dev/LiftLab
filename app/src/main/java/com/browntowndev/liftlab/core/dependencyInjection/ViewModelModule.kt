package com.browntowndev.liftlab.core.dependencyInjection

import com.browntowndev.liftlab.core.progression.StandardProgressionFactory
import com.browntowndev.liftlab.ui.viewmodels.BottomNavBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.BottomSheetViewModel
import com.browntowndev.liftlab.ui.viewmodels.CountdownTimerViewModel
import com.browntowndev.liftlab.ui.viewmodels.DonationViewModel
import com.browntowndev.liftlab.ui.viewmodels.EditWorkoutViewModel
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
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { LabViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { params -> LiftDetailsViewModel(params.get(), params[0], get(), get(), get(), get()) }
    viewModel { params -> WorkoutBuilderViewModel(params.get(), params.get(), get(), get(), get(), get(), get(), params.get(), get(), get()) }
    viewModel { params -> LiftLibraryViewModel(get(), get(), get(), params[0], params[1], params[2], params[3], params[4], params[5], params.get(), params.get(), get(), get()) }
    viewModel { params -> WorkoutViewModel(StandardProgressionFactory(), get(), get(), get(), get(), get(), get(), get(), get(), get(), params[0], params[1], get(), get()) }
    viewModel { params -> EditWorkoutViewModel(params.get(), get(), get(), params.get(), get(), get()) }
    viewModel { TopAppBarViewModel() }
    viewModel { BottomNavBarViewModel() }
    viewModel { BottomSheetViewModel() }
    viewModel { params -> CountdownTimerViewModel(params.get()) }
    viewModel { TimerViewModel() }
    viewModel { PickerViewModel() }
    viewModel { params -> HomeViewModel(get(), get(), get(), get(), get(), params.get(), params.get(), get(), get()) }
    viewModel { params -> SettingsViewModel(get(), get(), params.get(), get(), get()) }
    viewModel { params -> WorkoutHistoryViewModel(get(), params.get(), get(), get()) }
    viewModel { params -> DonationViewModel(params.get(), get(), get())}
}