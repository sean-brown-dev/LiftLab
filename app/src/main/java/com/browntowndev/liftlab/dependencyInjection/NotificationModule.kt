package com.browntowndev.liftlab.dependencyInjection

import com.browntowndev.liftlab.ui.notifications.NotificationHelper
import org.koin.dsl.module

val notificationModule = module {
    single {
        NotificationHelper(
            programRepository = get(),
            workoutsRepository = get(),
            workoutInProgressRepository = get(),
            restTimerInProgressRepository = get()
        )
    }
}