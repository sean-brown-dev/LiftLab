package com.browntowndev.liftlab.core.dependencyInjection

import org.greenrobot.eventbus.EventBus
import org.koin.dsl.module

val eventBusModule = module {
    single { EventBus.getDefault() }
}