package com.browntowndev.liftlab.dependencyInjection

import com.browntowndev.liftlab.core.common.NetworkMonitor
import com.browntowndev.liftlab.core.coroutines.AppDispatchers
import com.browntowndev.liftlab.core.coroutines.RealDispatchers
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

val AppScope = named("AppScope")
val AppJob   = named("AppJob")

val appModule = module {
    single<AppDispatchers> { RealDispatchers() }

    single<Job>(AppJob) { SupervisorJob() }

    // Use a general-purpose dispatcher; hop to IO inside tasks when needed.
    single<CoroutineScope>(AppScope) {
        CoroutineScope(
            get<Job>(AppJob) + get<AppDispatchers>().default + CoroutineName("AppScope")
        )
    }

    single {
        NetworkMonitor(
            context = get(),
            appScope = get(AppScope)
        )
    }
}
