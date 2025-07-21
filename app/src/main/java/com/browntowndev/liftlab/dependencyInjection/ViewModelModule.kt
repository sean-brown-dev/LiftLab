package com.browntowndev.liftlab.dependencyInjection

import com.browntowndev.liftlab.core.domain.progression.StandardProgressionFactory
import com.browntowndev.liftlab.ui.viewmodels.BottomNavBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.BottomSheetViewModel
import com.browntowndev.liftlab.ui.viewmodels.CountdownTimerViewModel
import com.browntowndev.liftlab.ui.viewmodels.DonationViewModel
import com.browntowndev.liftlab.ui.viewmodels.EditWorkoutViewModel
import com.browntowndev.liftlab.ui.viewmodels.FirestoreSyncViewModel
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
        LiftDetailsViewModel(params.get(), params[0], get(), get(), get(), get())
    }
    factory { params ->
        WorkoutBuilderViewModel(
            params.get(), params.get(), get(), get(), get(), get(), get(),
            params.get(), get(), get(), get(), get()
        )
    }
    factory { params ->
        LiftLibraryViewModel(
            get(), get(), get(), params[0], params[1], params[2], params[3],
            params[4], params[5], params.get(), params.get(), get(), get()
        )
    }
    factory { params ->
        WorkoutViewModel(
            StandardProgressionFactory(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), params[0], params[1], get(), get()
        )
    }
    factory { params ->
        EditWorkoutViewModel(params.get(), get(), get(), params.get(), get(), get())
    }
    factory { params ->
        CountdownTimerViewModel(params.get())
    }
    factory { params ->
        HomeViewModel(get(), get(), get(), get(), get(), params[0], params[1], params[2], get(), get(), get())
    }
    factory { params ->
        SettingsViewModel(get(), get(), params.get(), get(), get())
    }
    factory { params ->
        WorkoutHistoryViewModel(get(), params.get(), get(), get())
    }
    factory { params ->
        DonationViewModel(params.get(), get(), get())
    }

    viewModelOf(::LabViewModel)
    viewModelOf(::TopAppBarViewModel)
    viewModelOf(::BottomNavBarViewModel)
    viewModelOf(::BottomSheetViewModel)
    viewModelOf(::TimerViewModel)
    viewModelOf(::PickerViewModel)
    viewModelOf(::FirestoreSyncViewModel)
}