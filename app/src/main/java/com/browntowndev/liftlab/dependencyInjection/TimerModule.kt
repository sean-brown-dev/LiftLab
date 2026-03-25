package com.browntowndev.liftlab.dependencyInjection

import com.browntowndev.liftlab.ui.notifications.LiftLabTimer
import org.koin.core.qualifier.named
import org.koin.dsl.module

val CountdownTimer = named("CountdownTimer")
val DurationTimer = named("DurationTimer")

val timerModule = module {
    single(CountdownTimer) {
        LiftLabTimer()
    }
    single(DurationTimer) {
        LiftLabTimer()
    }
}