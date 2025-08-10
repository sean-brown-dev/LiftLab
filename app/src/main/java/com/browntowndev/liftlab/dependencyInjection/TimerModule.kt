package com.browntowndev.liftlab.dependencyInjection

import com.browntowndev.liftlab.ui.notifications.LiftLabTimer
import org.koin.core.qualifier.named
import org.koin.dsl.module

val timerModule = module {
    single(named("CountdownTimer")) {
        LiftLabTimer()
    }
    single(named("DurationTimer")) {
        LiftLabTimer()
    }
}