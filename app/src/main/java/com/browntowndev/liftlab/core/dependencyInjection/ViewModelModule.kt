package com.browntowndev.liftlab.core.dependencyInjection

import com.browntowndev.liftlab.ui.viewmodels.LiftLibraryViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { LiftLibraryViewModel(get()) }
}