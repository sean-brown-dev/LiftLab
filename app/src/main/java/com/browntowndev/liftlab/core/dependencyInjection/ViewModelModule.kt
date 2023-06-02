package com.browntowndev.liftlab.core.dependencyInjection

import com.browntowndev.liftlab.ui.viewmodels.LabViewModel
import com.browntowndev.liftlab.ui.viewmodels.LiftLibraryViewModel
import com.browntowndev.liftlab.ui.viewmodels.TopAppBarViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { LiftLibraryViewModel(get()) }
    viewModel { LabViewModel(get(), get()) }
    viewModel { TopAppBarViewModel() }
}