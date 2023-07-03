package com.browntowndev.liftlab.core.dependencyInjection

import com.browntowndev.liftlab.ui.viewmodels.BottomNavBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.BottomSheetViewModel
import com.browntowndev.liftlab.ui.viewmodels.LabViewModel
import com.browntowndev.liftlab.ui.viewmodels.LiftLibraryViewModel
import com.browntowndev.liftlab.ui.viewmodels.TopAppBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.WorkoutBuilderViewModel
import com.browntowndev.liftlab.ui.viewmodels.WorkoutViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { LabViewModel(get(), get(), get(), get()) }
    viewModel { params -> WorkoutBuilderViewModel(params.get(), params.get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { LiftLibraryViewModel(get(), get(), get(), get()) }
    viewModel { WorkoutViewModel(get()) }
    viewModel { TopAppBarViewModel() }
    viewModel { BottomNavBarViewModel() }
    viewModel { BottomSheetViewModel() }
}